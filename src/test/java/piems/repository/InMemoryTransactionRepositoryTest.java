package piems.repository;

/**
 * Concrete contract test verifying the in-memory implementation of {@link TransactionRepository}.
 */
class InMemoryTransactionRepositoryTest extends TransactionRepositoryContractTest {

    private final InMemoryTransactionRepository txRepo = new InMemoryTransactionRepository();
    private final InMemoryAccountRepository accRepo = new InMemoryAccountRepository(txRepo);
    private final InMemoryCategoryRepository catRepo = new InMemoryCategoryRepository(txRepo);

    @Override
    protected TransactionRepository createTransactionRepository() {
        return txRepo;
    }

    @Override
    protected AccountRepository createAccountRepository() {
        return accRepo;
    }

    @Override
    protected CategoryRepository createCategoryRepository() {
        return catRepo;
    }
}
