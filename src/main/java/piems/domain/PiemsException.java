package piems.domain;

/**
 * Base checked exception for all recoverable domain, service, and persistence errors.
 *
 * <p>Following Decision D-13, programmer errors raise {@link IllegalArgumentException},
 * while recoverable conditions are modeled as checked subclasses of {@code PiemsException}
 * and handled at service or UI boundaries.</p>
 */
public class PiemsException extends Exception {

    /**
     * Constructs a new PiemsException with the specified detail message.
     *
     * @param message the detail message
     */
    public PiemsException(String message) {
        super(message);
    }

    /**
     * Constructs a new PiemsException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public PiemsException(String message, Throwable cause) {
        super(message, cause);
    }
}
