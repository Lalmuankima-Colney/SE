package piems.repository;

import java.util.List;
import java.util.Optional;
import piems.domain.CategoryComponent;
import piems.domain.PersistenceException;

/**
 * Repository interface for managing persistent {@link CategoryComponent} hierarchies.
 *
 * <p>Supports hierarchical composite categories and soft-delete archiving
 * according to Requirements FR-05, FR-18, and Decisions D-08, D-11, and D-26.</p>
 */
public interface CategoryRepository {

    /**
     * Persists the category component or tree, assigning identifiers as needed.
     *
     * @param category category component to persist
     * @return persisted category component with identifier assigned
     * @throws PersistenceException if a persistence error occurs
     */
    CategoryComponent save(CategoryComponent category) throws PersistenceException;

    /**
     * Finds a category component by its unique identifier.
     *
     * @param id category identifier
     * @return optional containing the category if found, empty otherwise
     * @throws PersistenceException if querying fails
     */
    Optional<CategoryComponent> findById(long id) throws PersistenceException;

    /**
     * Finds a category component by its unique name.
     *
     * @param name category name
     * @return optional containing the category if found, empty otherwise
     * @throws PersistenceException if querying fails
     */
    Optional<CategoryComponent> findByName(String name) throws PersistenceException;

    /**
     * Returns all persisted root category components.
     *
     * @return list of root category components
     * @throws PersistenceException if querying fails
     */
    List<CategoryComponent> findAll() throws PersistenceException;

    /**
     * Deletes the category component with the specified identifier.
     *
     * @param id category identifier
     * @throws PersistenceException if deletion fails or category is in use
     */
    void delete(long id) throws PersistenceException;

    /**
     * Returns the total count of category components.
     *
     * @return category count
     * @throws PersistenceException if querying fails
     */
    int count() throws PersistenceException;
}
