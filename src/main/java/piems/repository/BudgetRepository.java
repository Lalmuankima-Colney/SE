package piems.repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import piems.domain.Budget;
import piems.domain.PersistenceException;

/**
 * Repository interface for managing persistent {@link Budget} entities.
 *
 * <p>Supports saving, finding, listing, and deleting monthly category budgets
 * across application restarts according to Requirements FR-09, FR-18, and Decision D-46.</p>
 */
public interface BudgetRepository {

    /**
     * Persists the given budget entity, assigning an identifier if new.
     *
     * @param budget budget entity to persist
     * @return persisted budget with identifier assigned
     * @throws PersistenceException if a persistence or unique constraint error occurs
     */
    Budget save(Budget budget) throws PersistenceException;

    /**
     * Finds a budget by its persistent identifier.
     *
     * @param id persistent budget ID
     * @return optional containing the budget if found, empty otherwise
     * @throws PersistenceException if querying fails
     */
    Optional<Budget> findById(long id) throws PersistenceException;

    /**
     * Finds a budget for the specified category ID and calendar month.
     *
     * @param categoryId persistent category ID
     * @param month      target calendar year and month
     * @return optional containing the budget if found, empty otherwise
     * @throws PersistenceException if querying fails
     */
    Optional<Budget> findByCategoryAndMonth(long categoryId, YearMonth month) throws PersistenceException;

    /**
     * Returns all persisted budgets ordered by identifier.
     *
     * @return list of all persisted budgets
     * @throws PersistenceException if querying fails
     */
    List<Budget> findAll() throws PersistenceException;

    /**
     * Deletes the budget with the specified identifier.
     *
     * @param id persistent budget ID
     * @throws PersistenceException if deletion fails
     */
    void delete(long id) throws PersistenceException;

    /**
     * Returns the total count of persisted budgets.
     *
     * @return total budget count
     * @throws PersistenceException if querying fails
     */
    int count() throws PersistenceException;
}
