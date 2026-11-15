package piems.ui.swing;

import java.util.Objects;
import piems.domain.Money;

/**
 * View model representing an account's derived balance and type for UI rendering (FR-07).
 *
 * @param id      account identifier
 * @param name    account display name
 * @param type    account type ("CASH", "BANK", "CREDIT_CARD")
 * @param balance dynamically derived current balance
 */
public record AccountBalanceItem(
        Long id,
        String name,
        String type,
        Money balance
) {
    /**
     * Canonical constructor validating non-null parameters.
     */
    public AccountBalanceItem {
        Objects.requireNonNull(name, "Account name cannot be null");
        Objects.requireNonNull(type, "Account type cannot be null");
        Objects.requireNonNull(balance, "Balance cannot be null");
    }
}
