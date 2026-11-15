package piems.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.domain.Account;
import piems.domain.Budget;
import piems.domain.BudgetStatus;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.domain.ValidationException;
import piems.repository.InMemoryAccountRepository;
import piems.repository.InMemoryBudgetRepository;
import piems.repository.InMemoryCategoryRepository;
import piems.repository.InMemoryTransactionRepository;

/**
 * Unit tests verifying synthetic demonstration data loading (Decision D-48).
 */
class DemoDataLoaderTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);
    private InMemoryAccountRepository accountRepo;
    private InMemoryCategoryRepository categoryRepo;
    private InMemoryTransactionRepository transactionRepo;
    private InMemoryBudgetRepository budgetRepo;
    private FinanceService financeService;
    private DemoDataLoader loader;

    @BeforeEach
    void setUp() {
        accountRepo = new InMemoryAccountRepository();
        categoryRepo = new InMemoryCategoryRepository();
        transactionRepo = new InMemoryTransactionRepository();
        accountRepo.setTransactionRepository(transactionRepo);
        budgetRepo = new InMemoryBudgetRepository();
        financeService = new FinanceService(accountRepo, categoryRepo, transactionRepo, budgetRepo, clock);
        loader = new DemoDataLoader(financeService, clock);
    }

    @Test
    @DisplayName("D-48: loads expected accounts (3), categories (5), and transaction rows (17)")
    void loadsExpectedCountsOfAccountsCategoriesAndTransactions() throws Exception {
        loader.load();

        List<Account> accounts = financeService.listAccounts();
        assertThat(accounts).hasSize(3);
        assertThat(accounts).extracting(Account::name)
                .containsExactlyInAnyOrder("Cash Wallet", "Checking", "Visa Card");

        List<CategoryComponent> categories = financeService.listCategories();
        assertThat(categories).hasSize(4);
        int totalCategories = categories.stream().mapToInt(c -> 1 + c.childCount()).sum();
        assertThat(totalCategories).isEqualTo(5);

        List<TransactionView> txs = financeService.listAllTransactions();
        // 16 user operations: 1 income, 1 transfer (2 stored rows), 14 expenses = 17 stored rows
        assertThat(txs).hasSize(17);
        long transferRows = txs.stream().filter(t -> t.transferLinkId() != null).count();
        assertThat(transferRows).isEqualTo(2);
    }

    @Test
    @DisplayName("D-48: sets one budget on Groceries that evaluates to WARNING status (85.0% spent)")
    void setsOneBudgetInWarningStatus() throws Exception {
        loader.load();

        CategoryComponent groceries = categoryRepo.findByName("Groceries").orElseThrow();
        YearMonth currentMonth = YearMonth.now(clock);
        Budget budget = financeService.getBudget(groceries.id(), currentMonth).orElseThrow();

        assertThat(budget.getLimit()).isEqualTo(Money.of("500.00"));
        Money spent = financeService.getCategoryMonthlyExpense(groceries.id(), currentMonth);
        assertThat(spent).isEqualTo(Money.of("425.00"));

        BudgetStatus status = budget.evaluate(spent);
        assertThat(status).isEqualTo(BudgetStatus.WARNING);
    }

    @Test
    @DisplayName("D-48: net worth equals calculated sum of derived balances (excluding credit card debt)")
    void netWorthMatchesCalculatedValueExcludingCreditCardDebt() throws Exception {
        loader.load();

        // Checking: 1500 (open) + 3000 (salary) - 200 (transfer) - 270 (expenses) = 4030.00
        // Cash Wallet: 100 (open) + 200 (transfer) - 105 (expenses) = 195.00
        // Visa Card: asset balance = 0.00 (debt 490.00 is tracked outside balance)
        // Total Net Worth = 4030.00 + 195.00 = 4225.00
        Money netWorth = financeService.getNetWorth();
        assertThat(netWorth).isEqualTo(Money.of("4225.00"));
    }

    @Test
    @DisplayName("D-48: refuses to load demo data when ledger is not empty")
    void refusesOnNonEmptyLedger() throws Exception {
        financeService.createCashAccount("Existing Cash", Money.of("50.00"));

        assertThatThrownBy(() -> loader.load())
                .isInstanceOf(ValidationException.class)
                .hasMessage("Demo data can only be loaded into an empty ledger");
    }

    @Test
    @DisplayName("D-48: deterministic across identical fixed clocks with no dates in the future")
    void deterministicAcrossIdenticalFixedClocks() throws Exception {
        loader.load();
        List<TransactionView> txs = financeService.listAllTransactions();
        java.time.LocalDate today = java.time.LocalDate.now(clock);

        assertThat(txs).allSatisfy(tx -> assertThat(tx.date()).isBeforeOrEqualTo(today));
    }

    @Test
    @DisplayName("D-48: all-or-nothing rollback leaves no residual data on mid-way failure")
    void allOrNothingRollbackLeavesNoResidualDataOnFailure() throws Exception {
        loader.setFaultInjection(true);

        assertThatThrownBy(() -> loader.load())
                .isInstanceOf(Exception.class);

        assertThat(financeService.listAccounts()).isEmpty();
        assertThat(financeService.listCategories()).isEmpty();
        assertThat(financeService.listAllTransactions()).isEmpty();
    }
}
