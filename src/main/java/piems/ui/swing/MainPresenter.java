package piems.ui.swing;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import piems.domain.Account;
import piems.domain.Budget;
import piems.domain.BudgetStatus;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.service.FinanceService;
import piems.service.TransactionView;

/**
 * Presenter coordinating user interactions, view updates, and facade invocations (MVP pattern).
 *
 * <p>Enforces zero business logic in the UI, offloads data access to an asynchronous
 * background {@link Executor}, and handles all exceptions via the top-level handler (D-05, D-15, D-40).</p>
 */
public class MainPresenter {

    private final FinanceService financeService;
    private final MainView view;
    private final Executor backgroundExecutor;
    private final Executor uiExecutor;
    private volatile FilterCriteria currentFilter = new FilterCriteria(null, null, null, null, null);

    private record FilterCriteria(
            String text,
            Long accountId,
            Long categoryId,
            LocalDate startDate,
            LocalDate endDate
    ) { }

    /**
     * Constructs a presenter with asynchronous background worker and UI thread executors.
     *
     * @param financeService facade service
     * @param view           visual presentation contract
     */
    public MainPresenter(FinanceService financeService, MainView view) {
        this(financeService, view, Executors.newSingleThreadExecutor(), Runnable::run);
    }

    /**
     * Constructs a presenter with explicit background and UI dispatch executors.
     *
     * @param financeService     facade service
     * @param view               visual presentation contract
     * @param backgroundExecutor executor for background tasks
     * @param uiExecutor         executor for view updates
     */
    public MainPresenter(
            FinanceService financeService,
            MainView view,
            Executor backgroundExecutor,
            Executor uiExecutor
    ) {
        this.financeService = Objects.requireNonNull(financeService, "financeService cannot be null");
        this.view = Objects.requireNonNull(view, "view cannot be null");
        this.backgroundExecutor = Objects.requireNonNull(backgroundExecutor, "backgroundExecutor cannot be null");
        this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor cannot be null");
        this.view.setPresenter(this);
    }

    /**
     * Initializes the view by loading accounts, categories, and initial transactions.
     */
    public void initialize() {
        runAsync(() -> {
            List<Account> accounts = financeService.listAccounts();
            List<CategoryComponent> categories = financeService.listCategories();
            List<TransactionView> transactions = financeService.listAllTransactions();
            uiExecutor.execute(() -> {
                view.setAccounts(accounts);
                view.setCategories(categories);
                view.showTransactions(transactions);
            });
        });
    }

    /**
     * Refreshes the transaction listing using the active query filter.
     */
    public void refreshTransactions() {
        runAsync(() -> {
            List<TransactionView> txs = financeService.findTransactions(
                    currentFilter.text(),
                    currentFilter.accountId(),
                    currentFilter.categoryId(),
                    currentFilter.startDate(),
                    currentFilter.endDate()
            );
            uiExecutor.execute(() -> view.showTransactions(txs));
        });
    }

    /**
     * Applies search and filter criteria to the transaction table (FR-08).
     *
     * @param text       optional note text search
     * @param accountId  optional account identifier filter
     * @param categoryId optional category identifier filter
     * @param startDate  optional start date
     * @param endDate    optional end date
     */
    public void applyFilter(String text, Long accountId, Long categoryId, LocalDate startDate, LocalDate endDate) {
        this.currentFilter = new FilterCriteria(text, accountId, categoryId, startDate, endDate);
        refreshTransactions();
    }

    /**
     * Records an expense transaction through the service facade (FR-03).
     *
     * @param accountId  selected account identifier
     * @param categoryId optional category identifier
     * @param amountText text representation of amount
     * @param date       optional date
     * @param note       optional note
     */
    public void addExpense(Long accountId, Long categoryId, String amountText, LocalDate date, String note) {
        if (accountId == null) {
            view.showErrorMessage("Please select an account.");
            return;
        }
        if (amountText == null || amountText.trim().isEmpty()) {
            view.showErrorMessage("Please enter an expense amount.");
            return;
        }
        runAsync(() -> {
            Money amount = Money.of(amountText.trim());
            financeService.recordExpense(accountId, categoryId, amount, date, note);
            uiExecutor.execute(() -> {
                view.showSuccessMessage("Expense recorded successfully: " + amount);
                view.clearExpenseInputs();
                refreshTransactions();
                if (categoryId != null) {
                    checkBudgetStatus(categoryId, YearMonth.from(date != null ? date : LocalDate.now()));
                }
            });
        });
    }

    /**
     * Evaluates and updates budget status with colour and text for a category and month (FR-10).
     *
     * @param categoryId category identifier
     * @param month      calendar month
     */
    public void checkBudgetStatus(Long categoryId, YearMonth month) {
        if (categoryId == null || month == null) {
            uiExecutor.execute(() -> view.showBudgetStatus("[NO BUDGET] No category selected", null));
            return;
        }
        runAsync(() -> {
            Optional<Budget> budgetOpt = financeService.getBudget(categoryId, month);
            if (budgetOpt.isEmpty()) {
                uiExecutor.execute(() -> view.showBudgetStatus("[NO BUDGET] No budget set for " + month, null));
                return;
            }
            Budget budget = budgetOpt.get();
            Money spent = financeService.getCategoryMonthlyExpense(categoryId, month);
            BudgetStatus status = budget.evaluate(spent);
            String desc = String.format("[%s] Spent %s of %s limit (%s)", status, spent, budget.getLimit(), month);
            uiExecutor.execute(() -> view.showBudgetStatus(desc, status));
        });
    }

    /**
     * Top-level uncaught exception handler routing unexpected errors to the view.
     *
     * @param throwable error or exception
     */
    public void handleUncaughtException(Throwable throwable) {
        String message = (throwable != null && throwable.getMessage() != null)
                ? throwable.getMessage()
                : "An unexpected error occurred.";
        uiExecutor.execute(() -> view.showErrorMessage(message));
    }

    /**
     * Functional interface for tasks that may throw checked domain or persistence exceptions.
     */
    @FunctionalInterface
    public interface ThrowingTask {
        /**
         * Executes the operation.
         *
         * @throws Exception on error
         */
        void run() throws Exception;
    }

    private void runAsync(ThrowingTask task) {
        uiExecutor.execute(() -> view.setBusy(true));
        backgroundExecutor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                handleUncaughtException(e);
            } finally {
                uiExecutor.execute(() -> view.setBusy(false));
            }
        });
    }
}
