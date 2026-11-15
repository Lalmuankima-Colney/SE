package piems.repository;

/**
 * Concrete contract test verifying the in-memory implementation of {@link AccountRepository}.
 */
class InMemoryAccountRepositoryTest extends AccountRepositoryContractTest {

    @Override
    protected AccountRepository createRepository() {
        return new InMemoryAccountRepository();
    }
}
