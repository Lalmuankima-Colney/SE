package piems.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.domain.Money;
import piems.repository.AccountRepository;
import piems.repository.CategoryRepository;
import piems.repository.InMemoryAccountRepository;
import piems.repository.InMemoryCategoryRepository;
import piems.repository.InMemoryTransactionRepository;
import piems.repository.TransactionRepository;
import piems.service.FinanceService;

/**
 * Scripted acceptance and UI workflow tests for {@link ConsoleApp}.
 *
 * <p>Verifies usability requirement NFR-04 (TC-NFR04-01), error handling (Decision D-13),
 * balance display after each action, and clean termination.</p>
 */
class ConsoleScriptTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneId.of("UTC"));

    private FinanceService service;

    @BeforeEach
    void setUp() {
        TransactionRepository txRepo = new InMemoryTransactionRepository();
        AccountRepository accountRepo = new InMemoryAccountRepository(txRepo);
        CategoryRepository catRepo = new InMemoryCategoryRepository(txRepo);
        service = new FinanceService(accountRepo, catRepo, txRepo, FIXED_CLOCK);
    }

    private String runScript(String inputScript) {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(new StringReader(inputScript));
        ConsoleApp app = new ConsoleApp(service, reader, out);
        app.run();
        return outBytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("TC-NFR04-01: Adding an expense requires at most 4 inputs")
    void recordsExpenseInFourInputs() throws Exception {
        // Precondition: Account exists with 100.00
        service.createCashAccount("Wallet", Money.of("100.00"));

        // Script: Select Option 4 (Record Expense), then exactly 4 inputs:
        // Input 1: Account name "Wallet"
        // Input 2: Category ID "" (blank for unassigned)
        // Input 3: Amount "42.50"
        // Input 4: Note "Groceries"
        // Followed by option 8 to exit.
        String script = String.join("\n",
                "4",
                "Wallet",
                "",
                "42.50",
                "Groceries",
                "8",
                ""
        );

        String output = runScript(script);

        assertThat(output)
                .contains("--- Record Expense ---")
                .contains("Expense recorded successfully. New balance for Wallet: 57.50")
                .contains("Goodbye!");
    }

    @Test
    @DisplayName("Displays accounts, derived balances, and net worth")
    void displaysAccountsAndNetWorth() throws Exception {
        service.createCashAccount("CashPocket", Money.of("50.00"));
        service.createBankAccount("Checking", Money.of("450.00"));

        String script = String.join("\n",
                "1",
                "8",
                ""
        );

        String output = runScript(script);

        assertThat(output)
                .contains("--- Accounts ---")
                .contains("CashPocket")
                .contains("Checking")
                .contains("Total Net Worth: 500.00");
    }

    @Test
    @DisplayName("Handles invalid menu choice and invalid amount gracefully with readable errors")
    void handlesInvalidInputAndDisplaysFriendlyMessage() {
        String script = String.join("\n",
                "9",
                "2",
                "1",
                "Emergency",
                "invalid-money",
                "8",
                ""
        );

        String output = runScript(script);

        assertThat(output)
                .contains("Invalid option: 9. Please select 1-8.")
                .contains("Error: Invalid amount format: invalid-money")
                .contains("Goodbye!");
    }

    @Test
    @DisplayName("Transfers funds between accounts and displays both updated balances")
    void transfersFundsAndDisplaysBothNewBalances() throws Exception {
        service.createCashAccount("Wallet", Money.of("200.00"));
        service.createBankAccount("Savings", Money.of("300.00"));

        String script = String.join("\n",
                "5",
                "Wallet",
                "Savings",
                "50.00",
                "Monthly Savings",
                "8",
                ""
        );

        String output = runScript(script);

        assertThat(output)
                .contains("Transfer completed successfully.")
                .contains("From Account (Wallet) new balance: 150.00")
                .contains("To Account (Savings) new balance: 350.00");
    }

    @Test
    @DisplayName("Category management: creates and lists categories through console")
    void managesCategoriesThroughConsole() {
        String script = String.join("\n",
                "6",
                "2",
                "Dining",
                "EXPENSE",
                "",
                "6",
                "1",
                "8",
                ""
        );

        String output = runScript(script);

        assertThat(output)
                .contains("Category created successfully")
                .contains("Dining");
    }
}
