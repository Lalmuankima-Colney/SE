package piems.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.domain.Category;
import piems.domain.CategoryComponent;
import piems.domain.CategoryGroup;
import piems.domain.PersistenceException;

/**
 * Abstract contract test verifying that all {@link CategoryRepository} implementations
 * adhere strictly to the repository interface contract (Decisions D-08, D-11, D-26).
 */
public abstract class CategoryRepositoryContractTest {

    private CategoryRepository repository;

    /**
     * Factory method creating a clean, isolated repository instance.
     *
     * @return fresh CategoryRepository instance
     * @throws Exception if setup fails
     */
    protected abstract CategoryRepository createRepository() throws Exception;

    @BeforeEach
    void setUp() throws Exception {
        repository = createRepository();
    }

    @Test
    @DisplayName("Saves and retrieves a leaf category with exact fields")
    void savesAndFindsLeafCategory() throws Exception {
        Category category = new Category(null, "Groceries", "EXPENSE", false);
        CategoryComponent saved = repository.save(category);

        assertThat(saved.id()).isNotNull().isPositive();
        assertThat(saved.name()).isEqualTo("Groceries");
        assertThat(saved.kind()).isEqualTo("EXPENSE");
        assertThat(saved.isArchived()).isFalse();
        assertThat(saved.isGroup()).isFalse();

        Optional<CategoryComponent> found = repository.findById(saved.id());
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Groceries");
        assertThat(found.get().kind()).isEqualTo("EXPENSE");
        assertThat(found.get().isGroup()).isFalse();
    }

    @Test
    @DisplayName("Saves and retrieves composite category group with children")
    void savesAndFindsCategoryGroupWithChildren() throws Exception {
        CategoryGroup group = new CategoryGroup(null, "Food & Dining", "EXPENSE", false);
        Category leaf1 = new Category(null, "Groceries", "EXPENSE", false);
        Category leaf2 = new Category(null, "Restaurants", "EXPENSE", false);
        group.add(leaf1);
        group.add(leaf2);

        CategoryComponent savedGroup = repository.save(group);
        assertThat(savedGroup.id()).isNotNull().isPositive();

        Optional<CategoryComponent> fetched = repository.findById(savedGroup.id());
        assertThat(fetched).isPresent();
        CategoryComponent component = fetched.get();
        assertThat(component.isGroup()).isTrue();
        assertThat(component.childCount()).isEqualTo(2);
        assertThat(component.leaves()).hasSize(2);
    }

    @Test
    @DisplayName("Finds category component by display name case-insensitively")
    void findsCategoryByName() throws Exception {
        repository.save(new Category(null, "Utilities", "EXPENSE"));

        Optional<CategoryComponent> exact = repository.findByName("Utilities");
        Optional<CategoryComponent> lower = repository.findByName("utilities");
        Optional<CategoryComponent> missing = repository.findByName("NonExistent");

        assertThat(exact).isPresent();
        assertThat(lower).isPresent();
        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("findAll returns all root category components")
    void findAllReturnsRootCategories() throws Exception {
        CategoryGroup parent = new CategoryGroup(null, "Entertainment", "EXPENSE");
        Category child = new Category(null, "Movies", "EXPENSE");
        parent.add(child);
        repository.save(parent);

        repository.save(new Category(null, "Salary", "INCOME"));

        List<CategoryComponent> roots = repository.findAll();
        assertThat(roots).hasSize(2);
    }

    @Test
    @DisplayName("Updates category name and archive status")
    void updatesCategory() throws Exception {
        CategoryComponent saved = repository.save(new Category(null, "Travel", "EXPENSE", false));
        CategoryComponent updated = new Category(saved.id(), "Travel & Holidays", "EXPENSE", true);

        repository.save(updated);

        Optional<CategoryComponent> reloaded = repository.findById(saved.id());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().name()).isEqualTo("Travel & Holidays");
        assertThat(reloaded.get().isArchived()).isTrue();
    }

    @Test
    @DisplayName("Deletes an unused category successfully")
    void deletesUnusedCategory() throws Exception {
        CategoryComponent saved = repository.save(new Category(null, "To Delete", "EXPENSE"));
        assertThat(repository.findById(saved.id())).isPresent();

        repository.delete(saved.id());

        assertThat(repository.findById(saved.id())).isEmpty();
    }
}
