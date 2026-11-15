package piems.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import piems.domain.PersistenceException;

/**
 * Fast, database-free in-memory implementation of {@link TransactionRepository}.
 *
 * <p>Supports atomic two-leg transfers with simulated rollback and fault injection
 * per Requirements FR-04, FR-18, NFR-03, and Decisions D-11 and D-23.</p>
 */
public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<Long, TransactionRecord> storage = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public TransactionRecord save(TransactionRecord record) {
        Objects.requireNonNull(record, "TransactionRecord cannot be null");
        Long id = record.id();
        if (id == null) {
            id = idSequence.getAndIncrement();
        }
        TransactionRecord persisted = new TransactionRecord(
                id,
                record.kind(),
                record.date(),
                record.amount(),
                record.note(),
                record.accountId(),
                record.transferLinkId(),
                record.categoryId(),
                record.ruleId()
        );
        storage.put(id, persisted);
        return persisted;
    }

    @Override
    public Optional<TransactionRecord> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<TransactionRecord> findAll() {
        return storage.values().stream()
                .sorted(Comparator.comparing(TransactionRecord::id))
                .toList();
    }

    @Override
    public List<TransactionRecord> findByAccountId(long accountId) {
        return storage.values().stream()
                .filter(r -> r.accountId() == accountId)
                .sorted(Comparator.comparing(TransactionRecord::id))
                .toList();
    }

    @Override
    public List<TransactionRecord> findByCategoryId(long categoryId) {
        return storage.values().stream()
                .filter(r -> r.categoryId() != null && r.categoryId() == categoryId)
                .sorted(Comparator.comparing(TransactionRecord::id))
                .toList();
    }

    @Override
    public void delete(long id) {
        storage.remove(id);
    }

    @Override
    public int count() {
        return storage.size();
    }

    @Override
    public void executeTransfer(TransactionRecord debitLeg, TransactionRecord creditLeg)
            throws PersistenceException {
        executeTransferWithFault(debitLeg, creditLeg, false);
    }

    @Override
    public void executeTransferWithFault(
            TransactionRecord debitLeg,
            TransactionRecord creditLeg,
            boolean faultSecondLeg
    ) throws PersistenceException {
        Objects.requireNonNull(debitLeg, "debitLeg cannot be null");
        Objects.requireNonNull(creditLeg, "creditLeg cannot be null");

        TransactionRecord savedDebit = save(debitLeg);
        if (faultSecondLeg) {
            storage.remove(savedDebit.id());
            throw new PersistenceException("Transfer rolled back: Injected fault during second transfer leg");
        }
        save(creditLeg);
    }

    @Override
    public List<TransactionRecord> findByQuery(TransactionQuery query) {
        Objects.requireNonNull(query, "query cannot be null");
        return storage.values().stream()
                .filter(query::matches)
                .sorted(Comparator.comparing(TransactionRecord::id))
                .toList();
    }

    @Override
    public List<TransactionRecord> findByTransferLinkId(String linkId) {
        Objects.requireNonNull(linkId, "linkId cannot be null");
        return storage.values().stream()
                .filter(r -> linkId.equals(r.transferLinkId()))
                .sorted(Comparator.comparing(TransactionRecord::id))
                .toList();
    }

    @Override
    public void deleteByTransferLinkId(String linkId) {
        Objects.requireNonNull(linkId, "linkId cannot be null");
        storage.entrySet().removeIf(entry -> linkId.equals(entry.getValue().transferLinkId()));
    }
}
