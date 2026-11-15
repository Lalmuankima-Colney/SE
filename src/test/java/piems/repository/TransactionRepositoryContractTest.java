package piems.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.domain.Account;
import piems.domain.BankAccount;
import piems.domain.CashAccount;
import piems.domain.Category;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.domain.PersistenceException;

/**
 * Abstract contract test verifying that all {@link TransactionRepository} implementations
 * adhere strictly to the repository interface contract (Decisions D-11, D-23, D-33).
 */
public abstract class TransactionRepositoryContractTest {

    private TransactionRepository transactionRepository;
    private AccountRepository accountRepository;
    private CategoryRepository categoryRepository;

    /**
     * Factory method creating a clean, isolated transaction repository instance.
     *
     * @return fresh TransactionRepository instance
     * @throws Exception if setup fails
     */
    protected abstract TransactionRepository createTransactionRepository() throws Exception;

    /**
     * Factory method creating a collaborator account repository instance.
     *
     * @return fresh AccountRepository instance
     * @throws Exception if setup fails
     */
    protected abstract AccountRepository createAccountRepository() throws Exception;

    /**
     * Factory method creating a collaborator category repository instance.
     *
     * @return fresh CategoryRepository instance
     * @throws Exception if setup fails
     */
    protected abstract CategoryRepository createCategoryRepository() throws Exception;

    @BeforeEach
    void setUp() throws Exception {
        accountRepository = createAccountRepository();
        categoryRepository = createCategoryRepository();
        transactionRepository = createTransactionRepository();
    }

    @Test
    @DisplayName("Saves and retrieves a single transaction record with exact Money")
    void savesAndFindsTransactionRecord() throws Exception {
        Account account = accountRepository.save(new BankAccount(null, "Checking", Money.of("1000.00")));
        CategoryComponent category = categoryRepository.save(new Category(null, "Consulting", "INCOME"));

        TransactionRecord record = new TransactionRecord(
                null, "INCOME", LocalDate.parse("2026-02-01"), Money.of("750.50"),
                "Project invoice", account.id(), null, category.id(), null
        );

        TransactionRecord saved = transactionRepository.save(record);
        assertThat(saved.id()).isNotNull().isPositive();
        assertThat(saved.kind()).isEqualTo("INCOME");
        assertThat(saved.amount()).isEqualTo(Money.of("750.50"));
        assertThat(saved.accountId()).isEqualTo(account.id());
        assertThat(saved.categoryId()).isEqualTo(category.id());

        Optional<TransactionRecord> fetched = transactionRepository.findById(saved.id());
        assertThat(fetched).isPresent();
        assertThat(fetched.get().note()).isEqualTo("Project invoice");
    }

