package piems.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.domain.Account;
import piems.domain.CategoryComponent;
import piems.domain.InsufficientFundsException;
import piems.domain.Money;
import piems.domain.RecordNotFoundException;
import piems.domain.ValidationException;
import piems.repository.AccountRepository;
import piems.repository.CategoryRepository;
import piems.repository.InMemoryAccountRepository;
import piems.repository.InMemoryCategoryRepository;
import piems.repository.InMemoryTransactionRepository;
import piems.repository.TransactionQuery;
import piems.repository.TransactionRepository;

/**
 * Unit and integration tests for {@link FinanceService} (Decision D-05, D-14).
 *
 * <p>Traces to requirements UC-01 to UC-05, FR-01 through FR-07, FR-19, and NFR-08.</p>
 */
class FinanceServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneId.of("UTC"));

    private AccountRepository accountRepo;
    private CategoryRepository categoryRepo;
    private TransactionRepository transactionRepo;
    private FinanceService service;

    @BeforeEach
    void setUp() {
        transactionRepo = new InMemoryTransactionRepository();
        accountRepo = new InMemoryAccountRepository(transactionRepo);
        categoryRepo = new InMemoryCategoryRepository(transactionRepo);
        service = new FinanceService(accountRepo, categoryRepo, transactionRepo, FIXED_CLOCK);
    }

    @Test
    @DisplayName("TC-FR07-01 / TC-NFR08-01: Net worth equals the sum of account derived balances")
    void netWorthEqualsSumOfBalances() throws Exception {
        Account wallet = service.createCashAccount("Wallet", Money.of("100.00"));
        Account bank = service.createBankAccount("Checking", Money.of("500.00"));
        Account savings = service.createBankAccount("Savings", Money.of("1000.00"));

        assertThat(service.getNetWorth()).isEqualTo(Money.of("1600.00"));

        service.recordIncome(bank.id(), null, Money.of("200.00"), null, "Salary");
        service.recordExpense(wallet.id(), null, Money.of("40.00"), null, "Groceries");
        service.recordExpense(savings.id(), null, Money.of("150.00"), null, "Appliance");

        assertThat(service.getAccountBalance(wallet.id())).isEqualTo(Money.of("60.00"));
        assertThat(service.getAccountBalance(bank.id())).isEqualTo(Money.of("700.00"));
        assertThat(service.getAccountBalance(savings.id())).isEqualTo(Money.of("850.00"));

        // Net worth: 60.00 + 700.00 + 850.00 = 1610.00
        assertThat(service.getNetWorth()).isEqualTo(Money.of("1610.00"));
    }

    @Test
    @DisplayName("TC-FR07-02: Derived balance matches opening balance plus transaction effects")
    void derivedBalanceMatchesOpeningPlusEffects() throws Exception {
        Account cash = service.createCashAccount("Pocket", Money.of("300.00"));
        Account bank = service.createBankAccount("Savings", Money.of("100.00"));

        service.recordIncome(cash.id(), null, Money.of("100.00"), null, "Gift");
        service.recordExpense(cash.id(), null, Money.of("40.00"), null, "Snacks");
        service.recordTransfer(cash.id(), bank.id(), Money.of("60.00"), null, "Deposit");

        // Opening (300) + Income (100) - Expense (40) - Transfer Out (60) = 300.00
        assertThat(service.getAccountBalance(cash.id())).isEqualTo(Money.of("300.00"));
        // Opening (100) + Transfer In (60) = 160.00
        assertThat(service.getAccountBalance(bank.id())).isEqualTo(Money.of("160.00"));
    }

    @Test
    @DisplayName("TC-FR02-01: Recording income raises account balance")
    void recordsIncomeAndUpdatesBalance() throws Exception {
        Account bank = service.createBankAccount("Checking", Money.zero());
        TransactionView tx = service.recordIncome(bank.id(), null, Money.of("250.00"), null, "Freelance");

        assertThat(tx.kind()).isEqualTo("INCOME");
        assertThat(tx.amount()).isEqualTo(Money.of("250.00"));
        assertThat(tx.date()).isEqualTo(LocalDate.now(FIXED_CLOCK));
        assertThat(service.getAccountBalance(bank.id())).isEqualTo(Money.of("250.00"));
    }

    @Test
    @DisplayName("TC-FR02-02: Recording income with non-positive amount is rejected")
    void incomeRejectsNonPositiveAmount() throws Exception {
        Account bank = service.createBankAccount("Checking", Money.zero());

        assertThatThrownBy(() -> service.recordIncome(bank.id(), null, Money.zero(), null, "Zero"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Transaction amount must be strictly greater than zero");
    }

    @Test
    @DisplayName("TC-FR03-01: Recording expense decrements account balance")
    void recordsExpenseAndChecksDebitRule() throws Exception {
        Account wallet = service.createCashAccount("Wallet", Money.of("100.00"));
        TransactionView tx = service.recordExpense(wallet.id(), null, Money.of("45.00"), null, "Lunch");

        assertThat(tx.kind()).isEqualTo("EXPENSE");
        assertThat(tx.amount()).isEqualTo(Money.of("45.00"));
        assertThat(service.getAccountBalance(wallet.id())).isEqualTo(Money.of("55.00"));
    }

    @Test
    @DisplayName("TC-FR03-02: Expense exceeding current balance on cash account is rejected")
    void expenseRejectsInsufficientFundsOnCashAccount() throws Exception {
        Account wallet = service.createCashAccount("Wallet", Money.of("50.00"));

        assertThatThrownBy(() -> service.recordExpense(wallet.id(), null, Money.of("50.01"), null, "Over"))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Expense exceeds current balance");
    }

    @Test
    @DisplayName("TC-FR03-04: Credit card accepts expense within limit, rejects beyond limit")
    void expenseRejectsExceedingCreditLimit() throws Exception {
        Account card = service.createCreditCardAccount("Visa", Money.of("200.00"));

        service.recordExpense(card.id(), null, Money.of("150.00"), null, "Gas");
        assertThat(service.getAccountBalance(card.id())).isEqualTo(Money.zero());

        assertThatThrownBy(() -> service.recordExpense(card.id(), null, Money.of("60.00"), null, "Too much"))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Expense exceeds available credit limit");
    }

    @Test
    @DisplayName("TC-FR01-02: Archived account rejects any debit operations")
    void archivedAccountRejectsDebit() throws Exception {
        Account cash = service.createCashAccount("OldWallet", Money.of("100.00"));
        service.archiveAccount(cash.id());

        assertThatThrownBy(() -> service.recordExpense(cash.id(), null, Money.of("10.00"), null, "Test"))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Cannot debit archived account");
    }

    @Test
    @DisplayName("TC-FR04-01: Transfer moves money between accounts and preserves total net worth")
    void transfersPreservesNetWorth() throws Exception {
        Account wallet = service.createCashAccount("Wallet", Money.of("100.00"));
        Account bank = service.createBankAccount("Checking", Money.of("500.00"));

        service.recordTransfer(wallet.id(), bank.id(), Money.of("70.00"), null, "ATM Deposit");

        assertThat(service.getAccountBalance(wallet.id())).isEqualTo(Money.of("30.00"));
        assertThat(service.getAccountBalance(bank.id())).isEqualTo(Money.of("570.00"));
        assertThat(service.getNetWorth()).isEqualTo(Money.of("600.00"));
    }

    @Test
    @DisplayName("TC-FR04-01: Transfer between identical accounts is rejected")
    void transferRejectsIdenticalAccounts() throws Exception {
        Account wallet = service.createCashAccount("Wallet", Money.of("100.00"));

        assertThatThrownBy(() -> service.recordTransfer(wallet.id(), wallet.id(), Money.of("10.00"), null, "Self"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Source and destination accounts must be distinct");
    }

    @Test
    @DisplayName("TC-FR05-02: Recording expense with an archived category is rejected")
    void recordsExpenseWithArchivedCategoryRejected() throws Exception {
        Account wallet = service.createCashAccount("Wallet", Money.of("100.00"));
        CategoryComponent food = service.createCategory("Food", "EXPENSE", null);
        service.archiveCategory(food.id());

        assertThatThrownBy(() -> service.recordExpense(wallet.id(), food.id(), Money.of("20.00"), null, "Dinner"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot assign archived category");
    }

    @Test
    @DisplayName("TC-FR05-01: Deleting category in use is rejected, unused can be deleted")
    void deletesUnusedCategoryAndRejectsCategoryInUse() throws Exception {
        Account wallet = service.createCashAccount("Wallet", Money.of("100.00"));
        CategoryComponent cat = service.createCategory("Temporary", "EXPENSE", null);
        service.recordExpense(wallet.id(), cat.id(), Money.of("15.00"), null, "Test");

        assertThatThrownBy(() -> service.deleteCategory(cat.id()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("is in use and cannot be deleted");

        CategoryComponent unused = service.createCategory("Spare", "EXPENSE", null);
        service.deleteCategory(unused.id());
        assertThatThrownBy(() -> service.getCategory(unused.id()))
                .isInstanceOf(RecordNotFoundException.class);
    }

    @Test
    @DisplayName("TC-FR19-02: Blank account name or invalid inputs throw ValidationException")
    void validatesInputsAndThrowsValidationException() {
        assertThatThrownBy(() -> service.createCashAccount("   ", Money.zero()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Account name cannot be null or empty");

        assertThatThrownBy(() -> service.createCategory("", "EXPENSE", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Category name cannot be null or empty");
    }

    @Test
    @DisplayName("UC-01 & UC-05: Category hierarchy creation and listing")
    void categoryHierarchyAndListing() throws Exception {
        CategoryComponent group = service.createCategoryGroup("Living", "EXPENSE", null);
        CategoryComponent child = service.createCategory("Rent", "EXPENSE", group.id());

        List<CategoryComponent> categories = service.listCategories();
        assertThat(categories).isNotEmpty();
        assertThat(service.getCategory(child.id()).path()).isEqualTo("Living > Rent");
    }

    @Test
    @DisplayName("FR-08: findTransactions filters by TransactionQuery and returns TransactionView records")
    void findTransactionsFiltersAndReturnsViews() throws Exception {
        Account bank = service.createBankAccount("Checking", Money.of("5000.00"));
        CategoryComponent food = service.createCategory("Food", "EXPENSE", null);

        service.recordExpense(bank.id(), food.id(), Money.of("100.00"), LocalDate.of(2026, 1, 10), "Lunch");
        service.recordExpense(bank.id(), food.id(), Money.of("200.00"), LocalDate.of(2026, 1, 25), "Dinner");

        TransactionQuery query = TransactionQuery.builder()
                .categoryId(food.id())
                .dateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15))
                .build();

        List<TransactionView> results = service.findTransactions(query);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).note()).isEqualTo("Lunch");
        assertThat(results.get(0).amount()).isEqualTo(Money.of("100.00"));
    }

    @Test
    @DisplayName("TC-FR06-02: Deleting one transfer leg atomically deletes both legs and restores balances")
    void deleteTransferDeletesBothLegsAndRestoresBalances() throws Exception {
        Account source = service.createBankAccount("Savings", Money.of("1000.00"));
        Account dest = service.createCashAccount("Wallet", Money.of("200.00"));

        service.recordTransfer(source.id(), dest.id(), Money.of("300.00"), null, "Fund wallet");
        assertThat(service.getAccountBalance(source.id())).isEqualTo(Money.of("700.00"));
        assertThat(service.getAccountBalance(dest.id())).isEqualTo(Money.of("500.00"));

        List<TransactionView> txs = service.findTransactions(TransactionQuery.all());
        assertThat(txs).hasSize(2);

        service.deleteTransaction(txs.get(0).id());

        assertThat(service.getAccountBalance(source.id())).isEqualTo(Money.of("1000.00"));
        assertThat(service.getAccountBalance(dest.id())).isEqualTo(Money.of("200.00"));
        assertThat(service.findTransactions(TransactionQuery.all())).isEmpty();
    }

    @Test
    @DisplayName("TC-FR06-01: Editing transaction amount keeps derived balances consistent")
    void editTransactionUpdatesAmountAndDerivedBalances() throws Exception {
        Account bank = service.createBankAccount("Checking", Money.of("100.00"));
        CategoryComponent food = service.createCategory("Food", "EXPENSE", null);

        TransactionView exp = service.recordExpense(bank.id(), food.id(), Money.of("40.00"), null, "Groceries");
        assertThat(service.getAccountBalance(bank.id())).isEqualTo(Money.of("60.00"));

        TransactionView updated = service.editTransaction(exp.id(), Money.of("30.00"), "Groceries revised");
        assertThat(updated.amount()).isEqualTo(Money.of("30.00"));
        assertThat(service.getAccountBalance(bank.id())).isEqualTo(Money.of("70.00"));
    }

    @Test
    @DisplayName("TC-FR04-04: Editing transfer updates amount and note across both legs atomically")
    void editingTransferUpdatesBothLegsAtomically() throws Exception {
        Account source = service.createBankAccount("Source", Money.of("1000.00"));
        Account dest = service.createCashAccount("Dest", Money.of("200.00"));

        service.recordTransfer(source.id(), dest.id(), Money.of("200.00"), null, "Original transfer");
        assertThat(service.getAccountBalance(source.id())).isEqualTo(Money.of("800.00"));
        assertThat(service.getAccountBalance(dest.id())).isEqualTo(Money.of("400.00"));

        List<TransactionView> txs = service.findTransactions(TransactionQuery.all());
        assertThat(txs).hasSize(2);

        TransactionView updated = service.editTransaction(txs.get(0).id(), Money.of("350.00"), "Revised transfer");
        assertThat(updated.amount()).isEqualTo(Money.of("350.00"));
        assertThat(service.getAccountBalance(source.id())).isEqualTo(Money.of("650.00"));
        assertThat(service.getAccountBalance(dest.id())).isEqualTo(Money.of("550.00"));

        List<TransactionView> afterTxs = service.findTransactions(TransactionQuery.all());
        assertThat(afterTxs).allMatch(t -> t.amount().equals(Money.of("350.00")));
        assertThat(afterTxs).allMatch(t -> "Revised transfer".equals(t.note()));
    }

    @Test
    @DisplayName("TC-FR04-05: Editing transfer exceeding available balance throws InsufficientFundsException")
    void editingTransferBeyondAvailableBalanceThrowsInsufficientFundsException() throws Exception {
        Account source = service.createCashAccount("Source", Money.of("100.00"));
        Account dest = service.createCashAccount("Dest", Money.of("50.00"));

        service.recordTransfer(source.id(), dest.id(), Money.of("60.00"), null, "Transfer 60");
        assertThat(service.getAccountBalance(source.id())).isEqualTo(Money.of("40.00"));

        List<TransactionView> txs = service.findTransactions(TransactionQuery.all());
        assertThatThrownBy(() -> service.editTransaction(txs.get(0).id(), Money.of("120.00"), "Transfer 120"))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(service.getAccountBalance(source.id())).isEqualTo(Money.of("40.00"));
        assertThat(service.getAccountBalance(dest.id())).isEqualTo(Money.of("110.00"));
    }

    @Test
    @DisplayName("D-48: TransactionView resolves account and category names across income, expense, and uncategorized")
    void transactionViewResolvesAccountAndCategoryNamesAcrossOperations() throws Exception {
        Account bank = service.createBankAccount("Main Bank", Money.of("1000.00"));
        Account cash = service.createCashAccount("Pocket Cash", Money.of("50.00"));
        CategoryComponent salary = service.createCategory("Salary", "INCOME", null);
        CategoryComponent food = service.createCategory("Food", "EXPENSE", null);

        service.recordIncome(bank.id(), salary.id(), Money.of("500.00"), null, "Paycheck");
        service.recordExpense(cash.id(), food.id(), Money.of("20.00"), null, "Snacks");
        service.recordExpense(cash.id(), null, Money.of("5.00"), null, "No category");

        List<TransactionView> txs = service.listAllTransactions();
        TransactionView inc = txs.stream().filter(t -> "Paycheck".equals(t.note())).findFirst().orElseThrow();
        assertThat(inc.accountName()).isEqualTo("Main Bank");
        assertThat(inc.categoryName()).isEqualTo("Salary");

        TransactionView exp = txs.stream().filter(t -> "Snacks".equals(t.note())).findFirst().orElseThrow();
        assertThat(exp.accountName()).isEqualTo("Pocket Cash");
        assertThat(exp.categoryName()).isEqualTo("Food");

        TransactionView unc = txs.stream().filter(t -> "No category".equals(t.note())).findFirst().orElseThrow();
        assertThat(unc.accountName()).isEqualTo("Pocket Cash");
        assertThat(unc.categoryName()).isNull();
    }

    @Test
    @DisplayName("D-48: TransactionView resolves names for transfer legs and archived accounts or categories")
    void transactionViewResolvesNamesForTransfersAndArchivedEntities() throws Exception {
        Account source = service.createCashAccount("Src", Money.of("200.00"));
        Account target = service.createCashAccount("Dst", Money.of("50.00"));
        service.recordTransfer(source.id(), target.id(), Money.of("50.00"), null, "Transfer");

        List<TransactionView> txs = service.listAllTransactions();
        TransactionView srcLeg = txs.stream().filter(t -> "Src".equals(t.accountName())).findFirst().orElseThrow();
        TransactionView dstLeg = txs.stream().filter(t -> "Dst".equals(t.accountName())).findFirst().orElseThrow();
        assertThat(srcLeg.categoryName()).isNull();
        assertThat(dstLeg.categoryName()).isNull();

        service.archiveAccount(source.id());
        List<TransactionView> after = service.listAllTransactions();
        TransactionView archived = after.stream().filter(t -> t.id().equals(srcLeg.id())).findFirst().orElseThrow();
        assertThat(archived.accountName()).isEqualTo("Src");
    }
}
