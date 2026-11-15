package piems.domain;

/**
 * Checked exception thrown when an account cannot accept a debit operation
 * due to insufficient available funds, exceeding credit limits, or being archived.
 *
 * <p>Following Decision D-13, recoverable transaction errors are represented
 * as checked exceptions handled at service or UI boundaries.</p>
 */
public class InsufficientFundsException extends PiemsException {

    /**
     * Constructs a new InsufficientFundsException with the specified detail message.
     *
     * @param message detail error message
     */
    public InsufficientFundsException(String message) {
        super(message);
    }

    /**
     * Constructs a new InsufficientFundsException with the specified message and cause.
     *
     * @param message detail error message
     * @param cause   underlying cause
     */
    public InsufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }
}
