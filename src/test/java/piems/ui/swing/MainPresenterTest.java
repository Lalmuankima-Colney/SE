package piems.ui.swing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.domain.Account;
import piems.domain.BudgetStatus;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.repository.InMemoryAccountRepository;
import piems.repository.InMemoryCategoryRepository;
import piems.repository.InMemoryTransactionRepository;
import piems.service.FinanceService;
import piems.service.TransactionView;

/**
 * Headless unit and presenter test suite for the Swing desktop interface (Decision D-40).
 *
 * <p>Tests presenter coordination, validation, filtering, budget status badge updates,
 * and top-level error handling without initializing concrete Swing widgets or graphical displays.</p>
 */
class MainPresenterTest {

    private FinanceService service;
    private FakeMainView fakeView;
    private MainPresenter presenter;
    private Account bankAccount;
    private CategoryComponent foodCategory;

    @BeforeEach
    void setUp() throws Exception {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneId.of("UTC"));
        InMemoryTransactionRepository txRepo = new InMemoryTransactionRepository();
        InMemoryAccountRepository accRepo = new InMemoryAccountRepository(txRepo);
        InMemoryCategoryRepository catRepo = new InMemoryCategoryRepository(txRepo);
        service = new FinanceService(accRepo, catRepo, txRepo, fixedClock);

        bankAccount = service.createBankAccount("Checking", Money.of("1000.00"));
        foodCategory = service.createCategory("Food", "EXPENSE", null);

