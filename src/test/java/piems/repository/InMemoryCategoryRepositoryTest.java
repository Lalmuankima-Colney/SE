package piems.repository;

/**
 * Concrete contract test verifying the in-memory implementation of {@link CategoryRepository}.
 */
class InMemoryCategoryRepositoryTest extends CategoryRepositoryContractTest {

    @Override
    protected CategoryRepository createRepository() {
        return new InMemoryCategoryRepository();
    }
}
