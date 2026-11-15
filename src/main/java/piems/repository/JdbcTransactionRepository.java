package piems.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import piems.domain.Money;
import piems.domain.PersistenceException;

/**
 * JDBC implementation of {@link TransactionRepository} supporting atomic two-leg transfers.
 *
 * <p>Enforces {@code PRAGMA foreign_keys = ON} on every connection and ensures
 * multi-statement transfer operations commit atomically or roll back completely
 * (Decisions D-11, D-23, D-32, D-33).</p>
 */
public class JdbcTransactionRepository implements TransactionRepository {

    private static final String INSERT_SQL = """
        INSERT INTO transactions (kind, date, amount_minor, note, account_id, transfer_link_id, category_id, rule_id)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String UPDATE_SQL = """
        UPDATE transactions
        SET kind = ?, date = ?, amount_minor = ?, note = ?, account_id = ?, transfer_link_id = ?,
            category_id = ?, rule_id = ?
        WHERE id = ?
        """;

    private static final String FIND_BY_ID_SQL = """
        SELECT id, kind, date, amount_minor, note, account_id, transfer_link_id, category_id, rule_id
        FROM transactions WHERE id = ?
        """;

    private final String jdbcUrl;

    /**
     * Constructs a repository with the specified SQLite JDBC URL.
     *
     * @param jdbcUrl SQLite JDBC connection URL
     */
    public JdbcTransactionRepository(String jdbcUrl) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl cannot be null");
    }

    /**
     * Creates a new connection with SQLite foreign key constraints enabled.
     *
     * @return active database connection
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
    public TransactionRecord save(TransactionRecord record) throws PersistenceException {
        Objects.requireNonNull(record, "record cannot be null");
        try (Connection conn = createConnection()) {
            if (record.id() == null) {
                long generatedId = insertInternal(conn, record);
                return copyWithId(record, generatedId);
            }
            updateInternal(conn, record);
            return record;
        } catch (SQLException e) {
            throw new PersistenceException("Failed to insert transaction: " + e.getMessage(), e);
        }
    }

    private TransactionRecord copyWithId(TransactionRecord r, long id) {
        return new TransactionRecord(
                id, r.kind(), r.date(), r.amount(), r.note(),
                r.accountId(), r.transferLinkId(), r.categoryId(), r.ruleId()
        );
    }

    /**
     * Inserts a single transaction record, returning the generated key.
     *
     * @param record transaction record to persist
     * @return generated transaction ID
     * @throws PersistenceException if insertion fails
     */
    public long insert(TransactionRecord record) throws PersistenceException {
        return save(record).id();
    }

    private long insertInternal(Connection conn, TransactionRecord record) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bindTransactionParameters(ps, record);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert failed, no generated key obtained");
            }
        }
    }

    private void updateInternal(Connection conn, TransactionRecord record) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            bindTransactionParameters(ps, record);
            ps.setLong(9, record.id());
            ps.executeUpdate();
        }
    }

    private void bindTransactionParameters(PreparedStatement ps, TransactionRecord record) throws SQLException {
        ps.setString(1, record.kind());
        ps.setString(2, record.date().toString());
        ps.setLong(3, record.amount().minorUnits());
        ps.setString(4, record.note());
        ps.setLong(5, record.accountId());
        ps.setString(6, record.transferLinkId());
        setNullableLong(ps, 7, record.categoryId());
        setNullableLong(ps, 8, record.ruleId());
    }

    private void setNullableLong(PreparedStatement ps, int paramIndex, Long value) throws SQLException {
        if (value != null) {
            ps.setLong(paramIndex, value);
        } else {
            ps.setNull(paramIndex, Types.INTEGER);
        }
    }

    @Override
    public Optional<TransactionRecord> findById(long id) throws PersistenceException {
        try (Connection conn = createConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID_SQL)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapResultSetToRecord(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to find transaction by ID " + id, e);
        }
    }

    @Override
    public List<TransactionRecord> findAll() throws PersistenceException {
        return queryList("SELECT * FROM transactions ORDER BY id", null);
    }

    @Override
    public List<TransactionRecord> findByAccountId(long accountId) throws PersistenceException {
        return queryList("SELECT * FROM transactions WHERE account_id = ? ORDER BY id", accountId);
    }

    @Override
    public List<TransactionRecord> findByCategoryId(long categoryId) throws PersistenceException {
        return queryList("SELECT * FROM transactions WHERE category_id = ? ORDER BY id", categoryId);
    }

    private List<TransactionRecord> queryList(String sql, Long param) throws PersistenceException {
        try (Connection conn = createConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setLong(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<TransactionRecord> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSetToRecord(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Query failed: " + e.getMessage(), e);
        }
    }

    private TransactionRecord mapResultSetToRecord(ResultSet rs) throws SQLException {
        Long categoryId = rs.getObject("category_id") != null ? rs.getLong("category_id") : null;
        Long ruleId = rs.getObject("rule_id") != null ? rs.getLong("rule_id") : null;
        return new TransactionRecord(
                rs.getLong("id"),
                rs.getString("kind"),
                LocalDate.parse(rs.getString("date")),
                Money.ofMinor(rs.getLong("amount_minor")),
                rs.getString("note"),
                rs.getLong("account_id"),
                rs.getString("transfer_link_id"),
                categoryId,
                ruleId
        );
    }

    @Override
    public void delete(long id) throws PersistenceException {
        try (Connection conn = createConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to delete transaction " + id, e);
        }
    }

    @Override
    public int count() throws PersistenceException {
        try (Connection conn = createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM transactions")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new PersistenceException("Failed to count transactions", e);
        }
    }

    @Override
    public void executeTransfer(TransactionRecord debitLeg, TransactionRecord creditLeg)
            throws PersistenceException {
        executeTransferWithFault(debitLeg, creditLeg, false);
    }

    @Override
    public void executeTransferWithFault(
            TransactionRecord debitLeg,
            TransactionRecord creditLeg,
            boolean faultSecondLeg
    ) throws PersistenceException {
        try (Connection conn = createConnection()) {
            conn.setAutoCommit(false);
            try {
                insertInternal(conn, debitLeg);
                if (faultSecondLeg) {
                    throw new SQLException("Injected fault during second transfer leg");
                }
                insertInternal(conn, creditLeg);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw new PersistenceException("Transfer rolled back: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Database error during transfer execution: " + e.getMessage(), e);
        }
    }

    @Override
    public List<TransactionRecord> findByQuery(TransactionQuery query) throws PersistenceException {
        Objects.requireNonNull(query, "query cannot be null");
        StringBuilder sql = new StringBuilder("SELECT * FROM transactions WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendQueryClauses(query, sql, params);
        sql.append(" ORDER BY id");

        try (Connection conn = createConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<TransactionRecord> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSetToRecord(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Query failed: " + e.getMessage(), e);
        }
    }

    private void appendQueryClauses(TransactionQuery q, StringBuilder sql, List<Object> params) {
        if (q.startDate() != null) {
            sql.append(" AND date >= ?");
            params.add(q.startDate().toString());
        }
        if (q.endDate() != null) {
            sql.append(" AND date <= ?");
            params.add(q.endDate().toString());
        }
        if (q.accountId() != null) {
            sql.append(" AND account_id = ?");
            params.add(q.accountId());
        }
        if (q.categoryId() != null) {
            sql.append(" AND category_id = ?");
            params.add(q.categoryId());
        }
        if (q.text() != null) {
            sql.append(" AND LOWER(note) LIKE ? ESCAPE '\\'");
            params.add("%" + escapeLikeWildcards(q.text().toLowerCase()) + "%");
        }
    }

    private static String escapeLikeWildcards(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    @Override
    public List<TransactionRecord> findByTransferLinkId(String linkId) throws PersistenceException {
        Objects.requireNonNull(linkId, "linkId cannot be null");
        String sql = "SELECT * FROM transactions WHERE transfer_link_id = ? ORDER BY id";
        try (Connection conn = createConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, linkId);
            try (ResultSet rs = ps.executeQuery()) {
                List<TransactionRecord> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSetToRecord(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Query by transfer link ID failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteByTransferLinkId(String linkId) throws PersistenceException {
        Objects.requireNonNull(linkId, "linkId cannot be null");
        String sql = "DELETE FROM transactions WHERE transfer_link_id = ?";
        try (Connection conn = createConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, linkId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Delete by transfer link ID failed: " + e.getMessage(), e);
        }
    }
}
