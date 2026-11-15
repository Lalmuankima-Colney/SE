package piems.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import piems.domain.Category;
import piems.domain.CategoryComponent;
import piems.domain.CategoryGroup;
import piems.domain.PersistenceException;

/**
 * JDBC implementation of {@link CategoryRepository} supporting composite trees and archiving.
 *
 * <p>Enforces {@code PRAGMA foreign_keys = ON} and handles recursive composite trees
 * according to Requirements FR-05, FR-18, and Decisions D-08, D-11, D-26, and D-32.</p>
 */
public class JdbcCategoryRepository implements CategoryRepository {

    private static final String INSERT_SQL = """
        INSERT INTO categories (name, parent_id, kind, archived)
        VALUES (?, ?, ?, ?)
        """;

    private static final String UPDATE_SQL = """
        UPDATE categories
        SET name = ?, parent_id = ?, kind = ?, archived = ?
        WHERE id = ?
        """;

    private final String jdbcUrl;

    /**
     * Constructs a repository with the specified SQLite JDBC URL.
     *
     * @param jdbcUrl SQLite JDBC connection URL
     */
    public JdbcCategoryRepository(String jdbcUrl) {
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
    public CategoryComponent save(CategoryComponent category) throws PersistenceException {
        Objects.requireNonNull(category, "Category cannot be null");
        try (Connection conn = createConnection()) {
            return saveInternal(conn, category, category.parent() != null ? category.parent().id() : null);
        } catch (SQLException e) {
            throw new PersistenceException("Failed to save category: " + e.getMessage(), e);
        }
    }

    private CategoryComponent saveInternal(Connection conn, CategoryComponent cat, Long parentId)
            throws SQLException {
        Long id = cat.id();
        if (id == null) {
            id = insertCategory(conn, cat, parentId);
        } else {
            updateCategory(conn, cat, parentId);
        }
        CategoryComponent persisted = cat.copyWithId(id);
        if (cat.isGroup()) {
            for (CategoryComponent child : cat.children()) {
                saveInternal(conn, child, id);
            }
        }
        return persisted;
    }

    private long insertCategory(Connection conn, CategoryComponent cat, Long parentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cat.name());
            setNullableLong(ps, 2, parentId);
            ps.setString(3, cat.kind());
            ps.setInt(4, cat.isArchived() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert failed, no ID returned");
            }
        }
    }

    private void updateCategory(Connection conn, CategoryComponent cat, Long parentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            ps.setString(1, cat.name());
            setNullableLong(ps, 2, parentId);
            ps.setString(3, cat.kind());
            ps.setInt(4, cat.isArchived() ? 1 : 0);
            ps.setLong(5, cat.id());
            ps.executeUpdate();
        }
    }

    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value != null) {
            ps.setLong(index, value);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }

    @Override
    public Optional<CategoryComponent> findById(long id) throws PersistenceException {
        try (Connection conn = createConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM categories WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(reconstructComponent(conn, rs));
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to find category by id " + id, e);
        }
    }

    @Override
    public Optional<CategoryComponent> findByName(String name) throws PersistenceException {
        if (name == null) {
            return Optional.empty();
        }
        try (Connection conn = createConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM categories WHERE LOWER(name) = LOWER(?)")) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(reconstructComponent(conn, rs));
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to find category by name " + name, e);
        }
    }

    private CategoryComponent reconstructComponent(Connection conn, ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String name = rs.getString("name");
        String kind = rs.getString("kind");
        boolean archived = rs.getInt("archived") == 1;

        List<CategoryComponent> children = loadDirectChildren(conn, id);
        if (!children.isEmpty()) {
            CategoryGroup group = new CategoryGroup(id, name, kind, archived);
            for (CategoryComponent child : children) {
                group.add(child);
            }
            return group;
        }
        return new Category(id, name, kind, archived);
    }

    private List<CategoryComponent> loadDirectChildren(Connection conn, long parentId) throws SQLException {
        String sql = "SELECT * FROM categories WHERE parent_id = ? ORDER BY id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<CategoryComponent> children = new ArrayList<>();
                while (rs.next()) {
                    children.add(reconstructComponent(conn, rs));
                }
                return children;
            }
        }
    }

    @Override
    public List<CategoryComponent> findAll() throws PersistenceException {
        try (Connection conn = createConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM categories WHERE parent_id IS NULL ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<CategoryComponent> roots = new ArrayList<>();
            while (rs.next()) {
                roots.add(reconstructComponent(conn, rs));
            }
            return roots;
        } catch (SQLException e) {
            throw new PersistenceException("Failed to find all categories: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(long id) throws PersistenceException {
        try (Connection conn = createConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM categories WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to delete category " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public int count() throws PersistenceException {
        try (Connection conn = createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM categories")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new PersistenceException("Failed to count categories", e);
        }
    }
}
