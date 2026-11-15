package piems.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import piems.domain.Account;
import piems.domain.BankAccount;
import piems.domain.CashAccount;
import piems.domain.CreditCardAccount;
import piems.domain.Money;
import piems.domain.PersistenceException;

/**
 * Fast, database-free in-memory implementation of {@link AccountRepository}.
 *
 * <p>Used for deterministic unit testing and domain simulations per Decision D-11.</p>
 */
public class InMemoryAccountRepository implements AccountRepository {

    private final Map<Long, Account> storage = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);
    private TransactionRepository transactionRepository;

    /**
     * Constructs an in-memory account repository with no linked transaction store.
     */
    public InMemoryAccountRepository() {
        this(null);
    }

    /**
     * Constructs an in-memory account repository linked to a transaction repository.
     *
     * @param transactionRepository transaction store for derived balance calculation
     */
    public InMemoryAccountRepository(TransactionRepository transactionRepository) {
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
    public Account save(Account account) throws PersistenceException {
        Objects.requireNonNull(account, "Account cannot be null");
        validateUniqueName(account);
        Long id = account.id();
        if (id == null) {
            id = idSequence.getAndIncrement();
        }
        Account persisted = recreateWithId(account, id);
        storage.put(id, persisted);
        return persisted;
    }

    private void validateUniqueName(Account account) throws PersistenceException {
        for (Account existing : storage.values()) {
            boolean sameName = existing.name().equalsIgnoreCase(account.name());
            boolean differentId = !Objects.equals(existing.id(), account.id());
            if (sameName && differentId) {
                throw new PersistenceException("Account with name '" + account.name() + "' already exists");
            }
        }
    }

    private Account recreateWithId(Account account, Long id) {
        return switch (account.type()) {
            case "CASH" -> new CashAccount(id, account.name(), account.openingBalance(), account.isArchived());
            case "BANK" -> new BankAccount(id, account.name(), account.openingBalance(), account.isArchived());
            case "CREDIT_CARD" ->
                new CreditCardAccount(id, account.name(), account.creditLimit(), account.isArchived());
            default -> throw new IllegalArgumentException("Unknown account type: " + account.type());
        };
    }

    @Override
    public Optional<Account> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<Account> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return storage.values().stream()
                .filter(a -> a.name().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    @Override
    public List<Account> findAll() {
        return storage.values().stream()
                .sorted(Comparator.comparing(Account::id))
                .toList();
    }

    @Override
    public void delete(long id) throws PersistenceException {
        if (!storage.containsKey(id)) {
            return;
        }
        if (transactionRepository != null && !transactionRepository.findByAccountId(id).isEmpty()) {
            throw new PersistenceException("Cannot delete account with existing transactions");
        }
        storage.remove(id);
    }

    @Override
    public int count() {
        return storage.size();
    }

    @Override
    public Money getDerivedBalance(long accountId) throws PersistenceException {
        Account account = storage.get(accountId);
        if (account == null) {
            throw new PersistenceException("Account not found for id: " + accountId);
        }
        if (transactionRepository == null) {
            return account.balance();
        }
        return computeDerivedBalance(account);
    }

    private Money computeDerivedBalance(Account account) throws PersistenceException {
        if ("CREDIT_CARD".equals(account.type())) {
            return Money.zero(account.openingBalance().currency());
        }
        long balanceMinor = account.openingBalance().minorUnits();
        List<TransactionRecord> txs = transactionRepository.findByAccountId(account.id());
        for (TransactionRecord tx : txs) {
            balanceMinor += computeEffectMinor(tx, account.id());
        }
        return Money.ofMinor(balanceMinor, account.openingBalance().currency());
    }

    private long computeEffectMinor(TransactionRecord tx, long accountId) throws PersistenceException {
        if ("INCOME".equalsIgnoreCase(tx.kind())) {
            return tx.amount().minorUnits();
        }
        if ("EXPENSE".equalsIgnoreCase(tx.kind())) {
            return -tx.amount().minorUnits();
        }
        if ("TRANSFER".equalsIgnoreCase(tx.kind()) && tx.transferLinkId() != null) {
            return computeTransferLegEffect(tx, accountId);
        }
        return 0;
    }

    private long computeTransferLegEffect(TransactionRecord tx, long accountId) throws PersistenceException {
        List<TransactionRecord> pair = transactionRepository.findAll().stream()
                .filter(t -> tx.transferLinkId().equals(t.transferLinkId()))
                .sorted(Comparator.comparing(TransactionRecord::id))
                .toList();
        if (pair.isEmpty()) {
            return 0;
        }
        boolean isDebitLeg = Objects.equals(pair.get(0).id(), tx.id());
        return isDebitLeg ? -tx.amount().minorUnits() : tx.amount().minorUnits();
    }
}
