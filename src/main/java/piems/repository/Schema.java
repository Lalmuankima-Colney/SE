package piems.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates and initializes the PIEMS SQLite relational schema.
 *
 * <p>Implements the data dictionary from SRS §3.4, incorporating decisions D-23
 * (two-leg transfers linked by UUID), D-25 (derived account balances), and D-26
 * (category archiving).</p>
 */
public final class Schema {

    private Schema() {
        // Utility class
    }

    /**
     * Creates all required database tables and indices if they do not already exist.
     *
     * @param connection active database connection
     * @throws SQLException if a database access error occurs
     */
    public static void createTables(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            createAccountAndCategoryTables(stmt);
            createRuleAndBudgetTables(stmt);
            createTransactionTable(stmt);
            createGoalTables(stmt);
            createIndexes(stmt);
        }
    }

    private static void createAccountAndCategoryTables(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                type TEXT NOT NULL,
                opening_minor INTEGER NOT NULL DEFAULT 0,
                credit_limit_minor INTEGER NOT NULL DEFAULT 0,
                archived INTEGER NOT NULL DEFAULT 0
            );
            """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                parent_id INTEGER REFERENCES categories(id) ON DELETE SET NULL,
                kind TEXT NOT NULL,
                archived INTEGER NOT NULL DEFAULT 0
            );
            """);
    }

    private static void createRuleAndBudgetTables(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS recurring_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                frequency TEXT NOT NULL,
                start_date TEXT NOT NULL,
                end_date TEXT,
                last_generated TEXT
            );
            """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS budgets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category_id INTEGER NOT NULL REFERENCES categories(id),
                month TEXT NOT NULL,
                limit_minor INTEGER NOT NULL
            );
            """);
    }

    private static void createTransactionTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                kind TEXT NOT NULL,
                date TEXT NOT NULL,
                amount_minor INTEGER NOT NULL,
                note TEXT,
                account_id INTEGER NOT NULL REFERENCES accounts(id),
                transfer_link_id TEXT,
                category_id INTEGER REFERENCES categories(id),
                rule_id INTEGER REFERENCES recurring_rules(id)
            );
            """);
    }

    private static void createGoalTables(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                target_minor INTEGER NOT NULL,
                deadline TEXT,
                status TEXT NOT NULL DEFAULT 'ACTIVE'
            );
            """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS goal_contributions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                goal_id INTEGER NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
                date TEXT NOT NULL,
                amount_minor INTEGER NOT NULL
            );
            """);
    }

    private static void createIndexes(Statement stmt) throws SQLException {
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_account ON transactions(account_id);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_date ON transactions(date);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_link ON transactions(transfer_link_id);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_category ON transactions(category_id);");
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_budget_cat_month ON budgets(category_id, month);");
    }
}
