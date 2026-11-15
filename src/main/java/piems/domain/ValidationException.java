package piems.domain;

/**
 * Checked exception thrown when business validation fails for user or external input.
 *
 * <p>Following Decision D-13, recoverable input and business rule validation errors
 * are represented as checked exceptions handled at service or UI boundaries.</p>
 */
public class ValidationException extends PiemsException {

    /**
     * Constructs a new ValidationException with the specified detail message.
     *
     * @param message description of the validation failure
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new ValidationException with the specified message and cause.
     *
     * @param message description of the validation failure
     * @param cause   underlying cause
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
