package piems.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import piems.domain.Account;
import piems.domain.CategoryComponent;
import piems.domain.InsufficientFundsException;
import piems.domain.Money;
import piems.domain.PersistenceException;
import piems.domain.RecordNotFoundException;
import piems.domain.ValidationException;

/**
 * Service loader populating deterministic synthetic demonstration data into an empty ledger (Decision D-48).
 *
 * <p>Enforces all-or-nothing atomicity by rolling back all created entities in reverse dependency
 * order upon any mid-way failure.</p>
 */
public class DemoDataLoader {

    private final FinanceService financeService;
    private final Clock clock;
    private boolean faultInjection = false;

    /**
     * Constructs a demo data loader with injected service and clock.
     *
     * @param financeService application facade
     * @param clock          deterministic clock provider
     */
    public DemoDataLoader(FinanceService financeService, Clock clock) {
        this.financeService = Objects.requireNonNull(financeService, "financeService cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    /**
     * Sets fault injection flag for all-or-nothing rollback verification.
     *
     * @param faultInjection true to inject a failure mid-way
     */
    public void setFaultInjection(boolean faultInjection) {
        this.faultInjection = faultInjection;
    }

    /**
     * Loads sample accounts, categories, transactions, and a budget into an empty ledger.
     *
     * @throws ValidationException        if ledger already has accounts
     * @throws PersistenceException       if database access fails
     * @throws RecordNotFoundException    if referenced entity not found
     * @throws InsufficientFundsException if account balance is insufficient
     */
    public void load() throws ValidationException, PersistenceException,
            RecordNotFoundException, InsufficientFundsException {
        if (!financeService.listAccounts().isEmpty()) {
            throw new ValidationException("Demo data can only be loaded into an empty ledger");
        }

        List<Long> accIds = new ArrayList<>();
        List<Long> catIds = new ArrayList<>();
        List<Long> txIds = new ArrayList<>();
        List<BudgetRef> budgets = new ArrayList<>();

        try {
            Account[] accs = createDemoAccounts(accIds);
            CategoryComponent[] cats = createDemoCategories(catIds);

            if (faultInjection) {
                throw new PersistenceException("Injected fault during demo data loading");
            }

            LocalDate today = LocalDate.now(clock);
            createDemoBudget(cats[2], budgets, today);
            recordDemoTransactions(accs, cats, today, txIds);
        } catch (Exception e) {
            rollback(txIds, budgets, catIds, accIds);
            throw e;
        }
    }

    private Account[] createDemoAccounts(List<Long> accIds) throws PersistenceException, ValidationException {
        Account cash = financeService.createCashAccount("Cash Wallet", Money.of("100.00"));
        accIds.add(cash.id());
        Account bank = financeService.createBankAccount("Checking", Money.of("1500.00"));
        accIds.add(bank.id());
        Account cc = financeService.createCreditCardAccount("Visa Card", Money.of("1000.00"));
        accIds.add(cc.id());
        return new Account[]{bank, cash, cc};
    }

    private CategoryComponent[] createDemoCategories(List<Long> catIds)
            throws PersistenceException, ValidationException, RecordNotFoundException {
        CategoryComponent living = financeService.createCategoryGroup("Living Expenses", "EXPENSE", null);
        catIds.add(living.id());
        CategoryComponent utils = financeService.createCategory("Utilities", "EXPENSE", living.id());
        catIds.add(utils.id());
        CategoryComponent groc = financeService.createCategory("Groceries", "EXPENSE", null);
        catIds.add(groc.id());
        CategoryComponent dining = financeService.createCategory("Dining Out", "EXPENSE", null);
        catIds.add(dining.id());
        CategoryComponent salary = financeService.createCategory("Salary", "INCOME", null);
        catIds.add(salary.id());
        return new CategoryComponent[]{salary, utils, groc, dining};
    }

    private void createDemoBudget(CategoryComponent groc, List<BudgetRef> budgets, LocalDate today)
            throws PersistenceException, ValidationException, RecordNotFoundException {
        YearMonth currentMonth = YearMonth.from(today);
        financeService.setBudget(groc.id(), currentMonth, Money.of("500.00"));
        budgets.add(new BudgetRef(groc.id(), currentMonth));
    }

    private void recordDemoTransactions(
            Account[] accs,
            CategoryComponent[] cats,
            LocalDate today,
            List<Long> txIds
    ) throws ValidationException, PersistenceException, RecordNotFoundException, InsufficientFundsException {
        Account bank = accs[0];
        Account cash = accs[1];
        Account cc = accs[2];
        CategoryComponent salary = cats[0];
        CategoryComponent utils = cats[1];
        CategoryComponent groc = cats[2];
        CategoryComponent dining = cats[3];

        txIds.add(financeService.recordIncome(
                bank.id(), salary.id(), Money.of("3000.00"), d(today, 1), "Monthly Salary").id());
        financeService.recordTransfer(
                bank.id(), cash.id(), Money.of("200.00"), d(today, 2), "ATM Withdrawal");

        recordDemoExpensesPart1(bank, cash, cc, groc, utils, dining, today, txIds);
        recordDemoExpensesPart2(bank, cash, cc, groc, utils, dining, today, txIds);
    }

    private void recordDemoExpensesPart1(
            Account bank, Account cash, Account cc,
            CategoryComponent groc, CategoryComponent utils, CategoryComponent dining,
            LocalDate today, List<Long> txIds
    ) throws ValidationException, PersistenceException, RecordNotFoundException, InsufficientFundsException {
        txIds.add(financeService.recordExpense(
                cash.id(), groc.id(), Money.of("45.00"), d(today, 3), "Local bakery").id());
        txIds.add(financeService.recordExpense(
                bank.id(), utils.id(), Money.of("120.00"), d(today, 4), "Electric bill").id());
        txIds.add(financeService.recordExpense(
                cc.id(), dining.id(), Money.of("35.00"), d(today, 5), "Lunch with team").id());
        txIds.add(financeService.recordExpense(
                cc.id(), groc.id(), Money.of("150.00"), d(today, 6), "Supermarket run").id());
        txIds.add(financeService.recordExpense(
                cash.id(), dining.id(), Money.of("20.00"), d(today, 7), "Coffee & snacks").id());
        txIds.add(financeService.recordExpense(
                bank.id(), utils.id(), Money.of("60.00"), d(today, 8), "Internet subscription").id());
        txIds.add(financeService.recordExpense(
                cc.id(), groc.id(), Money.of("130.00"), d(today, 9), "Weekly vegetables").id());
    }

    private void recordDemoExpensesPart2(
            Account bank, Account cash, Account cc,
            CategoryComponent groc, CategoryComponent utils, CategoryComponent dining,
            LocalDate today, List<Long> txIds
    ) throws ValidationException, PersistenceException, RecordNotFoundException, InsufficientFundsException {
        txIds.add(financeService.recordExpense(
                cash.id(), dining.id(), Money.of("25.00"), d(today, 10), "Takeout dinner").id());
        txIds.add(financeService.recordExpense(
                cc.id(), groc.id(), Money.of("100.00"), d(today, 11), "Pantry staples").id());
        txIds.add(financeService.recordExpense(
                bank.id(), utils.id(), Money.of("40.00"), d(today, 12), "Water bill").id());
        txIds.add(financeService.recordExpense(
                cc.id(), dining.id(), Money.of("45.00"), d(today, 13), "Weekend bistro").id());
        txIds.add(financeService.recordExpense(
                cash.id(), null, Money.of("15.00"), d(today, 14), "Newspaper & tips").id());
        txIds.add(financeService.recordExpense(
                cc.id(), dining.id(), Money.of("30.00"), d(today, 15), "Coffee break").id());
        txIds.add(financeService.recordExpense(
                bank.id(), null, Money.of("50.00"), d(today, 15), "Bank fee").id());
    }

    private LocalDate d(LocalDate today, int dayNum) {
        int clampedDay = Math.min(dayNum, today.getDayOfMonth());
        return today.withDayOfMonth(clampedDay);
    }

    private void rollback(
            List<Long> txIds,
            List<BudgetRef> budgets,
            List<Long> catIds,
            List<Long> accIds
    ) {
        for (Long txId : txIds) {
            try {
                financeService.deleteTransaction(txId);
            } catch (Exception ignored) {
            }
        }
        for (BudgetRef b : budgets) {
            try {
                financeService.deleteBudget(b.categoryId(), b.month());
            } catch (Exception ignored) {
            }
        }
        for (Long catId : catIds) {
            try {
                financeService.deleteCategory(catId);
            } catch (Exception ignored) {
            }
        }
        for (Long accId : accIds) {
            try {
                financeService.deleteAccount(accId);
            } catch (Exception ignored) {
            }
        }
    }

    private record BudgetRef(Long categoryId, YearMonth month) {
    }
}
