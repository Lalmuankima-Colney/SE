package piems.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import piems.domain.Account;
import piems.domain.BankAccount;
import piems.domain.Budget;
import piems.domain.CashAccount;
import piems.domain.Category;
import piems.domain.CategoryComponent;
import piems.domain.CategoryGroup;
import piems.domain.CreditCardAccount;
import piems.domain.Expense;
import piems.domain.Income;
import piems.domain.InsufficientFundsException;
import piems.domain.Money;
import piems.domain.PersistenceException;
import piems.domain.RecordNotFoundException;
import piems.domain.Transaction;
import piems.domain.Transfer;
import piems.domain.ValidationException;
import piems.report.CashFlowReport;
import piems.report.CategoryBreakdownReport;
import piems.report.CsvExporter;
import piems.report.MonthlySummaryReport;
import piems.report.ReportRequest;
import piems.report.ReportResult;
import piems.report.ReportStrategy;
import piems.report.TextExporter;
import piems.report.TransactionExportRow;
import piems.repository.AccountRepository;
import piems.repository.CategoryRepository;
import piems.repository.TransactionQuery;
import piems.repository.TransactionRecord;
import piems.repository.TransactionRepository;

/**
 * Application facade orchestrating core personal finance use cases (UC-01 through UC-05).
 *
 * <p>Following Decision D-05 and D-14, this service is the single entry point for UI
 * layers, validates domain invariants, enforces transaction boundaries, and uses an
 * injected {@link Clock} for deterministic temporal operations.</p>
 */
public class FinanceService {

    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;
    private final BudgetMonitor budgetMonitor;
    private final Map<String, ReportStrategy> reportStrategies = new ConcurrentHashMap<>();

    /**
     * Constructs a FinanceService with repository dependencies and default BudgetMonitor.
     *
     * @param accountRepository     account persistence repository
     * @param categoryRepository    category persistence repository
     * @param transactionRepository transaction persistence repository
     * @param clock                 clock provider for deterministic dates
     */
    public FinanceService(
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            Clock clock
    ) {
        this(accountRepository, categoryRepository, transactionRepository, clock, new BudgetMonitor(clock));
    }

    /**
     * Constructs a FinanceService with full dependencies including an injected BudgetMonitor.
     *
     * @param accountRepository     account persistence repository
     * @param categoryRepository    category persistence repository
     * @param transactionRepository transaction persistence repository
     * @param clock                 clock provider for deterministic dates
     * @param budgetMonitor         budget monitoring subject for alert notifications
     */
    public FinanceService(
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            Clock clock,
            BudgetMonitor budgetMonitor
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository cannot be null");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository cannot be null");
        this.transactionRepository = Objects.requireNonNull(
                transactionRepository, "transactionRepository cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.budgetMonitor = Objects.requireNonNull(budgetMonitor, "budgetMonitor cannot be null");
        registerReportStrategy(new MonthlySummaryReport());
        registerReportStrategy(new CategoryBreakdownReport());
        registerReportStrategy(new CashFlowReport());
    }

    /**
     * Creates and persists a cash account.
     *
     * @param name           account display name
     * @param openingBalance initial opening balance
     * @return persisted Account entity
     * @throws ValidationException  if name is blank or balance is invalid
     * @throws PersistenceException if persistence fails
     */
    public Account createCashAccount(String name, Money openingBalance)
            throws ValidationException, PersistenceException {
        validateName(name, "Account name");
        Objects.requireNonNull(openingBalance, "Opening balance cannot be null");
        return accountRepository.save(new CashAccount(null, name.trim(), openingBalance));
    }

    /**
     * Creates and persists a bank account.
     *
     * @param name           account display name
     * @param openingBalance initial opening balance
     * @return persisted Account entity
     * @throws ValidationException  if name is blank or balance is invalid
     * @throws PersistenceException if persistence fails
     */
    public Account createBankAccount(String name, Money openingBalance)
            throws ValidationException, PersistenceException {
        validateName(name, "Account name");
        Objects.requireNonNull(openingBalance, "Opening balance cannot be null");
        return accountRepository.save(new BankAccount(null, name.trim(), openingBalance));
    }

    /**
     * Creates and persists a credit card account.
     *
     * @param name        account display name
     * @param creditLimit maximum credit line limit
     * @return persisted Account entity
     * @throws ValidationException  if name is blank or limit is invalid
     * @throws PersistenceException if persistence fails
     */
    public Account createCreditCardAccount(String name, Money creditLimit)
            throws ValidationException, PersistenceException {
        validateName(name, "Account name");
        Objects.requireNonNull(creditLimit, "Credit limit cannot be null");
        return accountRepository.save(new CreditCardAccount(null, name.trim(), creditLimit));
    }

