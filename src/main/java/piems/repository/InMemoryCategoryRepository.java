package piems.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import piems.domain.CategoryComponent;
import piems.domain.PersistenceException;

/**
 * Fast, database-free in-memory implementation of {@link CategoryRepository}.
 *
 * <p>Supports hierarchical composite category trees and soft-delete archiving
 * per Requirements FR-05, FR-18, and Decisions D-08 and D-11.</p>
 */
public class InMemoryCategoryRepository implements CategoryRepository {

    private final Map<Long, CategoryComponent> storage = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);
    private TransactionRepository transactionRepository;

    /**
     * Constructs an in-memory category repository without a linked transaction store.
     */
    public InMemoryCategoryRepository() {
        this(null);
    }

    /**
     * Constructs an in-memory category repository linked to a transaction store.
     *
     * @param transactionRepository transaction store for verifying in-use constraints
     */
    public InMemoryCategoryRepository(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Configures the transaction repository collaborator.
     *
     * @param transactionRepository transaction store
     */
    public void setTransactionRepository(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public CategoryComponent save(CategoryComponent category) throws PersistenceException {
        Objects.requireNonNull(category, "Category cannot be null");
        validateUniqueName(category);
        Long id = category.id();
        if (id == null) {
            id = idSequence.getAndIncrement();
        }
        CategoryComponent persisted = category.copyWithId(id);
        storage.put(id, persisted);
        return persisted;
    }

    private void validateUniqueName(CategoryComponent category) throws PersistenceException {
        for (CategoryComponent existing : storage.values()) {
            boolean sameName = existing.name().equalsIgnoreCase(category.name());
            boolean sameKind = existing.kind().equalsIgnoreCase(category.kind());
            boolean differentId = !Objects.equals(existing.id(), category.id());
            if (sameName && sameKind && differentId) {
                throw new PersistenceException("Category with name '" + category.name() + "' already exists");
            }
        }
    }

    @Override
    public Optional<CategoryComponent> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<CategoryComponent> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return storage.values().stream()
                .filter(c -> c.name().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    @Override
    public List<CategoryComponent> findAll() {
        return storage.values().stream()
                .filter(c -> c.parent() == null)
                .sorted(Comparator.comparing(CategoryComponent::id))
                .toList();
    }

    @Override
    public void delete(long id) throws PersistenceException {
        CategoryComponent component = storage.get(id);
        if (component == null) {
            return;
        }
        if (component.isInUse()) {
            throw new PersistenceException("Cannot delete category '" + component.name() + "' because it is in use");
        }
        if (transactionRepository != null && !transactionRepository.findByCategoryId(id).isEmpty()) {
            throw new PersistenceException("Cannot delete category with associated transactions");
        }
        storage.remove(id);
    }

    @Override
    public int count() {
        return storage.size();
    }
}
