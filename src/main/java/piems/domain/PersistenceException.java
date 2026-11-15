package piems.domain;

/**
 * Checked exception thrown when a database or persistence operation fails.
 *
 * <p>Following Decision D-13, persistence failures roll back database transactions
 * and wrap the underlying causes inside a {@code PersistenceException}.</p>
 */
public class PersistenceException extends PiemsException {

    /**
     * Constructs a new PersistenceException with the specified detail message.
     *
     * @param message the detail message
     */
    public PersistenceException(String message) {
        super(message);
    }

    /**
     * Constructs a new PersistenceException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