        fakeView = new FakeMainView();
        presenter = new MainPresenter(service, fakeView, Runnable::run, Runnable::run);
    }

    @Test
    @DisplayName("TC-FR07-03: initialize populates accounts, categories, and initial transactions")
    void initializeLoadsAccountsCategoriesAndTransactions() {
        presenter.initialize();

        assertThat(fakeView.accounts).hasSize(1);
        assertThat(fakeView.accounts.get(0).name()).isEqualTo("Checking");
        assertThat(fakeView.categories).hasSize(1);
        assertThat(fakeView.categories.get(0).name()).isEqualTo("Food");
        assertThat(fakeView.transactions).isEmpty();
    }

    @Test
    @DisplayName("TC-FR03-05: addExpense records expense through facade, updates view and clears form")
    void addExpenseSuccessfullyRecordsExpenseAndRefreshesView() {
        presenter.addExpense(bankAccount.id(), foodCategory.id(), "150.00", LocalDate.of(2026, 3, 10), "Lunch");

        assertThat(fakeView.lastSuccessMessage).contains("150.00");
        assertThat(fakeView.expenseInputsCleared).isTrue();
        assertThat(fakeView.transactions).hasSize(1);
        assertThat(fakeView.transactions.get(0).amount()).isEqualTo(Money.of("150.00"));
    }

    @Test
    @DisplayName("TC-FR03-06: addExpense with insufficient funds routes error to top-level handler")
    void addExpenseWithInsufficientFundsDisplaysErrorMessage() {
        presenter.addExpense(bankAccount.id(), foodCategory.id(), "2000.00", null, "Too expensive");

        assertThat(fakeView.lastErrorMessage).contains("exceeds current balance");
        assertThat(fakeView.transactions).isEmpty();
    }

    @Test
    @DisplayName("TC-FR19-03: addExpense with invalid amount text displays validation error")
    void addExpenseWithInvalidAmountDisplaysErrorMessage() {
        presenter.addExpense(bankAccount.id(), foodCategory.id(), "invalid-amount", null, "Bad input");

        assertThat(fakeView.lastErrorMessage).isNotEmpty();
    }

    @Test
    @DisplayName("TC-FR08-02: applyFilter executes TransactionQuery and updates table")
    void applyFilterQueriesMatchingTransactionsAndUpdatesView() {
        presenter.addExpense(bankAccount.id(), foodCategory.id(), "50.00", LocalDate.of(2026, 3, 5), "Coffee");
        presenter.addExpense(bankAccount.id(), foodCategory.id(), "200.00", LocalDate.of(2026, 3, 8), "Dinner");

        presenter.applyFilter("Coffee", null, null, null, null);

        assertThat(fakeView.transactions).hasSize(1);
        assertThat(fakeView.transactions.get(0).note()).isEqualTo("Coffee");
    }

    @Test
    @DisplayName("TC-FR10-07: checkBudgetStatus displays WARNING at 80% spending threshold")
    void checkBudgetStatusDisplaysWarningWithColorAndTextAtThreshold() throws Exception {
        service.setBudget(foodCategory, YearMonth.of(2026, 3), Money.of("1000.00"));
        presenter.addExpense(bankAccount.id(), foodCategory.id(), "800.00", LocalDate.of(2026, 3, 12), "Groceries");

        presenter.checkBudgetStatus(foodCategory.id(), YearMonth.of(2026, 3));

        assertThat(fakeView.lastBudgetStatus).isEqualTo(BudgetStatus.WARNING);
        assertThat(fakeView.lastBudgetStatusText).contains("[WARNING]").contains("800.00");
    }

    @Test
    @DisplayName("TC-FR10-08: checkBudgetStatus displays EXCEEDED at 100% budget limit")
    void checkBudgetStatusDisplaysExceededWithColorAndTextAtLimit() throws Exception {
        service.setBudget(foodCategory, YearMonth.of(2026, 3), Money.of("500.00"));
        presenter.addExpense(bankAccount.id(), foodCategory.id(), "500.00", LocalDate.of(2026, 3, 14), "Party");

        presenter.checkBudgetStatus(foodCategory.id(), YearMonth.of(2026, 3));

        assertThat(fakeView.lastBudgetStatus).isEqualTo(BudgetStatus.EXCEEDED);
        assertThat(fakeView.lastBudgetStatusText).contains("[EXCEEDED]");
    }

    @Test
    @DisplayName("TC-FR10-09: checkBudgetStatus displays OK when spending is well within budget")
    void checkBudgetStatusDisplaysOkWithColorAndTextWhenWithinLimit() throws Exception {
        service.setBudget(foodCategory, YearMonth.of(2026, 3), Money.of("1000.00"));
        presenter.addExpense(bankAccount.id(), foodCategory.id(), "200.00", LocalDate.of(2026, 3, 14), "Snacks");

        presenter.checkBudgetStatus(foodCategory.id(), YearMonth.of(2026, 3));

        assertThat(fakeView.lastBudgetStatus).isEqualTo(BudgetStatus.OK);
        assertThat(fakeView.lastBudgetStatusText).contains("[OK]");
    }

    @Test
    @DisplayName("handleUncaughtException routes unexpected errors to view error handler (supporting D-13, D-40)")
    void topLevelExceptionHandlerCatchesUnexpectedExceptions() {
        presenter.handleUncaughtException(new RuntimeException("Simulated unexpected failure"));

        assertThat(fakeView.lastErrorMessage).contains("Simulated unexpected failure");
    }

    @Test
    @DisplayName("D-48: addExpense with no accounts shows clear empty state message")
    void addExpenseWithNoAccountsShowsHelpfulEmptyStateMessage() throws Exception {
        // Setup empty service with no accounts
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneId.of("UTC"));
        InMemoryTransactionRepository txRepo = new InMemoryTransactionRepository();
        InMemoryAccountRepository accRepo = new InMemoryAccountRepository(txRepo);
        InMemoryCategoryRepository catRepo = new InMemoryCategoryRepository(txRepo);
        FinanceService emptyService = new FinanceService(accRepo, catRepo, txRepo, fixedClock);
        FakeMainView emptyView = new FakeMainView();
        MainPresenter emptyPresenter = new MainPresenter(emptyService, emptyView, Runnable::run, Runnable::run);

        emptyPresenter.addExpense(null, null, "100.00", LocalDate.of(2026, 3, 15), "Lunch");

        assertThat(emptyView.lastErrorMessage).isEqualTo("Create an account first, or load demo data");
    }

    @Test
    @DisplayName("D-48: loadDemoData populates demo accounts, categories, and transactions into empty view")
    void loadDemoDataRefreshesViewWithAccountsCategoriesAndTransactions() {
        // Setup empty service
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneId.of("UTC"));
        InMemoryTransactionRepository txRepo = new InMemoryTransactionRepository();
        InMemoryAccountRepository accRepo = new InMemoryAccountRepository(txRepo);
        InMemoryCategoryRepository catRepo = new InMemoryCategoryRepository(txRepo);
        FinanceService emptyService = new FinanceService(accRepo, catRepo, txRepo, fixedClock);
        FakeMainView emptyView = new FakeMainView();
        MainPresenter emptyPresenter = new MainPresenter(emptyService, emptyView, Runnable::run, Runnable::run);

        emptyPresenter.loadDemoData();

        assertThat(emptyView.lastSuccessMessage).isEqualTo("Demo data loaded successfully.");
        assertThat(emptyView.accounts).hasSize(3);
        assertThat(emptyView.categories).hasSize(4);
        int totalCategories = emptyView.categories.stream().mapToInt(c -> 1 + c.childCount()).sum();
        assertThat(totalCategories).isEqualTo(5);
        assertThat(emptyView.transactions).hasSize(17);
    }

    @Test
    @DisplayName("D-48: loadDemoData displays refusal error message when account already exists")
    void loadDemoDataRefusesWhenAccountAlreadyExists() {
        // bankAccount already created in setUp()
        presenter.loadDemoData();

        assertThat(fakeView.lastErrorMessage).isEqualTo("Demo data can only be loaded into an empty ledger");
    }

    @Test
    @DisplayName("D-48: table model renders Account and Category names instead of numeric IDs")
    void tableModelDisplaysNamesInsteadOfIds() {
        TransactionTableModel model = new TransactionTableModel();
        assertThat(model.getColumnName(2)).isEqualTo("Account");
        assertThat(model.getColumnName(3)).isEqualTo("Category");

        TransactionView viewWithNames = new TransactionView(
                1L, "EXPENSE", LocalDate.of(2026, 3, 10), Money.of("45.00"), "Bakery",
                10L, null, 20L, "Cash Wallet", "Groceries"
        );
        TransactionView viewNoCategory = new TransactionView(
                2L, "EXPENSE", LocalDate.of(2026, 3, 11), Money.of("15.00"), "Tips",
                10L, null, null, "Cash Wallet", null
        );
        model.setTransactions(List.of(viewWithNames, viewNoCategory));

        assertThat(model.getValueAt(0, 2)).isEqualTo("Cash Wallet");
        assertThat(model.getValueAt(0, 3)).isEqualTo("Groceries");
        assertThat(model.getValueAt(1, 2)).isEqualTo("Cash Wallet");
        assertThat(model.getValueAt(1, 3)).isEqualTo("(none)");
    }

    @Test
    @DisplayName("D-49: addAccount creates account through facade and refreshes accounts and balances")
    void addAccountSuccessfullyPersistsAccountAndRefreshesView() {
        presenter.addAccount("Wallet", "CASH", "250.00");

        assertThat(fakeView.lastSuccessMessage).isEqualTo("Account created: Wallet");
        assertThat(fakeView.accounts).hasSize(2);
        assertThat(fakeView.accounts).extracting(Account::name).contains("Checking", "Wallet");
        assertThat(fakeView.balances).hasSize(2);
    }

    @Test
    @DisplayName("D-49: addCategory creates category through facade and refreshes view")
    void addCategorySuccessfullyPersistsCategoryAndRefreshesView() {
        presenter.addCategory("Utilities", "EXPENSE", null);

        assertThat(fakeView.lastSuccessMessage).isEqualTo("Category created: Utilities");
        assertThat(fakeView.categories).hasSize(2);
        assertThat(fakeView.categories).extracting(CategoryComponent::name).contains("Food", "Utilities");
    }

    @Test
    @DisplayName("D-49: clearExpenseForm clears inputs and resets budget status")
    void clearExpenseFormResetsInputsAndBudgetStatus() {
        presenter.clearExpenseForm();

        assertThat(fakeView.expenseInputsCleared).isTrue();
        assertThat(fakeView.lastBudgetStatusText).isEqualTo("[NO BUDGET] Select category");
        assertThat(fakeView.lastBudgetStatus).isNull();
    }

    @Test
    @DisplayName("D-49: initialize populates account balances and calculates net worth")
    void initializePopulatesBalancesAndNetWorth() {
        presenter.initialize();

        assertThat(fakeView.balances).hasSize(1);
        assertThat(fakeView.balances.get(0).name()).isEqualTo("Checking");
        assertThat(fakeView.balances.get(0).balance()).isEqualTo(Money.of("1000.00"));
        assertThat(fakeView.lastNetWorth).isEqualTo(Money.of("1000.00"));
    }

    @Test
    @DisplayName("D-50: Reset button in Search & Filter clears table sorter sort keys and selection")
    void resetFilterClearsSortKeysAndSelection() throws Exception {
        MainFrame frame = new MainFrame();
        var sorterField = MainFrame.class.getDeclaredField("sorter");
        sorterField.setAccessible(true);
        var sorter = (javax.swing.table.TableRowSorter<?>) sorterField.get(frame);

        var resetBtnField = MainFrame.class.getDeclaredField("resetButton");
        resetBtnField.setAccessible(true);
        var resetBtn = (javax.swing.JButton) resetBtnField.get(frame);

        sorter.setSortKeys(List.of(new javax.swing.RowSorter.SortKey(0, javax.swing.SortOrder.DESCENDING)));
        assertThat(sorter.getSortKeys()).isNotEmpty();

        resetBtn.doClick();

        assertThat(sorter.getSortKeys()).isNullOrEmpty();
    }

    @Test
    @DisplayName("D-50: Clear Form button resets account selection to default and updates balance label")
    void clearExpenseFormResetsAccountSelectionAndBalance() throws Exception {
        MainFrame frame = new MainFrame();
        frame.setAccounts(List.of(
                new piems.domain.BankAccount(1L, "Checking", Money.of("1000.00"), false),
                new piems.domain.BankAccount(2L, "Savings", Money.of("5000.00"), false)
        ));
        frame.showAccountBalances(List.of(
                new AccountBalanceItem(1L, "Checking", "BANK", Money.of("1000.00")),
                new AccountBalanceItem(2L, "Savings", "BANK", Money.of("5000.00"))
        ), Money.of("6000.00"));

        var accComboField = MainFrame.class.getDeclaredField("accountCombo");
        accComboField.setAccessible(true);
        var accCombo = (javax.swing.JComboBox<?>) accComboField.get(frame);

        accCombo.setSelectedIndex(1);
        assertThat(accCombo.getSelectedIndex()).isEqualTo(1);

        frame.clearExpenseInputs();

        assertThat(accCombo.getSelectedIndex()).isEqualTo(0);
    }

    @Test
    @DisplayName("D-50: setAccounts retains currently selected account when reloaded")
    void setAccountsRetainsCurrentlySelectedAccount() throws Exception {
        MainFrame frame = new MainFrame();
        Account acc1 = new piems.domain.BankAccount(1L, "Checking", Money.of("1000.00"), false);
        Account acc2 = new piems.domain.BankAccount(2L, "Savings", Money.of("5000.00"), false);
        frame.setAccounts(List.of(acc1, acc2));

        var accComboField = MainFrame.class.getDeclaredField("accountCombo");
        accComboField.setAccessible(true);
        var accCombo = (javax.swing.JComboBox<?>) accComboField.get(frame);

        accCombo.setSelectedIndex(1);
        assertThat(accCombo.getSelectedIndex()).isEqualTo(1);

        // Simulate refresh (e.g. after adding another account or reload)
        Account acc3 = new piems.domain.CashAccount(3L, "Cash", Money.of("200.00"), false);
        frame.setAccounts(List.of(acc1, acc2, acc3));

        // Must still select acc2 (index 1), not be wiped to index 0!
        assertThat(accCombo.getSelectedIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("D-50: Record Expense category dropdown includes only EXPENSE categories")
    void expenseFormCategoryComboExcludesIncomeCategories() throws Exception {
        MainFrame frame = new MainFrame();
        CategoryComponent exp = service.createCategory("Health", "EXPENSE", null);
        CategoryComponent inc = service.createCategory("Salary", "INCOME", null);
        frame.setCategories(List.of(exp, inc));

        var catComboField = MainFrame.class.getDeclaredField("categoryCombo");
        catComboField.setAccessible(true);
        var catCombo = (javax.swing.JComboBox<?>) catComboField.get(frame);

        // Expected: (None) + Health (2 items). Salary (INCOME) must not appear in expense form!
        assertThat(catCombo.getItemCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("D-50: Account combo box has sufficient width (>150px) preventing label squeeze")
    void layoutExpandsAccountComboWidth() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless());
        MainFrame frame = new MainFrame();
        frame.pack();
        frame.setSize(1040, 640);
        frame.validate();

        var accComboField = MainFrame.class.getDeclaredField("accountCombo");
        accComboField.setAccessible(true);
        var accCombo = (javax.swing.JComboBox<?>) accComboField.get(frame);

        assertThat(accCombo.getWidth()).isGreaterThanOrEqualTo(160);
    }

    private static class FakeMainView implements MainView {
        private List<Account> accounts = new ArrayList<>();
        private List<CategoryComponent> categories = new ArrayList<>();
        private List<TransactionView> transactions = new ArrayList<>();
        private List<AccountBalanceItem> balances = new ArrayList<>();
        private Money lastNetWorth;
        private String lastErrorMessage;
        private String lastSuccessMessage;
        private String lastBudgetStatusText;
        private BudgetStatus lastBudgetStatus;
        private boolean expenseInputsCleared;
        private boolean busy;

        @Override
        public void setPresenter(MainPresenter presenter) {
        }

        @Override
        public void showTransactions(List<TransactionView> transactions) {
            this.transactions = transactions;
        }

        @Override
        public void showAccountBalances(List<AccountBalanceItem> balances, Money netWorth) {
            this.balances = balances;
            this.lastNetWorth = netWorth;
        }

        @Override
        public void setAccounts(List<Account> accounts) {
            this.accounts = accounts;
        }

        @Override
        public void setCategories(List<CategoryComponent> categories) {
            this.categories = categories;
        }

        @Override
        public void showBudgetStatus(String statusText, BudgetStatus status) {
            this.lastBudgetStatusText = statusText;
            this.lastBudgetStatus = status;
        }

        @Override
        public void showErrorMessage(String message) {
            this.lastErrorMessage = message;
        }

        @Override
        public void showSuccessMessage(String message) {
            this.lastSuccessMessage = message;
        }

        @Override
        public void clearExpenseInputs() {
            this.expenseInputsCleared = true;
        }

        @Override
        public void setBusy(boolean busy) {
            this.busy = busy;
        }
    }
}
