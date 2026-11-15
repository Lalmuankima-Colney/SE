package piems.repository;

/**
 * Concrete contract test verifying {@link InMemoryBudgetRepository} (D-11, D-46).
 */
class InMemoryBudgetRepositoryTest extends BudgetRepositoryContractTest {

    @Override
    protected CategoryRepository createCategoryRepository() {
        return new InMemoryCategoryRepository();
    }

    @Override
    protected BudgetRepository createBudgetRepository(CategoryRepository categoryRepository) {
        return new InMemoryBudgetRepository();
    }
}