    /**
     * Lists all persisted accounts.
     *
     * @return list of accounts
     * @throws PersistenceException if querying fails
     */
    public List<Account> listAccounts() throws PersistenceException {
        return accountRepository.findAll();
    }

    /**
     * Retrieves an account by its unique identifier.
     *
     * @param id account ID
     * @return account entity
     * @throws RecordNotFoundException if account does not exist
     * @throws PersistenceException    if querying fails
     */
    public Account getAccount(long id) throws RecordNotFoundException, PersistenceException {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RecordNotFoundException("Account not found with id: " + id));
    }

    /**
     * Retrieves an account by display name.
     *
     * @param name account name
     * @return account entity
     * @throws RecordNotFoundException if account does not exist
     * @throws PersistenceException    if querying fails
     */
    public Account getAccountByName(String name) throws RecordNotFoundException, PersistenceException {
        return accountRepository.findByName(name)
                .orElseThrow(() -> new RecordNotFoundException("Account not found with name: " + name));
    }

    /**
     * Archives an account, preventing any subsequent debits.
     *
     * @param id account ID
     * @throws RecordNotFoundException if account does not exist
     * @throws PersistenceException    if updating fails
     */
    public void archiveAccount(long id) throws RecordNotFoundException, PersistenceException {
        Account account = getAccount(id);
        account.archive();
        accountRepository.save(account);
    }

    /**
     * Returns the derived current balance of an account (Decision D-25, FR-07).
     *
     * @param id account ID
     * @return derived current balance
     * @throws RecordNotFoundException if account does not exist
     * @throws PersistenceException    if querying fails
     */
    public Money getAccountBalance(long id) throws RecordNotFoundException, PersistenceException {
        getAccount(id); // verify existence
        return accountRepository.getDerivedBalance(id);
    }

    /**
     * Calculates the total net worth across all accounts (FR-07, TC-FR07-01).
     *
     * @return sum of all account derived balances
     * @throws PersistenceException if querying fails
     */
    public Money getNetWorth() throws PersistenceException {
        List<Account> accounts = accountRepository.findAll();
        Money total = Money.zero();
        for (Account account : accounts) {
            Money balance = accountRepository.getDerivedBalance(account.id());
            total = total.add(balance);
        }
        return total;
    }

    /**
     * Records an income transaction (UC-02, FR-02).
     *
     * @param accountId  account ID to credit
     * @param categoryId optional income category ID
     * @param amount     positive income amount
     * @param date       transaction date, or null to default to today
     * @param note       optional note
     * @return persisted transaction view
     * @throws ValidationException     if inputs are invalid
     * @throws RecordNotFoundException if account or category not found
     * @throws PersistenceException    if persistence fails
     */
    public TransactionView recordIncome(
            long accountId,
            Long categoryId,
            Money amount,
            LocalDate date,
            String note
    ) throws ValidationException, RecordNotFoundException, PersistenceException {
        Account account = getAccount(accountId);
        validatePositiveAmount(amount);
        validateCategory(categoryId, "INCOME");
        LocalDate txDate = (date != null) ? date : LocalDate.now(clock);

        TransactionRecord record = new TransactionRecord(
                null, "INCOME", txDate, amount, note, account.id(), null, categoryId, null
        );
        return toView(transactionRepository.save(record));
    }

    /**
     * Records an expense transaction (UC-03, FR-03, NFR-04).
     *
     * @param accountId  account ID to debit
     * @param categoryId optional expense category ID
     * @param amount     positive expense amount
     * @param date       transaction date, or null to default to today
     * @param note       optional note
     * @return persisted transaction view
     * @throws ValidationException        if inputs are invalid
     * @throws RecordNotFoundException    if account or category not found
     * @throws InsufficientFundsException if account cannot accept debit
     * @throws PersistenceException       if persistence fails
     */
    public TransactionView recordExpense(
            long accountId,
            Long categoryId,
            Money amount,
            LocalDate date,
            String note
    ) throws ValidationException, RecordNotFoundException, InsufficientFundsException, PersistenceException {
        Account account = getAccount(accountId);
        validatePositiveAmount(amount);
        validateCategory(categoryId, "EXPENSE");
        checkDebitPermission(account, amount);
        LocalDate txDate = (date != null) ? date : LocalDate.now(clock);

        TransactionRecord record = new TransactionRecord(
                null, "EXPENSE", txDate, amount, note, account.id(), null, categoryId, null
        );
        TransactionRecord saved = transactionRepository.save(record);
        if (categoryId != null) {
            evaluateBudgetAfterExpense(categoryId, txDate);
        }
        return toView(saved);
    }

    private void checkDebitPermission(Account account, Money amount)
            throws InsufficientFundsException, PersistenceException {
        if (account.isArchived()) {
            throw new InsufficientFundsException("Cannot debit archived account: " + account.name());
        }
        if ("CREDIT_CARD".equals(account.type())) {
            long debtMinor = computeCreditCardDebtMinor(account.id());
            long availableMinor = account.creditLimit().minorUnits() - debtMinor;
            if (amount.minorUnits() > availableMinor) {
                throw new InsufficientFundsException("Expense exceeds available credit limit on: " + account.name());
            }
        } else {
            Money currentBalance = accountRepository.getDerivedBalance(account.id());
            if (amount.minorUnits() > currentBalance.minorUnits()) {
                throw new InsufficientFundsException("Expense exceeds current balance on account: " + account.name());
            }
        }
    }

    private long computeCreditCardDebtMinor(long accountId) throws PersistenceException {
        List<TransactionRecord> txs = transactionRepository.findByAccountId(accountId);
        long debt = 0;
        for (TransactionRecord tx : txs) {
            if ("EXPENSE".equalsIgnoreCase(tx.kind())) {
                debt += tx.amount().minorUnits();
            } else if ("INCOME".equalsIgnoreCase(tx.kind())) {
                debt -= tx.amount().minorUnits();
            }
        }
        return Math.max(0, debt);
    }

    /**
     * Executes an atomic transfer between two accounts (UC-04, FR-04, D-23).
     *
     * @param fromAccountId source account ID
     * @param toAccountId   destination account ID
     * @param amount        transfer amount
     * @param date          transaction date, or null for today
     * @param note          optional note
     * @throws ValidationException        if inputs are invalid
     * @throws RecordNotFoundException    if accounts do not exist
     * @throws InsufficientFundsException if source cannot accept debit
     * @throws PersistenceException       if execution or rollback occurs
     */
    public void recordTransfer(
            long fromAccountId,
            long toAccountId,
            Money amount,
            LocalDate date,
            String note
    ) throws ValidationException, RecordNotFoundException, InsufficientFundsException, PersistenceException {
        if (fromAccountId == toAccountId) {
            throw new ValidationException("Source and destination accounts must be distinct");
        }
        Account fromAccount = getAccount(fromAccountId);
        Account toAccount = getAccount(toAccountId);
        validatePositiveAmount(amount);
        checkDebitPermission(fromAccount, amount);

        LocalDate txDate = (date != null) ? date : LocalDate.now(clock);
        String linkId = UUID.randomUUID().toString();
        TransactionRecord debit = new TransactionRecord(
                null, "TRANSFER", txDate, amount, note, fromAccount.id(), linkId, null, null
        );
        TransactionRecord credit = new TransactionRecord(
                null, "TRANSFER", txDate, amount, note, toAccount.id(), linkId, null, null
        );
        transactionRepository.executeTransfer(debit, credit);
    }

    /**
     * Deletes a transaction by its identifier (FR-06, D-23).
     *
     * <p>If the transaction is part of a two-leg transfer, both legs sharing the
     * transfer link identifier are deleted atomically, restoring balances on both accounts.</p>
     *
     * @param id transaction identifier
     * @throws RecordNotFoundException if transaction record does not exist
     * @throws PersistenceException    if deletion fails
     */
    public void deleteTransaction(long id) throws RecordNotFoundException, PersistenceException {
        TransactionRecord record = transactionRepository.findById(id)
                .orElseThrow(() -> new RecordNotFoundException("Transaction not found: " + id));
        if ("TRANSFER".equalsIgnoreCase(record.kind()) && record.transferLinkId() != null) {
            transactionRepository.deleteByTransferLinkId(record.transferLinkId());
        } else {
            transactionRepository.delete(id);
            if (record.categoryId() != null && "EXPENSE".equalsIgnoreCase(record.kind())) {
                evaluateBudgetAfterExpense(record.categoryId(), record.date());
            }
        }
    }

    /**
     * Edits the amount and note of an existing transaction (FR-06, D-23).
     *
     * <p>For transfers, updates both linked legs atomically to keep balances consistent.</p>
     *
     * @param id        transaction identifier
     * @param newAmount positive new amount
     * @param newNote   new note (optional)
     * @return updated transaction view
     * @throws RecordNotFoundException    if transaction does not exist
     * @throws ValidationException        if newAmount is non-positive or invalid
     * @throws InsufficientFundsException if new amount violates debit limits
     * @throws PersistenceException       if persistence fails
     */
    public TransactionView editTransaction(long id, Money newAmount, String newNote)
            throws RecordNotFoundException, ValidationException, InsufficientFundsException, PersistenceException {
        validatePositiveAmount(newAmount);
        TransactionRecord record = transactionRepository.findById(id)
                .orElseThrow(() -> new RecordNotFoundException("Transaction not found: " + id));
        if ("TRANSFER".equalsIgnoreCase(record.kind()) && record.transferLinkId() != null) {
            return editTransferLegs(record, newAmount, newNote);
        }
        return editSingleTransaction(record, newAmount, newNote);
    }

    private TransactionView editTransferLegs(
            TransactionRecord sourceRecord,
            Money newAmount,
            String newNote
    ) throws RecordNotFoundException, InsufficientFundsException, PersistenceException {
        List<TransactionRecord> legs = transactionRepository.findByTransferLinkId(sourceRecord.transferLinkId());
        if (legs.isEmpty()) {
            throw new RecordNotFoundException("Transfer legs not found: " + sourceRecord.transferLinkId());
        }
        TransactionRecord debitLeg = legs.get(0);
        if (newAmount.minorUnits() > debitLeg.amount().minorUnits()) {
            Account debitAccount = getAccount(debitLeg.accountId());
            Money delta = Money.ofMinor(
                    newAmount.minorUnits() - debitLeg.amount().minorUnits(),
                    newAmount.currency()
            );
            checkDebitPermission(debitAccount, delta);
        }
        TransactionView result = null;
        for (TransactionRecord leg : legs) {
            String note = (newNote != null) ? newNote : leg.note();
            TransactionRecord updated = new TransactionRecord(
                    leg.id(), leg.kind(), leg.date(), newAmount, note,
                    leg.accountId(), leg.transferLinkId(), leg.categoryId(), leg.ruleId()
            );
            TransactionRecord saved = transactionRepository.save(updated);
            if (Objects.equals(leg.id(), sourceRecord.id())) {
                result = toView(saved);
            }
        }
        return (result != null) ? result : toView(legs.get(0));
    }

    private TransactionView editSingleTransaction(
            TransactionRecord record,
            Money newAmount,
            String newNote
    ) throws RecordNotFoundException, InsufficientFundsException, PersistenceException {
        if ("EXPENSE".equalsIgnoreCase(record.kind()) && newAmount.minorUnits() > record.amount().minorUnits()) {
            Account account = getAccount(record.accountId());
            Money delta = Money.ofMinor(
                    newAmount.minorUnits() - record.amount().minorUnits(),
                    newAmount.currency()
            );
            checkDebitPermission(account, delta);
        }
        String note = (newNote != null) ? newNote : record.note();
        TransactionRecord updated = new TransactionRecord(
                record.id(), record.kind(), record.date(), newAmount, note,
                record.accountId(), record.transferLinkId(), record.categoryId(), record.ruleId()
        );
        TransactionRecord saved = transactionRepository.save(updated);
        if (record.categoryId() != null && "EXPENSE".equalsIgnoreCase(record.kind())) {
            evaluateBudgetAfterExpense(record.categoryId(), record.date());
        }
        return toView(saved);
    }

    /**
     * Creates and persists a leaf category (UC-05, FR-05).
     *
     * @param name     category display name
     * @param kind     category kind ("INCOME" or "EXPENSE")
     * @param parentId optional parent category group ID
     * @return persisted CategoryComponent
     * @throws ValidationException     if inputs are invalid
     * @throws RecordNotFoundException if parent group does not exist
     * @throws PersistenceException    if persistence fails
     */
    public CategoryComponent createCategory(String name, String kind, Long parentId)
            throws ValidationException, RecordNotFoundException, PersistenceException {
        validateName(name, "Category name");
        Category leaf = new Category(null, name.trim(), kind);
        return attachAndSaveCategory(leaf, parentId);
    }

    /**
     * Creates and persists a category group (UC-05, FR-05, D-08).
     *
     * @param name     group display name
     * @param kind     group kind ("INCOME" or "EXPENSE")
     * @param parentId optional parent category group ID
     * @return persisted CategoryGroup
     * @throws ValidationException     if inputs are invalid
     * @throws RecordNotFoundException if parent group does not exist
     * @throws PersistenceException    if persistence fails
     */
    public CategoryComponent createCategoryGroup(String name, String kind, Long parentId)
            throws ValidationException, RecordNotFoundException, PersistenceException {
        validateName(name, "Category group name");
        CategoryGroup group = new CategoryGroup(null, name.trim(), kind);
        return attachAndSaveCategory(group, parentId);
    }

    private CategoryComponent attachAndSaveCategory(CategoryComponent component, Long parentId)
            throws RecordNotFoundException, PersistenceException {
        if (parentId != null) {
            CategoryComponent parent = getCategory(parentId);
            if (!parent.isGroup()) {
                throw new PersistenceException("Specified parent category is not a group");
            }
            parent.add(component);
            CategoryComponent savedChild = categoryRepository.save(component);
            categoryRepository.save(parent);
            return savedChild;
        }
        return categoryRepository.save(component);
    }

    /**
     * Lists all root categories and category groups.
     *
     * @return list of category components
     * @throws PersistenceException if querying fails
     */
    public List<CategoryComponent> listCategories() throws PersistenceException {
        return categoryRepository.findAll();
    }

    /**
     * Retrieves a category component by its identifier.
     *
     * @param id category ID
     * @return CategoryComponent entity
     * @throws RecordNotFoundException if category does not exist
     * @throws PersistenceException    if querying fails
     */
    public CategoryComponent getCategory(long id) throws RecordNotFoundException, PersistenceException {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RecordNotFoundException("Category not found with id: " + id));
    }

    /**
     * Archives a category (FR-05, D-26).
     *
     * @param id category ID
     * @throws RecordNotFoundException if category does not exist
     * @throws PersistenceException    if updating fails
     */
    public void archiveCategory(long id) throws RecordNotFoundException, PersistenceException {
        CategoryComponent category = getCategory(id);
        category.archive();
        categoryRepository.save(category);
    }

    /**
     * Deletes an unused category (FR-05, TC-FR05-01).
     *
     * @param id category ID
     * @throws ValidationException     if category is in use
     * @throws RecordNotFoundException if category does not exist
     * @throws PersistenceException    if deletion fails
     */
    public void deleteCategory(long id) throws ValidationException, RecordNotFoundException, PersistenceException {
        CategoryComponent category = getCategory(id);
        if (category.isInUse() || !transactionRepository.findByCategoryId(id).isEmpty()) {
            throw new ValidationException("Category '" + category.name() + "' is in use and cannot be deleted");
        }
        categoryRepository.delete(id);
    }

    /**
     * Lists all transaction records associated with the specified account.
     *
     * @param accountId account ID
     * @return list of transaction records
     * @throws PersistenceException if querying fails
     */
    /**
     * Lists all transaction views associated with the specified account.
     *
     * @param accountId account ID
     * @return list of transaction views
     * @throws PersistenceException if querying fails
     */
    public List<TransactionView> listTransactionsForAccount(long accountId) throws PersistenceException {
        return transactionRepository.findByAccountId(accountId).stream()
                .map(FinanceService::toView)
                .toList();
    }

    /**
     * Lists all transaction views.
     *
     * @return list of all transaction views
     * @throws PersistenceException if querying fails
     */
    public List<TransactionView> listAllTransactions() throws PersistenceException {
        return transactionRepository.findAll().stream()
                .map(FinanceService::toView)
                .toList();
    }

    /**
     * Searches and filters transactions according to the provided criteria (FR-08).
     *
     * @param query search and filter criteria combined with logical AND
     * @return list of matching transaction views
     * @throws PersistenceException if data access fails
     */
    public List<TransactionView> findTransactions(TransactionQuery query) throws PersistenceException {
        Objects.requireNonNull(query, "query cannot be null");
        return transactionRepository.findByQuery(query).stream()
                .map(FinanceService::toView)
                .toList();
    }

    /**
     * Searches and filters transactions according to the provided filter parameters (FR-08).
     *
     * @param text       optional note text search
     * @param accountId  optional account identifier filter
     * @param categoryId optional category identifier filter
     * @param startDate  optional start date
     * @param endDate    optional end date
     * @return list of matching transaction views
     * @throws PersistenceException if data access fails
     */
    public List<TransactionView> findTransactions(
            String text,
            Long accountId,
            Long categoryId,
            LocalDate startDate,
            LocalDate endDate
    ) throws PersistenceException {
        TransactionQuery.Builder builder = TransactionQuery.builder();
        if (text != null && !text.isBlank()) {
            builder.text(text.trim());
        }
        if (accountId != null && accountId > 0) {
            builder.accountId(accountId);
        }
        if (categoryId != null && categoryId > 0) {
            builder.categoryId(categoryId);
        }
        if (startDate != null || endDate != null) {
            builder.dateRange(startDate, endDate);
        }
        return findTransactions(builder.build());
    }

    private static TransactionView toView(TransactionRecord r) {
        return new TransactionView(
                r.id(), r.kind(), r.date(), r.amount(), r.note(), r.accountId(), r.transferLinkId(), r.categoryId()
        );
    }

    /**
     * Registers an observer to receive budget threshold alert notifications (FR-10, D-09).
     *
     * @param listener listener observer to register
     */
    public void registerBudgetListener(BudgetListener listener) {
        budgetMonitor.registerListener(listener);
    }

    /**
     * Unregisters an observer from budget threshold alert notifications.
     *
     * @param listener listener observer to remove
     */
    public void unregisterBudgetListener(BudgetListener listener) {
        budgetMonitor.unregisterListener(listener);
    }

    /**
     * Returns the underlying BudgetMonitor subject.
     *
     * @return BudgetMonitor instance
     */
    public BudgetMonitor getBudgetMonitor() {
        return budgetMonitor;
    }

    /**
     * Sets and registers a monthly budget for a category component (UC-06, FR-09).
     *
     * @param category category component
     * @param month target calendar month
     * @param limit spending limit (must be strictly positive)
     * @return created Budget
     * @throws ValidationException if a budget already exists for this category/month
     */
    public Budget setBudget(CategoryComponent category, YearMonth month, Money limit)
            throws ValidationException {
        Objects.requireNonNull(category, "Category cannot be null");
        Objects.requireNonNull(month, "Month cannot be null");
        validatePositiveAmount(limit);
        Budget budget = new Budget(category, month, limit);
        budgetMonitor.registerBudget(budget);
        return budget;
    }

    /**
     * Sets and registers a monthly budget by category ID (UC-06, FR-09).
     *
     * @param categoryId persistent category ID
     * @param month target calendar month
     * @param limit spending limit (must be strictly positive)
     * @return created Budget
     * @throws ValidationException if category is archived or duplicate budget exists
     * @throws RecordNotFoundException if category does not exist
     * @throws PersistenceException if querying category fails
     */
    public Budget setBudget(long categoryId, YearMonth month, Money limit)
            throws ValidationException, RecordNotFoundException, PersistenceException {
        CategoryComponent category = getCategory(categoryId);
        return setBudget(category, month, limit);
    }

    /**
     * Retrieves the registered budget for a category component and month.
     *
     * @param category category component
     * @param month target calendar month
     * @return Optional containing the budget, or empty
     */
    public Optional<Budget> getBudget(CategoryComponent category, YearMonth month) {
        return budgetMonitor.getBudget(category, month);
    }

    /**
     * Retrieves the registered budget for a category ID and month.
     *
     * @param categoryId persistent category ID
     * @param month target calendar month
     * @return Optional containing the budget, or empty
     * @throws RecordNotFoundException if category does not exist
     * @throws PersistenceException if querying fails
     */
    public Optional<Budget> getBudget(long categoryId, YearMonth month)
            throws RecordNotFoundException, PersistenceException {
        CategoryComponent category = getCategory(categoryId);
        return getBudget(category, month);
    }

    /**
     * Calculates the total expenses recorded for a category in the specified month.
     *
     * @param categoryId category identifier
     * @param month      target calendar month
     * @return total expenses in the month
     * @throws PersistenceException if data access fails
     */
    public Money getCategoryMonthlyExpense(long categoryId, YearMonth month) throws PersistenceException {
        return calculateTotalCategoryExpense(categoryId, month);
    }

    private void evaluateBudgetAfterExpense(Long categoryId, LocalDate date) {
        try {
            CategoryComponent cat = categoryRepository.findById(categoryId).orElse(null);
            if (cat != null) {
                YearMonth month = YearMonth.from(date);
                Optional<Budget> budgetOpt = budgetMonitor.getBudget(cat, month);
                if (budgetOpt.isPresent()) {
                    Money totalSpent = calculateTotalCategoryExpense(categoryId, month);
                    budgetMonitor.check(budgetOpt.get(), totalSpent, date);
                }
            }
        } catch (Exception ignored) {
            // Decision D-09: Budget alerts through Observer and never exceptions;
            // a threshold alert or evaluation issue must never abort recording an expense.
        }
    }

    private Money calculateTotalCategoryExpense(long categoryId, YearMonth month)
            throws PersistenceException {
        List<TransactionRecord> txs = transactionRepository.findByCategoryId(categoryId);
        long totalMinor = 0;
        for (TransactionRecord tx : txs) {
            if ("EXPENSE".equalsIgnoreCase(tx.kind()) && YearMonth.from(tx.date()).equals(month)) {
                totalMinor += tx.amount().minorUnits();
            }
        }
        return Money.ofMinor(totalMinor);
    }

    private void validateName(String name, String fieldLabel) throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException(fieldLabel + " cannot be null or empty");
        }
    }

    private void validatePositiveAmount(Money amount) throws ValidationException {
        if (amount == null || amount.isZero()) {
            throw new ValidationException("Transaction amount must be strictly greater than zero");
        }
    }

    private void validateCategory(Long categoryId, String expectedKind)
            throws ValidationException, RecordNotFoundException, PersistenceException {
        if (categoryId == null) {
            return;
        }
        CategoryComponent cat = getCategory(categoryId);
        if (cat.isArchived()) {
            throw new ValidationException("Cannot assign archived category '" + cat.name() + "' to new transaction");
        }
        if (!expectedKind.equalsIgnoreCase(cat.kind())) {
            throw new ValidationException(
                    "Category kind mismatch: expected " + expectedKind + " but was " + cat.kind());
        }
    }

    /**
     * Registers a report strategy with this service facade.
     *
     * @param strategy report strategy to register
     */
    public void registerReportStrategy(ReportStrategy strategy) {
        Objects.requireNonNull(strategy, "Report strategy cannot be null");
        reportStrategies.put(strategy.name(), strategy);
    }

    /**
     * Generates a financial report using the specified report strategy.
     *
     * @param strategy report strategy to execute
     * @param request  report parameter specification
     * @return generated report result
     * @throws PersistenceException if retrieving transaction records fails
     */
    public ReportResult generateReport(ReportStrategy strategy, ReportRequest request)
            throws PersistenceException {
        Objects.requireNonNull(strategy, "Report strategy cannot be null");
        Objects.requireNonNull(request, "Report request cannot be null");
        List<Transaction> transactions = loadAllDomainTransactions();
        return strategy.generate(request, transactions);
    }

    /**
     * Generates a financial report by registered strategy name.
     *
     * @param strategyName registered strategy identifier
     * @param request      report parameter specification
     * @return generated report result
     * @throws ValidationException  if strategy name is unknown or invalid
     * @throws PersistenceException if retrieving transaction records fails
     */
    public ReportResult generateReport(String strategyName, ReportRequest request)
            throws ValidationException, PersistenceException {
        if (strategyName == null || strategyName.trim().isEmpty()) {
            throw new ValidationException("Strategy name cannot be null or empty");
        }
        ReportStrategy strategy = reportStrategies.get(strategyName.trim());
        if (strategy == null) {
            throw new ValidationException("Unknown report strategy: " + strategyName);
        }
        return generateReport(strategy, request);
    }

    private List<Transaction> loadAllDomainTransactions() throws PersistenceException {
        List<TransactionRecord> records = transactionRepository.findAll();
        List<Transaction> result = new ArrayList<>();
        for (TransactionRecord rec : records) {
            Transaction tx = toDomainTransaction(rec);
            if (tx != null) {
                result.add(tx);
            }
        }
        return result;
    }

    private Transaction toDomainTransaction(TransactionRecord rec) throws PersistenceException {
        Account account = accountRepository.findById(rec.accountId()).orElse(null);
        CategoryComponent cat = null;
        if (rec.categoryId() != null) {
            cat = categoryRepository.findById(rec.categoryId()).orElse(null);
        }
        if ("INCOME".equals(rec.kind())) {
            return new Income(rec.id(), rec.date(), rec.amount(), rec.note(), account, cat);
        }
        if ("EXPENSE".equals(rec.kind())) {
            return new Expense(rec.id(), rec.date(), rec.amount(), rec.note(), account, cat);
        }
        if ("TRANSFER".equals(rec.kind())) {
            return new Transfer(rec.id(), rec.date(), rec.amount(), rec.note(), account, rec.transferLinkId());
        }
        return null;
    }

    /**
     * Ingests transactions from a CSV document string (FR-17, D-30).
     *
     * @param csvContent raw CSV text
     * @return ImportResult containing counts of imported, duplicate, and skipped records
     * @throws ImportException if the CSV is empty or has an invalid header
     */
    public ImportResult importCsv(String csvContent) throws ImportException {
        CsvImporter importer = new CsvImporter(
                accountRepository, categoryRepository, transactionRepository, budgetMonitor
        );
        return importer.importCsv(csvContent);
    }

    /**
     * Exports all ledger transactions in standard 7-column RFC 4180 CSV format (FR-16, D-30).
     *
     * @return serialized CSV string
     * @throws PersistenceException if repository access fails
     */
    public String exportTransactionsCsv() throws PersistenceException {
        List<TransactionExportRow> rows = prepareTransactionExportRows();
        return new CsvExporter().exportTransactions(rows);
    }

    /**
     * Exports all ledger transactions in aligned plain text tabular format (D-10).
     *
     * @return aligned text table string
     * @throws PersistenceException if repository access fails
     */
    public String exportTransactionsText() throws PersistenceException {
        List<TransactionExportRow> rows = prepareTransactionExportRows();
        return new TextExporter().exportTransactions(rows);
    }

    /**
     * Exports a ReportResult in RFC 4180 CSV format (FR-16, D-10).
     *
     * @param report structured report result
     * @return CSV formatted report string
     */
    public String exportReportCsv(ReportResult report) {
        return new CsvExporter().export(report);
    }

    /**
     * Generates and exports a report in RFC 4180 CSV format (FR-16, D-10).
     *
     * @param strategy report generation strategy
     * @param request  report parameter request
     * @return CSV formatted report string
     * @throws PersistenceException if data retrieval fails
     */
    public String exportReportCsv(ReportStrategy strategy, ReportRequest request) throws PersistenceException {
        return exportReportCsv(generateReport(strategy, request));
    }

    /**
     * Exports a ReportResult in aligned plain text tabular format (D-10).
     *
     * @param report structured report result
     * @return aligned text table string
     */
    public String exportReportText(ReportResult report) {
        return new TextExporter().export(report);
    }

    /**
     * Generates and exports a report in aligned plain text tabular format (D-10).
     *
     * @param strategy report generation strategy
     * @param request  report parameter request
     * @return aligned text table string
     * @throws PersistenceException if data retrieval fails
     */
    public String exportReportText(ReportStrategy strategy, ReportRequest request) throws PersistenceException {
        return exportReportText(generateReport(strategy, request));
    }

    private List<TransactionExportRow> prepareTransactionExportRows() throws PersistenceException {
        Map<Long, String> accNames = loadAccountNameMap();
        Map<Long, String> catNames = loadCategoryNameMap();
        List<TransactionRecord> records = transactionRepository.findAll();
        Set<String> processedTransferLinks = new HashSet<>();
        List<TransactionExportRow> rows = new ArrayList<>();

        for (TransactionRecord r : records) {
            if ("TRANSFER".equalsIgnoreCase(r.kind()) && r.transferLinkId() != null) {
                if (!processedTransferLinks.contains(r.transferLinkId())) {
                    processedTransferLinks.add(r.transferLinkId());
                    rows.add(buildTransferExportRow(r.transferLinkId(), records, accNames));
                }
            } else {
                rows.add(buildRegularExportRow(r, accNames, catNames));
            }
        }
        return rows;
    }

    private TransactionExportRow buildTransferExportRow(
            String linkId, List<TransactionRecord> all, Map<Long, String> accNames
    ) {
        List<TransactionRecord> legs = all.stream()
                .filter(tx -> linkId.equals(tx.transferLinkId()))
                .sorted(Comparator.comparing(TransactionRecord::id))
                .toList();
        if (legs.size() >= 2) {
            TransactionRecord debit = legs.get(0);
            TransactionRecord credit = legs.get(1);
            return new TransactionExportRow(
                    debit.date(), "TRANSFER",
                    accNames.getOrDefault(debit.accountId(), "Unknown"),
                    accNames.getOrDefault(credit.accountId(), "Unknown"),
                    "", debit.amount(), debit.note()
            );
        }
        TransactionRecord single = legs.get(0);
        return new TransactionExportRow(
                single.date(), "TRANSFER",
                accNames.getOrDefault(single.accountId(), "Unknown"),
                "", "", single.amount(), single.note()
        );
    }

    private TransactionExportRow buildRegularExportRow(
            TransactionRecord r, Map<Long, String> accNames, Map<Long, String> catNames
    ) {
        String acc = accNames.getOrDefault(r.accountId(), "Unknown");
        String cat = (r.categoryId() != null) ? catNames.getOrDefault(r.categoryId(), "") : "";
        return new TransactionExportRow(
                r.date(), r.kind().toUpperCase(), acc, "", cat, r.amount(), r.note()
        );
    }

    private Map<Long, String> loadAccountNameMap() throws PersistenceException {
        Map<Long, String> map = new HashMap<>();
        for (Account a : accountRepository.findAll()) {
            map.put(a.id(), a.name());
        }
        return map;
    }

    private Map<Long, String> loadCategoryNameMap() throws PersistenceException {
        Map<Long, String> map = new HashMap<>();
        for (CategoryComponent c : categoryRepository.findAll()) {
            map.put(c.id(), c.name());
        }
        return map;
    }
}
