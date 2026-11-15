package piems.service;

import piems.domain.PiemsException;

/**
 * Checked exception indicating an unrecoverable CSV file structure or header failure (D-13, FR-17).
 */
public class ImportException extends PiemsException {

    /**
     * Constructs an ImportException with a descriptive error message.
     *
     * @param message explanation of the import failure
     */
    public ImportException(String message) {
        super(message);
    }

    /**
     * Constructs an ImportException with an error message and underlying cause.
     *
     * @param message explanation of the import failure
     * @param cause   underlying exception
     */
    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
