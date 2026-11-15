package piems.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.domain.Account;
import piems.domain.BudgetStatus;
import piems.domain.CategoryComponent;
import piems.domain.InsufficientFundsException;
import piems.domain.Money;
import piems.domain.PersistenceException;
import piems.domain.ValidationException;
import piems.repository.InMemoryAccountRepository;
import piems.repository.InMemoryCategoryRepository;
import piems.repository.InMemoryTransactionRepository;
import piems.repository.TransactionQuery;
import piems.repository.TransactionRecord;
import piems.repository.TransactionRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Black-box test suite for Use Case UC-03 (Record Expense) main and alternative flows.
 *
 * <p>Sources of truth: docs/SRS.md Section 4.1 (UC-03 Main Flow, Alternatives 4a, 5a, 6a, 7a)
 * and Requirement FR-03, FR-10, FR-19.</p>
 */
public class ExpenseUseCaseFlowBlackBoxTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-15T10:00:00Z"), ZoneId.of("UTC"));

    private FailingTransactionRepository transactionRepo;
    private InMemoryAccountRepository accountRepo;
    private InMemoryCategoryRepository categoryRepo;
    private FinanceService service;
    private Account checkingAccount;
    private CategoryComponent groceriesCategory;

    /**
     * Initializes service, accounts, and categories for each test.
     */
    @BeforeEach
    public void setUp() throws Exception {
        transactionRepo = new FailingTransactionRepository();
        accountRepo = new InMemoryAccountRepository(transactionRepo);
        categoryRepo = new InMemoryCategoryRepository(transactionRepo);
        service = new FinanceService(accountRepo, categoryRepo, transactionRepo, FIXED_CLOCK);

        checkingAccount = service.createBankAccount("Checking", Money.of("500.00"));
        groceriesCategory = service.createCategory("Groceries", "EXPENSE", null);
    }

    /**
     * UC-03 Main flow: valid input records expense, decreases balance, updates ledger.
     * TC-FR03-13: Checking 500.00, record 120.00 expense -> balance is 380.00, 1 transaction row.
     */
    @Test
    @DisplayName("TC-FR03-13: UC-03 Main flow successfully records expense and updates ledger")
    public void uc03MainFlowRecordsExpenseUpdatesBalanceAndMaintainsLedger() throws Exception {
        TransactionView view = service.recordExpense(
                checkingAccount.id(),
                groceriesCategory.id(),
                Money.of("120.00"),
                LocalDate.of(2026, 5, 10),
                "Weekly market"
        );

        assertNotNull(view.id());
        assertEquals(Money.of("120.00"), view.amount());
        assertEquals("EXPENSE", view.kind());
        assertEquals(Money.of("380.00"), service.getAccountBalance(checkingAccount.id()));

        List<TransactionView> txs = service.findTransactions(
                new TransactionQuery(null, null, checkingAccount.id(), null, null));
        assertEquals(1, txs.size());
    }

    /**
     * UC-03 Alternative flow 4a: invalid input (zero amount, null category) rejected with ValidationException.
     * TC-FR19-13: Amount 0.00 or null category throws ValidationException; balance unchanged.
     */
    @Test
    @DisplayName("TC-FR19-13: UC-03 Alternative 4a invalid input throws ValidationException")
    public void uc03Alternative4aInvalidInputThrowsValidationException() throws Exception {
        CategoryComponent salaryCategory = service.createCategory("Salary", "INCOME", null);

        assertThrows(ValidationException.class, () -> service.recordExpense(
                checkingAccount.id(), groceriesCategory.id(), Money.of("0.00"),
                LocalDate.of(2026, 5, 10), "Zero"));

        assertThrows(ValidationException.class, () -> service.recordExpense(
                checkingAccount.id(), salaryCategory.id(), Money.of("10.00"),
                LocalDate.of(2026, 5, 10), "Income category for expense"));

        assertEquals(Money.of("500.00"), service.getAccountBalance(checkingAccount.id()));
    }

    /**
     * UC-03 Alternative flow 5a: debit rule fails (overdraft attempt on bank account).
     * TC-FR03-14: Expense 500.01 on balance 500.00 throws InsufficientFundsException; balance unchanged.
     */
    @Test
    @DisplayName("TC-FR03-14: UC-03 Alternative 5a debit rule failure leaves balance and ledger unchanged")
    public void uc03Alternative5aDebitRuleFailureLeavesBalancesAndLedgerUnchanged() throws Exception {
        assertThrows(InsufficientFundsException.class, () -> service.recordExpense(
                checkingAccount.id(), groceriesCategory.id(), Money.of("500.01"),
                LocalDate.of(2026, 5, 10), "Excess"));

        assertEquals(Money.of("500.00"), service.getAccountBalance(checkingAccount.id()));
        List<TransactionView> txs = service.findTransactions(
                new TransactionQuery(null, null, checkingAccount.id(), null, null));
        assertTrue(txs.isEmpty());
    }

    /**
     * UC-03 Alternative flow 6a: repository failure rolls back and reports PersistenceException.
     * TC-FR04-06: Simulated database disk failure throws PersistenceException; balance remains 500.00.
     */
    @Test
    @DisplayName("TC-FR04-06: UC-03 Alternative 6a repository failure rolls back atomically")
    public void uc03Alternative6aRepositoryFailureRollsBackAtomically() throws Exception {
        transactionRepo.setSimulateSaveFailure(true);

        assertThrows(PersistenceException.class, () -> service.recordExpense(
                checkingAccount.id(), groceriesCategory.id(), Money.of("50.00"),
                LocalDate.of(2026, 5, 10), "Will fail"));

        transactionRepo.setSimulateSaveFailure(false);
        assertEquals(Money.of("500.00"), service.getAccountBalance(checkingAccount.id()));
        List<TransactionView> txs = service.findTransactions(
                new TransactionQuery(null, null, checkingAccount.id(), null, null));
        assertTrue(txs.isEmpty());
    }

    /**
     * UC-03 Alternative flow 7a: threshold crossed raises budget alert.
     * TC-FR10-12: Budget 200.00, expense 160.00 (80%) raises WARNING alert to registered listener.
     */
    @Test
    @DisplayName("TC-FR10-12: UC-03 Alternative 7a threshold crossing emits budget warning alert")
    public void uc03Alternative7aThresholdCrossingEmitsBudgetWarningAlert() throws Exception {
        service.setBudget(groceriesCategory.id(), YearMonth.of(2026, 5), Money.of("200.00"));
        List<BudgetAlertEvent> events = new ArrayList<>();
        service.registerBudgetListener(events::add);

        service.recordExpense(
                checkingAccount.id(), groceriesCategory.id(), Money.of("160.00"),
                LocalDate.of(2026, 5, 10), "Groceries at 80%");

        assertEquals(1, events.size());
        assertEquals(BudgetStatus.WARNING, events.get(0).status());
    }

    /**
     * Delegating test repository that can inject persistence failures.
     */
    private static class FailingTransactionRepository implements TransactionRepository {
        private final InMemoryTransactionRepository delegate = new InMemoryTransactionRepository();
        private boolean simulateSaveFailure = false;

        public void setSimulateSaveFailure(boolean simulate) {
            this.simulateSaveFailure = simulate;
        }

        @Override
        public TransactionRecord save(TransactionRecord record) throws PersistenceException {
            if (simulateSaveFailure) {
                throw new PersistenceException("Simulated persistence write failure");
            }
            return delegate.save(record);
        }

        @Override
        public Optional<TransactionRecord> findById(long id) throws PersistenceException {
            return delegate.findById(id);
        }

        @Override
        public List<TransactionRecord> findAll() throws PersistenceException {
            return delegate.findAll();
        }

        @Override
        public List<TransactionRecord> findByAccountId(long accountId) throws PersistenceException {
            return delegate.findByAccountId(accountId);
        }

        @Override
        public List<TransactionRecord> findByCategoryId(long categoryId) throws PersistenceException {
            return delegate.findByCategoryId(categoryId);
        }

        @Override
        public void delete(long id) throws PersistenceException {
            delegate.delete(id);
        }

        @Override
        public int count() throws PersistenceException {
            return delegate.count();
        }

        @Override
        public void executeTransfer(TransactionRecord debitLeg, TransactionRecord creditLeg)
                throws PersistenceException {
            delegate.executeTransfer(debitLeg, creditLeg);
        }

        @Override
        public void executeTransferWithFault(TransactionRecord debit, TransactionRecord credit, boolean fault)
                throws PersistenceException {
            delegate.executeTransferWithFault(debit, credit, fault);
        }

        @Override
        public List<TransactionRecord> findByQuery(TransactionQuery query) throws PersistenceException {
            return delegate.findByQuery(query);
        }

        @Override
        public List<TransactionRecord> findByTransferLinkId(String linkId) throws PersistenceException {
            return delegate.findByTransferLinkId(linkId);
        }

        @Override
        public void deleteByTransferLinkId(String linkId) throws PersistenceException {
            delegate.deleteByTransferLinkId(linkId);
        }
    }
}
