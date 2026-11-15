package piems.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import piems.domain.Account;
import piems.domain.CategoryComponent;
import piems.domain.InsufficientFundsException;
import piems.domain.Money;
import piems.domain.PersistenceException;
import piems.domain.PiemsException;
import piems.domain.RecordNotFoundException;
import piems.domain.ValidationException;
import piems.service.FinanceService;
import piems.service.TransactionView;

/**
 * Text-based console application interface for PIEMS.
 *
 * <p>Separates presentation from application logic by delegating all financial operations
 * exclusively to {@link FinanceService} (Decision D-05, D-14). Contains zero business
 * rules and handles user-facing errors gracefully (Decision D-13).</p>
 */
public class ConsoleApp {

    private final FinanceService service;
    private final BufferedReader reader;
    private final PrintStream out;

    /**
     * Constructs a ConsoleApp using standard input and output streams.
     *
     * @param service application finance service facade
     */
    public ConsoleApp(FinanceService service) {
        this(service, System.in, System.out);
    }

    /**
     * Constructs a ConsoleApp with custom input and output streams.
     *
     * @param service application finance service facade
     * @param in      input stream to read commands from
     * @param out     output stream to print responses to
     */
    public ConsoleApp(FinanceService service, InputStream in, PrintStream out) {
        this(service, new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(in, "in cannot be null"), StandardCharsets.UTF_8)), out);
    }

    /**
     * Constructs a ConsoleApp with a reader and output stream for testing.
     *
     * @param service application finance service facade
     * @param reader  buffered reader providing user input
     * @param out     destination print stream
     */
    public ConsoleApp(FinanceService service, BufferedReader reader, PrintStream out) {
        this.service = Objects.requireNonNull(service, "service cannot be null");
        this.reader = Objects.requireNonNull(reader, "reader cannot be null");
        this.out = Objects.requireNonNull(out, "out cannot be null");
    }

    /**
     * Executes the main interactive console loop until an exit command or EOF.
     */
    public void run() {
        out.println("Welcome to PIEMS!");
        boolean running = true;
        while (running) {
            ConsoleMenu.displayMainMenu(out);
            try {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                running = dispatch(line.trim());
            } catch (PiemsException | IllegalArgumentException e) {
                out.println("Error: " + e.getMessage());
            } catch (IOException e) {
                out.println("I/O Error: " + e.getMessage());
                break;
            }
        }
    }

    private boolean dispatch(String choice) throws IOException, PiemsException {
        switch (choice) {
            case "1" -> viewAccountsAndNetWorth();
            case "2" -> createAccount();
            case "3" -> recordIncome();
            case "4" -> recordExpense();
            case "5" -> recordTransfer();
            case "6" -> manageCategories();
            case "7" -> viewAccountTransactions();
            case "8" -> {
                out.println("Goodbye!");
                return false;
            }
            default -> out.println("Invalid option: " + choice + ". Please select 1-8.");
        }
        return true;
    }

    private void viewAccountsAndNetWorth() throws PiemsException {
        List<Account> accounts = service.listAccounts();
        out.println();
        out.println("--- Accounts ---");
        if (accounts.isEmpty()) {
            out.println("No accounts found.");
        } else {
            for (Account account : accounts) {
                Money balance = service.getAccountBalance(account.id());
                out.printf("ID: %d | %-15s | Type: %-11s | Balance: %s%n",
                        account.id(), account.name(), account.type(), balance);
            }
        }
        Money netWorth = service.getNetWorth();
        out.println("Total Net Worth: " + netWorth);
    }

    private void createAccount() throws IOException, PiemsException {
        out.println();
        out.println("--- Create Account ---");
        String typeChoice = readLine("Account Type (1: Cash, 2: Bank, 3: Credit Card): ");
        if (typeChoice == null) {
            return;
        }
        String name = readLine("Account Name: ");
        if (name == null) {
            return;
        }
        String amtPrompt = "3".equals(typeChoice) ? "Credit Limit: " : "Opening Balance: ";
        String amtStr = readLine(amtPrompt);
        if (amtStr == null) {
            return;
        }
        Account created = executeCreateAccount(typeChoice, name, Money.of(amtStr));
        Money balance = service.getAccountBalance(created.id());
        out.println("Account created successfully (ID: " + created.id() + "). Balance: " + balance);
    }

    private Account executeCreateAccount(String typeChoice, String name, Money amt)
            throws ValidationException, PersistenceException {
        return switch (typeChoice) {
            case "1" -> service.createCashAccount(name, amt);
            case "2" -> service.createBankAccount(name, amt);
            case "3" -> service.createCreditCardAccount(name, amt);
            default -> throw new IllegalArgumentException("Invalid account type option: " + typeChoice);
        };
    }

    private void recordIncome() throws IOException, ValidationException,
            RecordNotFoundException, PersistenceException {
        out.println();
        out.println("--- Record Income ---");
        String accInput = readLine("Account (ID or Name): ");
        if (accInput == null) {
            return;
        }
        Account account = resolveAccount(accInput);
        String catInput = readLine("Category ID (leave blank if none): ");
        Long categoryId = parseOptionalLong(catInput);
        String amtStr = readLine("Amount: ");
        if (amtStr == null) {
            return;
        }
        String note = readLine("Note (optional): ");
        service.recordIncome(account.id(), categoryId, Money.of(amtStr), null, note);
        Money newBalance = service.getAccountBalance(account.id());
        out.println("Income recorded successfully. New balance for " + account.name() + ": " + newBalance);
    }

    private void recordExpense() throws IOException, ValidationException,
            RecordNotFoundException, InsufficientFundsException, PersistenceException {
        out.println();
        out.println("--- Record Expense ---");
        String accInput = readLine("Account (ID or Name): ");
        if (accInput == null) {
            return;
        }
        Account account = resolveAccount(accInput);
        String catInput = readLine("Category ID (leave blank if none): ");
        Long categoryId = parseOptionalLong(catInput);
        String amtStr = readLine("Amount: ");
        if (amtStr == null) {
            return;
        }
        String note = readLine("Note (optional): ");
        service.recordExpense(account.id(), categoryId, Money.of(amtStr), null, note);
        Money newBalance = service.getAccountBalance(account.id());
        out.println("Expense recorded successfully. New balance for " + account.name() + ": " + newBalance);
    }

    private void recordTransfer() throws IOException, ValidationException,
            RecordNotFoundException, InsufficientFundsException, PersistenceException {
        out.println();
        out.println("--- Transfer Funds ---");
        String fromInput = readLine("From Account (ID or Name): ");
        if (fromInput == null) {
            return;
        }
        Account fromAccount = resolveAccount(fromInput);
        String toInput = readLine("To Account (ID or Name): ");
        if (toInput == null) {
            return;
        }
        Account toAccount = resolveAccount(toInput);
        String amtStr = readLine("Amount: ");
        if (amtStr == null) {
            return;
        }
        String note = readLine("Note (optional): ");
        service.recordTransfer(fromAccount.id(), toAccount.id(), Money.of(amtStr), null, note);
        Money fromBal = service.getAccountBalance(fromAccount.id());
        Money toBal = service.getAccountBalance(toAccount.id());
        out.println("Transfer completed successfully.");
        out.println("From Account (" + fromAccount.name() + ") new balance: " + fromBal);
        out.println("To Account (" + toAccount.name() + ") new balance: " + toBal);
    }

    private void manageCategories() throws IOException, ValidationException,
            RecordNotFoundException, PersistenceException {
        out.println();
        out.println("--- Category Management ---");
        out.println("1. List Categories");
        out.println("2. Add Leaf Category");
        out.println("3. Add Category Group");
        out.println("4. Archive Category");
        out.println("5. Delete Category");
        String choice = readLine("Select action (1-5): ");
        if (choice == null) {
            return;
        }
        switch (choice) {
            case "1" -> listCategoriesAction();
            case "2" -> createCategoryAction(false);
            case "3" -> createCategoryAction(true);
            case "4" -> archiveCategoryAction();
            case "5" -> deleteCategoryAction();
            default -> throw new IllegalArgumentException("Invalid category option: " + choice);
        }
    }

    private void listCategoriesAction() throws PersistenceException {
        List<CategoryComponent> cats = service.listCategories();
        out.println();
        out.println("--- Categories ---");
        if (cats.isEmpty()) {
            out.println("No categories found.");
        } else {
            for (CategoryComponent c : cats) {
                out.printf("ID: %d | %-20s | Kind: %-7s | Archived: %s%n",
                        c.id(), c.path(), c.kind(), c.isArchived());
            }
        }
    }

    private void createCategoryAction(boolean isGroup) throws IOException,
            ValidationException, RecordNotFoundException, PersistenceException {
        String name = readLine("Category Name: ");
        if (name == null) {
            return;
        }
        String kind = readLine("Kind (INCOME/EXPENSE): ");
        if (kind == null) {
            return;
        }
        String parentStr = readLine("Parent Group ID (blank for root): ");
        Long parentId = parseOptionalLong(parentStr);
        CategoryComponent comp = isGroup
                ? service.createCategoryGroup(name, kind, parentId)
                : service.createCategory(name, kind, parentId);
        out.println("Category created successfully (ID: " + comp.id() + "): " + comp.path());
    }

    private void archiveCategoryAction() throws IOException,
            RecordNotFoundException, PersistenceException {
        String idStr = readLine("Category ID to archive: ");
        if (idStr == null) {
            return;
        }
        long id = Long.parseLong(idStr);
        service.archiveCategory(id);
        out.println("Category " + id + " archived successfully.");
    }

    private void deleteCategoryAction() throws IOException,
            ValidationException, RecordNotFoundException, PersistenceException {
        String idStr = readLine("Category ID to delete: ");
        if (idStr == null) {
            return;
        }
        long id = Long.parseLong(idStr);
        service.deleteCategory(id);
        out.println("Category " + id + " deleted successfully.");
    }

    private void viewAccountTransactions() throws IOException,
            RecordNotFoundException, PersistenceException {
        out.println();
        out.println("--- Account Transactions ---");
        String accInput = readLine("Account (ID or Name): ");
        if (accInput == null) {
            return;
        }
        Account account = resolveAccount(accInput);
        List<TransactionView> txs = service.listTransactionsForAccount(account.id());
        if (txs.isEmpty()) {
            out.println("No transactions for account: " + account.name());
        } else {
            for (TransactionView tx : txs) {
                String noteStr = (tx.note() != null) ? tx.note() : "";
                out.printf("ID: %d | Date: %s | Kind: %-8s | Amount: %s | Note: %s%n",
                        tx.id(), tx.date(), tx.kind(), tx.amount(), noteStr);
            }
        }
    }

    private Account resolveAccount(String input) throws RecordNotFoundException, PersistenceException {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Account selection cannot be empty");
        }
        String trimmed = input.trim();
        try {
            long id = Long.parseLong(trimmed);
            return service.getAccount(id);
        } catch (NumberFormatException e) {
            return service.getAccountByName(trimmed);
        }
    }

    private String readLine(String prompt) throws IOException {
        out.print(prompt);
        out.flush();
        String line = reader.readLine();
        return (line != null) ? line.trim() : null;
    }

    private Long parseOptionalLong(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        return Long.parseLong(input.trim());
    }
}
