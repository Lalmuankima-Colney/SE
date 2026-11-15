package piems.domain;

/**
 * Lifecycle status of a savings goal (FR-12, Decision Q-008, D-35).
 *
 * <p>Under Decision Q-008 and D-35, PIEMS v1.0 models transitions between
 * {@link #ACTIVE} and {@link #ACHIEVED} only. The 'OVERDUE' and 'CANCELLED'
 * states are deferred to post-v1.0.</p>
 */
public enum GoalStatus {

    /**
     * Goal is in progress and total contributions have not yet reached the target amount.
     */
    ACTIVE,

    /**
     * Total contributions have reached or exceeded the target amount (TC-FR12-01).
     */
    ACHIEVED
}