    @Test
    @DisplayName("Filters transactions by account ID and lists all")
    void findAllAndFindByAccountId() throws Exception {
        Account acc1 = accountRepository.save(new CashAccount(null, "Wallet", Money.of("100.00")));
        Account acc2 = accountRepository.save(new BankAccount(null, "Bank", Money.of("500.00")));

        transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.parse("2026-02-02"), Money.of("10.00"),
                "Coffee", acc1.id(), null, null, null
        ));
        transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.parse("2026-02-03"), Money.of("20.00"),
                "Lunch", acc1.id(), null, null, null
        ));
        transactionRepository.save(new TransactionRecord(
                null, "INCOME", LocalDate.parse("2026-02-04"), Money.of("200.00"),
                "Interest", acc2.id(), null, null, null
        ));

        List<TransactionRecord> acc1Txs = transactionRepository.findByAccountId(acc1.id());
        List<TransactionRecord> acc2Txs = transactionRepository.findByAccountId(acc2.id());
        List<TransactionRecord> all = transactionRepository.findAll();

        assertThat(acc1Txs).hasSize(2);
        assertThat(acc2Txs).hasSize(1);
        assertThat(all).hasSize(3);
    }

    @Test
    @DisplayName("Filters transactions by category ID")
    void findByCategoryId() throws Exception {
        Account acc = accountRepository.save(new CashAccount(null, "Cash", Money.of("200.00")));
        CategoryComponent catFood = categoryRepository.save(new Category(null, "Food", "EXPENSE"));
        CategoryComponent catTravel = categoryRepository.save(new Category(null, "Travel", "EXPENSE"));

        transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.parse("2026-02-05"), Money.of("15.00"),
                "Meal", acc.id(), null, catFood.id(), null
        ));
        transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.parse("2026-02-06"), Money.of("50.00"),
                "Taxi", acc.id(), null, catTravel.id(), null
        ));

        List<TransactionRecord> foodTxs = transactionRepository.findByCategoryId(catFood.id());
        assertThat(foodTxs).hasSize(1);
        assertThat(foodTxs.get(0).note()).isEqualTo("Meal");
    }

    @Test
    @DisplayName("Deletes a transaction record")
    void deletesTransaction() throws Exception {
        Account acc = accountRepository.save(new CashAccount(null, "Cash", Money.of("100.00")));
        TransactionRecord saved = transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.parse("2026-02-07"), Money.of("5.00"),
                "Newspaper", acc.id(), null, null, null
        ));

        assertThat(transactionRepository.findById(saved.id())).isPresent();
        transactionRepository.delete(saved.id());
        assertThat(transactionRepository.findById(saved.id())).isEmpty();
    }

    @Test
    @DisplayName("Executes atomic transfer writing both linked legs")
    void atomicTransferWritesBothLegs() throws Exception {
        Account source = accountRepository.save(new BankAccount(null, "Source", Money.of("1000.00")));
        Account dest = accountRepository.save(new CashAccount(null, "Dest", Money.of("0.00")));
        String linkId = UUID.randomUUID().toString();

        TransactionRecord debit = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-02-08"), Money.of("100.00"),
                "Transfer out", source.id(), linkId, null, null
        );
        TransactionRecord credit = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-02-08"), Money.of("100.00"),
                "Transfer in", dest.id(), linkId, null, null
        );

        transactionRepository.executeTransfer(debit, credit);

        List<TransactionRecord> sourceTxs = transactionRepository.findByAccountId(source.id());
        List<TransactionRecord> destTxs = transactionRepository.findByAccountId(dest.id());

        assertThat(sourceTxs).hasSize(1);
        assertThat(destTxs).hasSize(1);
        assertThat(sourceTxs.get(0).transferLinkId()).isEqualTo(linkId);
        assertThat(destTxs.get(0).transferLinkId()).isEqualTo(linkId);
    }

    @Test
    @DisplayName("Rolls back completely when transfer fault is injected on second leg")
    void atomicTransferRollsBackOnFault() throws Exception {
        Account source = accountRepository.save(new BankAccount(null, "Source Acc", Money.of("500.00")));
        Account dest = accountRepository.save(new CashAccount(null, "Dest Acc", Money.of("0.00")));
        String linkId = UUID.randomUUID().toString();

        TransactionRecord debit = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-02-09"), Money.of("50.00"),
                "Debit leg", source.id(), linkId, null, null
        );
        TransactionRecord credit = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-02-09"), Money.of("50.00"),
                "Credit leg", dest.id(), linkId, null, null
        );

        assertThatThrownBy(() -> transactionRepository.executeTransferWithFault(debit, credit, true))
                .isInstanceOf(PersistenceException.class);

        assertThat(transactionRepository.count()).isZero();
    }

    private record QueryFixture(
            Account acc1, Account acc2, CategoryComponent catFood, CategoryComponent catTravel,
            TransactionRecord t1, TransactionRecord t2, TransactionRecord t3,
            TransactionRecord t4, TransactionRecord t5
    ) {}

    private QueryFixture seedQueryFixture() throws Exception {
        Account acc1 = accountRepository.save(new BankAccount(null, "Checking", Money.of("5000.00")));
        Account acc2 = accountRepository.save(new CashAccount(null, "Wallet", Money.of("500.00")));
        CategoryComponent catFood = categoryRepository.save(new Category(null, "Food", "EXPENSE"));
        CategoryComponent catTravel = categoryRepository.save(new Category(null, "Travel", "EXPENSE"));

        TransactionRecord t1 = transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.of(2026, 1, 10), Money.of("50.00"), "Supermarket shopping",
                acc1.id(), null, catFood.id(), null));
        TransactionRecord t2 = transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.of(2026, 1, 15), Money.of("30.00"), "Dinner with friends",
                acc1.id(), null, catFood.id(), null));
        TransactionRecord t3 = transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.of(2026, 1, 20), Money.of("20.00"), "Metro card recharge",
                acc1.id(), null, catTravel.id(), null));
        TransactionRecord t4 = transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.of(2026, 2, 5), Money.of("40.00"), "Corner supermarket",
                acc1.id(), null, catFood.id(), null));
        TransactionRecord t5 = transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.of(2026, 1, 15), Money.of("10.00"), "Snacks",
                acc2.id(), null, catFood.id(), null));
        return new QueryFixture(acc1, acc2, catFood, catTravel, t1, t2, t3, t4, t5);
    }

    @Test
    @DisplayName("TC-FR08-01: Filters transactions by date, category, account, and text, combining with AND")
    void filtersCombineWithAnd() throws Exception {
        QueryFixture f = seedQueryFixture();

        TransactionQuery q1 = TransactionQuery.builder()
                .categoryId(f.catFood().id())
                .dateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
                .build();
        assertThat(transactionRepository.findByQuery(q1))
                .extracting(TransactionRecord::id).containsExactly(f.t1().id(), f.t2().id(), f.t5().id());

        TransactionQuery q2 = TransactionQuery.builder()
                .categoryId(f.catFood().id())
                .dateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
                .accountId(f.acc1().id())
                .build();
        assertThat(transactionRepository.findByQuery(q2))
                .extracting(TransactionRecord::id).containsExactly(f.t1().id(), f.t2().id());

        TransactionQuery q3 = TransactionQuery.builder()
                .categoryId(f.catFood().id())
                .dateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
                .accountId(f.acc1().id())
                .text("DINNER")
                .build();
        assertThat(transactionRepository.findByQuery(q3))
                .extracting(TransactionRecord::id).containsExactly(f.t2().id());
    }

    @Test
    @DisplayName("TransactionQuery.all returns all persisted records")
    void emptyQueryReturnsAllRecords() throws Exception {
        Account acc = accountRepository.save(new CashAccount(null, "Wallet", Money.of("500.00")));
        transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.of(2026, 1, 10), Money.of("10.00"), "Item 1",
                acc.id(), null, null, null));
        transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.of(2026, 1, 11), Money.of("20.00"), "Item 2",
                acc.id(), null, null, null));

        List<TransactionRecord> results = transactionRepository.findByQuery(TransactionQuery.all());
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("TC-FR08-04 / D-44: Text filter escapes LIKE wildcards (%, _, \\) and matches literal substrings")
    void textFilterMatchesLiteralWildcardCharacters() throws Exception {
        Account acc = accountRepository.save(new CashAccount(null, "Wallet", Money.of("1000.00")));
        TransactionRecord matchPercent = transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.of(2026, 1, 10), Money.of("10.00"), "Discount 50% applied",
                acc.id(), null, null, null));
        TransactionRecord decoyPercent = transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.of(2026, 1, 11), Money.of("20.00"), "Discount 5000 applied",
                acc.id(), null, null, null));
        TransactionRecord matchUnderscore = transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.of(2026, 1, 12), Money.of("30.00"), "code_ref_123",
                acc.id(), null, null, null));
        TransactionRecord decoyUnderscore = transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.of(2026, 1, 13), Money.of("40.00"), "code-ref-123",
                acc.id(), null, null, null));

        TransactionQuery qPercent = TransactionQuery.builder().text("50%").build();
        List<TransactionRecord> percentResults = transactionRepository.findByQuery(qPercent);
        assertThat(percentResults).extracting(TransactionRecord::id).containsExactly(matchPercent.id());

        TransactionQuery qUnderscore = TransactionQuery.builder().text("code_ref").build();
        List<TransactionRecord> underscoreResults = transactionRepository.findByQuery(qUnderscore);
        assertThat(underscoreResults).extracting(TransactionRecord::id).containsExactly(matchUnderscore.id());
    }

    @Test
    @DisplayName("TransactionQuery returns empty list when no records match")
    void unmatchedQueryReturnsEmptyList() throws Exception {
        Account acc = accountRepository.save(new CashAccount(null, "Wallet", Money.of("500.00")));
        transactionRepository.save(new TransactionRecord(
                null, "EXPENSE", LocalDate.of(2026, 1, 10), Money.of("10.00"), "Coffee",
                acc.id(), null, null, null));

        TransactionQuery query = TransactionQuery.builder().text("NonExistent").build();
        assertThat(transactionRepository.findByQuery(query)).isEmpty();
    }

    @Test
    @DisplayName("TransactionQuery rejects start date after end date")
    void queryRejectsStartDateAfterEndDate() {
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 1, 1);
        assertThatThrownBy(() -> new TransactionQuery(start, end, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startDate cannot be after endDate");
    }

    @Test
    @DisplayName("findByTransferLinkId returns both linked legs ordered by id")
    void findByTransferLinkIdReturnsBothLegs() throws Exception {
        Account source = accountRepository.save(new BankAccount(null, "Bank Src", Money.of("1000.00")));
        Account dest = accountRepository.save(new CashAccount(null, "Cash Dst", Money.of("0.00")));
        String linkId = UUID.randomUUID().toString();
        TransactionRecord debit = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-03-01"), Money.of("150.00"),
                "Transfer out", source.id(), linkId, null, null
        );
        TransactionRecord credit = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-03-01"), Money.of("150.00"),
                "Transfer in", dest.id(), linkId, null, null
        );
        transactionRepository.executeTransfer(debit, credit);

        List<TransactionRecord> legs = transactionRepository.findByTransferLinkId(linkId);
        assertThat(legs).hasSize(2);
        assertThat(legs.get(0).accountId()).isEqualTo(source.id());
        assertThat(legs.get(1).accountId()).isEqualTo(dest.id());
    }

    @Test
    @DisplayName("deleteByTransferLinkId deletes both linked legs atomically")
    void deleteByTransferLinkIdDeletesBothLegs() throws Exception {
        Account source = accountRepository.save(new BankAccount(null, "Bank A", Money.of("1000.00")));
        Account dest = accountRepository.save(new CashAccount(null, "Cash B", Money.of("0.00")));
        String linkId = UUID.randomUUID().toString();
        TransactionRecord debit = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-03-02"), Money.of("200.00"),
                "Out", source.id(), linkId, null, null
        );
        TransactionRecord credit = new TransactionRecord(
                null, "TRANSFER", LocalDate.parse("2026-03-02"), Money.of("200.00"),
                "In", dest.id(), linkId, null, null
        );
        transactionRepository.executeTransfer(debit, credit);

        assertThat(transactionRepository.findByTransferLinkId(linkId)).hasSize(2);
        transactionRepository.deleteByTransferLinkId(linkId);
        assertThat(transactionRepository.findByTransferLinkId(linkId)).isEmpty();
    }
}
