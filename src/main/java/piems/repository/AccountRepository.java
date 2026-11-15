package piems.repository;

import java.util.List;
import java.util.Optional;
import piems.domain.Account;
import piems.domain.Money;
import piems.domain.PersistenceException;

/**
 * Repository interface for managing persistent {@link Account} entities.
 *
 * <p>Following Decision D-11, repository contracts are independent of persistence
 * mechanisms and are implemented by both production JDBC and fast in-memory stores.</p>
 */
public interface AccountRepository {

    /**
     * Persists the given account, assigning an identifier if new.
     *
     * @param account account entity to persist
     * @return persisted account with identity assigned
     * @throws PersistenceException if a persistence error occurs
     */
    Account save(Account account) throws PersistenceException;

    /**
     * Finds an account by its unique identifier.
     *
     * @param id account identifier
     * @return optional containing the account if found, empty otherwise
     * @throws PersistenceException if querying fails
     */
    Optional<Account> findById(long id) throws PersistenceException;

    /**
     * Finds an account by its unique display name.
     *
     * @param name account display name
     * @return optional containing the account if found, empty otherwise
     * @throws PersistenceException if querying fails
     */
    Optional<Account> findByName(String name) throws PersistenceException;

    /**
     * Returns all persisted accounts ordered by ID.
     *
     * @return unmodifiable or detached list of all accounts
     * @throws PersistenceException if querying fails
     */
    List<Account> findAll() throws PersistenceException;

    /**
     * Deletes the account with the specified identifier.
     *
     * @param id account identifier
     * @throws PersistenceException if deletion fails or account is in use
     */
    void delete(long id) throws PersistenceException;

    /**
     * Returns the total count of persisted accounts.
     *
     * @return account count
     * @throws PersistenceException if querying fails
     */
    int count() throws PersistenceException;

    /**
     * Dynamically calculates the derived balance of an account from its opening balance
     * and applied transaction effects, per Decision D-25.
     *
     * @param accountId account identifier
     * @return derived current balance
     * @throws PersistenceException if querying fails or account is not found
     */
    Money getDerivedBalance(long accountId) throws PersistenceException;
}
