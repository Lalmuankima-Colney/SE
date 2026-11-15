package piems.app;

import java.time.Clock;
import piems.domain.PersistenceException;
import piems.repository.SqliteDatabase;
import piems.service.FinanceService;
import piems.ui.ConsoleApp;
import piems.ui.swing.SwingApp;

/**
 * Composition root and main application entry point.
 *
 * <p>Wires database repositories, domain services, and UI presentation components
 * via constructor injection without singletons (Decision D-05, D-14, D-22, D-40).</p>
 */
public final class Main {

    private Main() {
        // Utility class
    }

    /**
     * Application entry point bootstrapping PIEMS in console or Swing GUI mode.
     *
     * @param args optional arguments; pass {@code --gui} to start the Swing GUI
     */
    public static void main(String[] args) {
        boolean useGui = isGuiRequested(args);
        String dbUrl = extractDbUrl(args);
        try (SqliteDatabase db = new SqliteDatabase(dbUrl)) {
            FinanceService service = new FinanceService(
                    db.accountRepository(),
                    db.categoryRepository(),
                    db.transactionRepository(),
                    Clock.systemDefaultZone()
            );
            if (useGui) {
                SwingApp.launch(service);
            } else {
                new ConsoleApp(service).run();
            }
        } catch (PersistenceException e) {
            System.err.println("Fatal database initialization error: " + e.getMessage());
        }
    }

    private static boolean isGuiRequested(String[] args) {
        if ("swing".equalsIgnoreCase(System.getProperty("ui"))) {
            return true;
        }
        for (String arg : args) {
            if ("--gui".equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the SQLite JDBC connection URL from CLI arguments (D-44, P-308).
     *
     * <p>Auto-prepends {@code jdbc:sqlite:} if the provided database file path
     * does not already start with the JDBC SQLite prefix.</p>
     *
     * @param args CLI arguments array
     * @return resolved SQLite JDBC connection URL
     */
    public static String extractDbUrl(String[] args) {
        if (args != null) {
            for (String arg : args) {
                if (arg != null && !arg.startsWith("--") && !arg.isBlank()) {
                    String trimmed = arg.trim();
                    if (!trimmed.startsWith("jdbc:sqlite:")) {
                        return "jdbc:sqlite:" + trimmed;
                    }
                    return trimmed;
                }
            }
        }
        return "jdbc:sqlite:piems.db";
    }
}
