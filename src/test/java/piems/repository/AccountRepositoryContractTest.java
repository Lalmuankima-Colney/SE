package piems.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.domain.Account;
import piems.domain.BankAccount;
import piems.domain.CashAccount;
import piems.domain.CreditCardAccount;
import piems.domain.Money;
import piems.domain.PersistenceException;

/**
 * Abstract contract test verifying that all {@link AccountRepository} implementations
 * adhere strictly to the repository interface contract (Decision D-11).
 */
public abstract class AccountRepositoryContractTest {

    private AccountRepository repository;

    /**
     * Factory method creating a clean, isolated repository instance.
     *
     * @return fresh AccountRepository instance
     * @throws Exception if setup fails
     */
    protected abstract AccountRepository createRepository() throws Exception;

    @BeforeEach
    void setUp() throws Exception {
        repository = createRepository();
    }

    @Test
    @DisplayName("Saves and retrieves a cash account with exact fields")
    void savesAndFindsCashAccount() throws Exception {
        Account account = new CashAccount(null, "Pocket Cash", Money.of("150.00"), false);
        Account saved = repository.save(account);

        assertThat(saved.id()).isNotNull().isPositive();
        assertThat(saved.name()).isEqualTo("Pocket Cash");
        assertThat(saved.type()).isEqualTo("CASH");
        assertThat(saved.openingBalance()).isEqualTo(Money.of("150.00"));
        assertThat(saved.isArchived()).isFalse();

        Optional<Account> found = repository.findById(saved.id());
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Pocket Cash");
        assertThat(found.get().type()).isEqualTo("CASH");
    }

    @Test
    @DisplayName("Saves and retrieves bank and credit card accounts")
    void savesAndFindsBankAndCreditCardAccounts() throws Exception {
        Account bank = repository.save(new BankAccount(null, "Salary Checking", Money.of("2500.00"), false));
        Account card = repository.save(new CreditCardAccount(null, "Rewards Card", Money.of("5000.00"), false));

        assertThat(bank.type()).isEqualTo("BANK");
        assertThat(card.type()).isEqualTo("CREDIT_CARD");
        assertThat(card.creditLimit()).isEqualTo(Money.of("5000.00"));

        assertThat(repository.findById(bank.id())).isPresent();
        assertThat(repository.findById(card.id())).isPresent();
    }

    @Test
    @DisplayName("Finds account by display name case-insensitively")
    void findsAccountByName() throws Exception {
        repository.save(new CashAccount(null, "Main Wallet", Money.of("50.00")));

        Optional<Account> exact = repository.findByName("Main Wallet");
        Optional<Account> caseInsensitive = repository.findByName("main wallet");
        Optional<Account> missing = repository.findByName("Ghost Account");

        assertThat(exact).isPresent();
        assertThat(caseInsensitive).isPresent();
        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("findAll returns all persisted accounts")
    void findAllReturnsAllPersistedAccounts() throws Exception {
        repository.save(new CashAccount(null, "Wallet A", Money.of("10.00")));
        repository.save(new BankAccount(null, "Bank B", Money.of("20.00")));
        repository.save(new CreditCardAccount(null, "Card C", Money.of("30.00")));

        List<Account> all = repository.findAll();
        assertThat(all).hasSize(3);
        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("Updates existing account details and archive status")
    void updatesExistingAccount() throws Exception {
        Account saved = repository.save(new BankAccount(null, "Emergency Fund", Money.of("1000.00"), false));
        Account updated = new BankAccount(saved.id(), "Emergency Savings", Money.of("1000.00"), true);

        repository.save(updated);

        Optional<Account> reloaded = repository.findById(saved.id());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().name()).isEqualTo("Emergency Savings");
        assertThat(reloaded.get().isArchived()).isTrue();
    }

    @Test
    @DisplayName("Deletes an account successfully")
    void deletesAccount() throws Exception {
        Account saved = repository.save(new CashAccount(null, "Temporary Cash", Money.of("20.00")));
        assertThat(repository.findById(saved.id())).isPresent();

        repository.delete(saved.id());

        assertThat(repository.findById(saved.id())).isEmpty();
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("Rejects duplicate account names with PersistenceException")
    void rejectsDuplicateAccountNames() throws Exception {
        repository.save(new CashAccount(null, "Unique Name", Money.of("10.00")));

        assertThatThrownBy(() -> repository.save(new CashAccount(null, "Unique Name", Money.of("20.00"))))
                .isInstanceOf(PersistenceException.class);
    }
}
