package piems.repository;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import piems.domain.Budget;
import piems.domain.PersistenceException;

/**
 * Fast, thread-safe in-memory implementation of {@link BudgetRepository}.
 *
 * <p>Used for deterministic unit testing and domain simulations without a SQLite file (D-11).</p>
 */
public class InMemoryBudgetRepository implements BudgetRepository {

    private final Map<Long, Budget> storage = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public Budget save(Budget budget) throws PersistenceException {
        Objects.requireNonNull(budget, "budget cannot be null");
        validateUniqueCategoryAndMonth(budget);
        Long id = budget.getId();
        if (id == null) {
            id = idSequence.getAndIncrement();
        }
        Budget persisted = new Budget(id, budget.getCategory(), budget.getMonth(), budget.getLimit());
        storage.put(id, persisted);
        return persisted;
    }

    private void validateUniqueCategoryAndMonth(Budget budget) throws PersistenceException {
        Long catId = budget.getCategory().id();
        for (Budget existing : storage.values()) {
            boolean sameCat = Objects.equals(existing.getCategory().id(), catId);
            boolean sameMonth = existing.getMonth().equals(budget.getMonth());
            boolean differentId = !Objects.equals(existing.getId(), budget.getId());
            if (sameCat && sameMonth && differentId) {
                throw new PersistenceException("A budget already exists for category ID "
                        + catId + " in " + budget.getMonth());
            }
        }
    }

    @Override
    public Optional<Budget> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<Budget> findByCategoryAndMonth(long categoryId, YearMonth month) {
        if (month == null) {
            return Optional.empty();
        }
        return storage.values().stream()
                .filter(b -> Objects.equals(b.getCategory().id(), categoryId) && b.getMonth().equals(month))
                .findFirst();
    }

    @Override
    public List<Budget> findAll() {
        return storage.values().stream()
                .sorted(Comparator.comparing(Budget::getId))
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
}
