package piems.repository;

import java.util.List;
import java.util.Optional;
import piems.domain.PersistenceException;

/**
 * Repository interface for managing transaction persistence and atomic transfers.
 *
 * <p>Enforces atomic multi-statement transfer operations, automatic rollback on error,
 * and exact monetary representation per Requirements FR-04, FR-18, NFR-03,
 * and Decisions D-11, D-23, and D-33.</p>
 */
public interface TransactionRepository {

    /**
     * Persists the given transaction record, assigning an identifier if new.
     *
     * @param record transaction record to persist
     * @return persisted transaction record with generated identifier
     * @throws PersistenceException if insertion or update fails
     */
    TransactionRecord save(TransactionRecord record) throws PersistenceException;

    /**
     * Finds a transaction record by its unique identifier.
     *
     * @param id transaction identifier
     * @return optional containing the record if found, empty otherwise
     * @throws PersistenceException if querying fails
     */
    Optional<TransactionRecord> findById(long id) throws PersistenceException;

    /**
     * Returns all persisted transaction records.
     *
     * @return list of transaction records
     * @throws PersistenceException if querying fails
     */
    List<TransactionRecord> findAll() throws PersistenceException;

    /**
     * Returns all transaction records associated with the specified account.
     *
     * @param accountId account identifier
     * @return list of transaction records for the account
     * @throws PersistenceException if querying fails
     */
    List<TransactionRecord> findByAccountId(long accountId) throws PersistenceException;

    /**
     * Returns all transaction records categorized under the specified category.
     *
     * @param categoryId category identifier
     * @return list of transaction records for the category
     * @throws PersistenceException if querying fails
     */
    List<TransactionRecord> findByCategoryId(long categoryId) throws PersistenceException;

    /**
     * Deletes the transaction record with the specified identifier.
     *
     * @param id transaction identifier
     * @throws PersistenceException if deletion fails
     */
    void delete(long id) throws PersistenceException;

    /**
     * Returns the total count of persisted transactions.
     *
     * @return transaction count
     * @throws PersistenceException if querying fails
     */
    int count() throws PersistenceException;

    /**
     * Executes an atomic transfer writing both debit and credit legs in one transaction.
     *
     * @param debitLeg  debit leg of the transfer
     * @param creditLeg credit leg of the transfer
     * @throws PersistenceException if either leg fails, triggering a rollback
     */
    void executeTransfer(TransactionRecord debitLeg, TransactionRecord creditLeg)
            throws PersistenceException;

    /**
     * Executes an atomic transfer with optional fault injection on the second leg.
     *
     * @param debitLeg       debit leg of the transfer
     * @param creditLeg      credit leg of the transfer
     * @param faultSecondLeg if true, injects a failure on the second leg to trigger rollback
     * @throws PersistenceException if writing fails or fault is injected
     */
    void executeTransferWithFault(
            TransactionRecord debitLeg,
            TransactionRecord creditLeg,
            boolean faultSecondLeg
    ) throws PersistenceException;

    /**
     * Finds all transaction records matching the specified query criteria (FR-08).
     *
     * @param query search and filter criteria combined with logical AND
     * @return list of matching transaction records ordered by identifier
     * @throws PersistenceException if querying fails
     */
    List<TransactionRecord> findByQuery(TransactionQuery query) throws PersistenceException;

    /**
     * Returns all transaction records sharing the specified transfer link identifier (FR-04, FR-06).
     *
     * @param linkId transfer link identifier
     * @return list of transaction records sharing the transfer link identifier
     * @throws PersistenceException if querying fails
     */
    List<TransactionRecord> findByTransferLinkId(String linkId) throws PersistenceException;

    /**
     * Deletes all transaction records sharing the specified transfer link identifier (FR-04, FR-06, D-23).
     *
     * @param linkId transfer link identifier
     * @throws PersistenceException if deletion fails
     */
    void deleteByTransferLinkId(String linkId) throws PersistenceException;
}
