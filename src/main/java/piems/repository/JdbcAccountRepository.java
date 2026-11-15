package piems.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import piems.domain.Account;
import piems.domain.BankAccount;
import piems.domain.CashAccount;
import piems.domain.CreditCardAccount;
import piems.domain.Money;
import piems.domain.PersistenceException;

/**
 * JDBC implementation of {@link AccountRepository} with SQLite persistence.
 *
 * <p>Enforces {@code PRAGMA foreign_keys = ON;} and derives account balances dynamically
 * via indexed SQL aggregation over transactions per Decisions D-05, D-11, D-25, and D-32.</p>
 */
public class JdbcAccountRepository implements AccountRepository {

    private static final String INSERT_SQL = """
        INSERT INTO accounts (name, type, opening_minor, credit_limit_minor, archived)
        VALUES (?, ?, ?, ?, ?)
        """;

    private static final String UPDATE_SQL = """
        UPDATE accounts
        SET name = ?, type = ?, opening_minor = ?, credit_limit_minor = ?, archived = ?
        WHERE id = ?
        """;

    private static final String BALANCE_SQL = """
        SELECT a.opening_minor,
               COALESCE((
                   SELECT SUM(
                       CASE
                           WHEN t.kind = 'INCOME' THEN t.amount_minor
                           WHEN t.kind = 'EXPENSE' THEN -t.amount_minor
                           WHEN t.kind = 'TRANSFER' AND t.id = (
                               SELECT MIN(sub.id) FROM transactions sub WHERE sub.transfer_link_id = t.transfer_link_id
                           ) THEN -t.amount_minor
                           WHEN t.kind = 'TRANSFER' AND t.id = (
                               SELECT MAX(sub.id) FROM transactions sub WHERE sub.transfer_link_id = t.transfer_link_id
                           ) THEN t.amount_minor
                           ELSE 0
                       END
                   )
                   FROM transactions t
                   WHERE t.account_id = a.id
               ), 0) AS effects_minor
        FROM accounts a
        WHERE a.id = ?
        """;

    private final String jdbcUrl;

    /**
     * Constructs a repository with the specified SQLite JDBC URL.
     *
     * @param jdbcUrl SQLite JDBC connection URL
     */
    public JdbcAccountRepository(String jdbcUrl) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl cannot be null");
    }

    /**
     * Creates a connection with foreign key enforcement enabled.
     *
     * @return active connection
     * @throws SQLException if a connection cannot be established
     */
    public Connection createConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    @Override
    public Account save(Account account) throws PersistenceException {
        Objects.requireNonNull(account, "Account cannot be null");
        try (Connection conn = createConnection()) {
            if (account.id() == null) {
                long generatedId = insertAccount(conn, account);
                return recreateWithId(account, generatedId);
            }
            updateAccount(conn, account);
            return account;
        } catch (SQLException e) {
            throw new PersistenceException("Failed to save account: " + e.getMessage(), e);
        }
    }

    private long insertAccount(Connection conn, Account account) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bindAccountParameters(ps, account);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert failed, no ID obtained");
            }
        }
    }

    private void updateAccount(Connection conn, Account account) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            bindAccountParameters(ps, account);
            ps.setLong(6, account.id());
            ps.executeUpdate();
        }
    }

    private void bindAccountParameters(PreparedStatement ps, Account account) throws SQLException {
        ps.setString(1, account.name());
        ps.setString(2, account.type());
        ps.setLong(3, account.openingBalance().minorUnits());
        ps.setLong(4, account.creditLimit().minorUnits());
        ps.setInt(5, account.isArchived() ? 1 : 0);
    }

    private Account recreateWithId(Account account, Long id) {
        return switch (account.type()) {
            case "CASH" -> new CashAccount(id, account.name(), account.openingBalance(), account.isArchived());
            case "BANK" -> new BankAccount(id, account.name(), account.openingBalance(), account.isArchived());
            case "CREDIT_CARD" ->
                new CreditCardAccount(id, account.name(), account.creditLimit(), account.isArchived());
            default -> throw new IllegalArgumentException("Unknown account type: " + account.type());
        };
    }

    @Override
    public Optional<Account> findById(long id) throws PersistenceException {
        String sql = "SELECT id, name, type, opening_minor, credit_limit_minor, archived FROM accounts WHERE id = ?";
        return querySingle(sql, ps -> ps.setLong(1, id));
    }

    @Override
    public Optional<Account> findByName(String name) throws PersistenceException {
        if (name == null) {
            return Optional.empty();
        }
        String sql = """
            SELECT id, name, type, opening_minor, credit_limit_minor, archived
            FROM accounts WHERE LOWER(name) = LOWER(?)
            """;
        return querySingle(sql, ps -> ps.setString(1, name.trim()));
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private Optional<Account> querySingle(String sql, StatementBinder binder) throws PersistenceException {
        try (Connection conn = createConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapResultSetToAccount(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Query failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Account> findAll() throws PersistenceException {
        String sql = "SELECT id, name, type, opening_minor, credit_limit_minor, archived FROM accounts ORDER BY id";
        try (Connection conn = createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<Account> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapResultSetToAccount(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new PersistenceException("Failed to find all accounts", e);
        }
    }

    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String name = rs.getString("name");
        String type = rs.getString("type");
        long openingMinor = rs.getLong("opening_minor");
        long creditLimitMinor = rs.getLong("credit_limit_minor");
        boolean archived = rs.getInt("archived") == 1;

        return switch (type) {
            case "CASH" -> new CashAccount(id, name, Money.ofMinor(openingMinor), archived);
            case "BANK" -> new BankAccount(id, name, Money.ofMinor(openingMinor), archived);
            case "CREDIT_CARD" -> new CreditCardAccount(id, name, Money.ofMinor(creditLimitMinor), archived);
            default -> throw new SQLException("Unknown account type stored: " + type);
        };
    }

    @Override
    public void delete(long id) throws PersistenceException {
        try (Connection conn = createConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM accounts WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to delete account " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public int count() throws PersistenceException {
        try (Connection conn = createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM accounts")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new PersistenceException("Failed to count accounts", e);
        }
    }

    @Override
    public Money getDerivedBalance(long accountId) throws PersistenceException {
        Account account = findById(accountId)
                .orElseThrow(() -> new PersistenceException("Account not found with id: " + accountId));
        if ("CREDIT_CARD".equals(account.type())) {
            return Money.zero(account.openingBalance().currency());
        }
        try (Connection conn = createConnection();
             PreparedStatement ps = conn.prepareStatement(BALANCE_SQL)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long opening = rs.getLong("opening_minor");
                    long effects = rs.getLong("effects_minor");
                    return Money.ofMinor(opening + effects);
                }
                throw new PersistenceException("Account not found with id: " + accountId);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to calculate derived balance: " + e.getMessage(), e);
        }
    }
}
