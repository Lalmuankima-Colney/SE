package piems.domain;

/**
 * Checked exception thrown when an entity record cannot be located by its identifier.
 *
 * <p>Following Decision D-13, recoverable lookup misses are represented
 * as checked exceptions handled at service or UI boundaries.</p>
 */
public class RecordNotFoundException extends PiemsException {

    /**
     * Constructs a new RecordNotFoundException with the specified detail message.
     *
     * @param message description of the missing record
     */
    public RecordNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new RecordNotFoundException with the specified message and cause.
     *
     * @param message description of the missing record
     * @param cause   underlying cause
     */
    public RecordNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
