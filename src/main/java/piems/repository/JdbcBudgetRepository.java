package piems.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import piems.domain.Budget;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.domain.PersistenceException;

/**
 * JDBC implementation of {@link BudgetRepository} backed by SQLite.
 *
 * <p>Persists category budgets to SQLite table {@code budgets} and enforces foreign keys
 * and unique category-month constraints (FR-09, FR-18, Decisions D-11, D-32, D-46).</p>
 */
public class JdbcBudgetRepository implements BudgetRepository {

    private static final String INSERT_SQL = """
        INSERT INTO budgets (category_id, month, limit_minor)
        VALUES (?, ?, ?)
        """;

    private static final String UPDATE_SQL = """
        UPDATE budgets
        SET category_id = ?, month = ?, limit_minor = ?
        WHERE id = ?
        """;

    private static final String FIND_BY_ID_SQL = """
        SELECT id, category_id, month, limit_minor
        FROM budgets WHERE id = ?
        """;

    private static final String FIND_BY_CAT_MONTH_SQL = """
        SELECT id, category_id, month, limit_minor
        FROM budgets WHERE category_id = ? AND month = ?
        """;

    private static final String FIND_ALL_SQL = """
        SELECT id, category_id, month, limit_minor
        FROM budgets ORDER BY id ASC
        """;

    private final String jdbcUrl;
    private final CategoryRepository categoryRepository;

    /**
     * Constructs a repository with JDBC connection URL and category repository collaborator.
     *
     * @param jdbcUrl            SQLite JDBC connection URL
     * @param categoryRepository category repository to resolve category components
     */
    public JdbcBudgetRepository(String jdbcUrl, CategoryRepository categoryRepository) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl cannot be null");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository cannot be null");
    }

    /**
     * Constructs a repository defaulting category resolution to a {@link JdbcCategoryRepository}.
     *
     * @param jdbcUrl SQLite JDBC connection URL
     */
    public JdbcBudgetRepository(String jdbcUrl) {
        this(jdbcUrl, new JdbcCategoryRepository(jdbcUrl));
    }

    /**
     * Creates an active connection with foreign key enforcement enabled.
     *
     * @return active connection
     * @throws SQLException if connection fails
     */
    public Connection createConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    @Override
    public Budget save(Budget budget) throws PersistenceException {
        Objects.requireNonNull(budget, "budget cannot be null");
        try (Connection conn = createConnection()) {
            if (budget.getId() == null) {
                long generatedId = insertInternal(conn, budget);
                return new Budget(generatedId, budget.getCategory(), budget.getMonth(), budget.getLimit());
            }
            updateInternal(conn, budget);
            return budget;
        } catch (SQLException e) {
            throw new PersistenceException("Failed to persist budget: " + e.getMessage(), e);
        }
    }

    private long insertInternal(Connection conn, Budget budget) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, budget.getCategory().id());
            stmt.setString(2, budget.getMonth().toString());
            stmt.setLong(3, budget.getLimit().minorUnits());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to retrieve generated key for budget");
    }

    private void updateInternal(Connection conn, Budget budget) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {
            stmt.setLong(1, budget.getCategory().id());
            stmt.setString(2, budget.getMonth().toString());
            stmt.setLong(3, budget.getLimit().minorUnits());
            stmt.setLong(4, budget.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public Optional<Budget> findById(long id) throws PersistenceException {
        try (Connection conn = createConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_ID_SQL)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to query budget by id: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Budget> findByCategoryAndMonth(long categoryId, YearMonth month)
            throws PersistenceException {
        Objects.requireNonNull(month, "month cannot be null");
        try (Connection conn = createConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_CAT_MONTH_SQL)) {
            stmt.setLong(1, categoryId);
            stmt.setString(2, month.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to query budget: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Budget> findAll() throws PersistenceException {
        try (Connection conn = createConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_ALL_SQL);
             ResultSet rs = stmt.executeQuery()) {
            List<Budget> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new PersistenceException("Failed to list all budgets: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(long id) throws PersistenceException {
        try (Connection conn = createConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM budgets WHERE id = ?")) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to delete budget: " + e.getMessage(), e);
        }
    }

    @Override
    public int count() throws PersistenceException {
        try (Connection conn = createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM budgets")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new PersistenceException("Failed to count budgets: " + e.getMessage(), e);
        }
    }

    private Budget mapRow(ResultSet rs) throws SQLException, PersistenceException {
        long id = rs.getLong("id");
        long catId = rs.getLong("category_id");
        YearMonth month = YearMonth.parse(rs.getString("month"));
        Money limit = Money.ofMinor(rs.getLong("limit_minor"));
        CategoryComponent category = categoryRepository.findById(catId)
                .orElseThrow(() -> new PersistenceException("Category not found for budget: " + catId));
        return new Budget(id, category, month, limit);
    }
}
