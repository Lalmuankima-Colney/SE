package piems.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.domain.Budget;
import piems.domain.Category;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.domain.PersistenceException;

/**
 * Abstract contract test verifying that {@link BudgetRepository} implementations
 * adhere strictly to the repository contract (FR-09, FR-18, Decisions D-11, D-46).
 */
public abstract class BudgetRepositoryContractTest {

    private BudgetRepository repository;
    private CategoryRepository categoryRepository;
    private CategoryComponent foodCategory;
    private CategoryComponent utilityCategory;

    /**
     * Factory method to create an isolated category repository.
     *
     * @return fresh CategoryRepository instance
     * @throws Exception if setup fails
     */
    protected abstract CategoryRepository createCategoryRepository() throws Exception;

    /**
     * Factory method to create an isolated budget repository.
     *
     * @param categoryRepository category repository dependency
     * @return fresh BudgetRepository instance
     * @throws Exception if setup fails
     */
    protected abstract BudgetRepository createBudgetRepository(CategoryRepository categoryRepository) throws Exception;

    @BeforeEach
    void setUp() throws Exception {
        categoryRepository = createCategoryRepository();
        repository = createBudgetRepository(categoryRepository);
        foodCategory = categoryRepository.save(new Category(null, "Food", "EXPENSE", false));
        utilityCategory = categoryRepository.save(new Category(null, "Utilities", "EXPENSE", false));
    }

    @Test
    @DisplayName("TC-FR09-04: Saves and retrieves budget with exact attributes")
    void savesAndRetrievesBudgetWithExactAttributes() throws Exception {
        YearMonth month = YearMonth.of(2026, 6);
        Budget budget = new Budget(foodCategory, month, Money.of("400.00"));
        Budget saved = repository.save(budget);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getCategory().id()).isEqualTo(foodCategory.id());
        assertThat(saved.getMonth()).isEqualTo(month);
        assertThat(saved.getLimit()).isEqualTo(Money.of("400.00"));

        Optional<Budget> foundById = repository.findById(saved.getId());
        assertThat(foundById).isPresent();
        assertThat(foundById.get().getLimit()).isEqualTo(Money.of("400.00"));

        Optional<Budget> foundByCatMonth = repository.findByCategoryAndMonth(foodCategory.id(), month);
        assertThat(foundByCatMonth).isPresent();
        assertThat(foundByCatMonth.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("TC-FR09-05: Rejects duplicate budget for same category and month")
    void rejectsDuplicateBudgetForSameCategoryAndMonth() throws Exception {
        YearMonth month = YearMonth.of(2026, 6);
        repository.save(new Budget(foodCategory, month, Money.of("400.00")));

        Budget duplicate = new Budget(foodCategory, month, Money.of("500.00"));
        assertThatThrownBy(() -> repository.save(duplicate))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("TC-FR09-06: Updates existing budget limit")
    void updatesBudgetLimit() throws Exception {
        YearMonth month = YearMonth.of(2026, 7);
        Budget saved = repository.save(new Budget(foodCategory, month, Money.of("300.00")));

        Budget updated = new Budget(saved.getId(), foodCategory, month, Money.of("450.00"));
        repository.save(updated);

        Optional<Budget> retrieved = repository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getLimit()).isEqualTo(Money.of("450.00"));
    }

    @Test
    @DisplayName("TC-FR09-07: Deletes budget by identifier")
    void deletesBudget() throws Exception {
        YearMonth month = YearMonth.of(2026, 8);
        Budget saved = repository.save(new Budget(foodCategory, month, Money.of("200.00")));
        assertThat(repository.findById(saved.getId())).isPresent();

        repository.delete(saved.getId());
        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("TC-FR09-08: findAll returns all persisted budgets")
    void findAllReturnsAllBudgets() throws Exception {
        repository.save(new Budget(foodCategory, YearMonth.of(2026, 5), Money.of("200.00")));
        repository.save(new Budget(utilityCategory, YearMonth.of(2026, 5), Money.of("150.00")));
        repository.save(new Budget(foodCategory, YearMonth.of(2026, 6), Money.of("250.00")));

        List<Budget> all = repository.findAll();
        assertThat(all).hasSize(3);
        assertThat(repository.count()).isEqualTo(3);
    }
}
