package piems.ui;

import java.io.PrintStream;

/**
 * Menu presentation component for the PIEMS console user interface.
 *
 * <p>Separates menu formatting from execution logic to ensure clean UI presentation
 * without embedded business rules (Decision D-05, D-15).</p>
 */
public final class ConsoleMenu {

    private ConsoleMenu() {
        // Utility class
    }

    /**
     * Prints the main application menu choices to the given output stream.
     *
     * @param out destination print stream
     */
    public static void displayMainMenu(PrintStream out) {
        out.println();
        out.println("=== PIEMS - Personal Income & Expense Manager ===");
        out.println("1. View Accounts & Net Worth");
        out.println("2. Create New Account");
        out.println("3. Record Income");
        out.println("4. Record Expense");
        out.println("5. Transfer Funds");
        out.println("6. Manage Categories");
        out.println("7. View Account Transactions");
        out.println("8. Exit");
        out.print("Select an option (1-8): ");
    }
}
