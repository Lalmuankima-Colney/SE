package piems.service;

/**
 * Observer interface for receiving budget threshold alert events (Decision D-09, FR-10).
 *
 * <p>Implementations must not throw exceptions that interrupt business transaction recording.
 * Any exceptions thrown by a listener are caught and contained by the subject (Decision D-09).</p>
 */
@FunctionalInterface
public interface BudgetListener {

    /**
     * Invoked when a monitored budget crosses a threshold (WARNING or EXCEEDED).
     *
     * @param event details of the budget threshold crossing event
     */
    void onBudgetAlert(BudgetAlertEvent event);
}
