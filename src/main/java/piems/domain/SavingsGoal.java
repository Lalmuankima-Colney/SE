package piems.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Savings goal entity tracking progress towards a financial target (FR-12, UC-09, Q-008, D-35).
 *
 * <h2>State Transition Table (Decision Q-008, D-35 &amp; docs/diagrams/state-goal.mmd)</h2>
 * <table border="1">
 *   <caption>Lifecycle state transitions for SavingsGoal</caption>
 *   <tr>
 *     <th>Current State</th>
 *     <th>Trigger / Event</th>
 *     <th>Guard / Condition</th>
 *     <th>Next State</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>{@code [*] (None)}</td>
 *     <td>Goal created</td>
 *     <td>{@code targetAmount > 0}</td>
 *     <td>{@link GoalStatus#ACTIVE}</td>
 *     <td>Goal initialized with target amount; contributions list empty.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link GoalStatus#ACTIVE}</td>
 *     <td>Contribution added/modified</td>
 *     <td>{@code currentAmount < targetAmount}</td>
 *     <td>{@link GoalStatus#ACTIVE}</td>
 *     <td>Total contributions remain below target; goal remains active/in-progress.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link GoalStatus#ACTIVE}</td>
 *     <td>Contribution added/modified</td>
 *     <td>{@code currentAmount >= targetAmount}</td>
 *     <td>{@link GoalStatus#ACHIEVED}</td>
 *     <td>Total contributions reach or exceed target; goal becomes achieved (TC-FR12-01).</td>
 *   </tr>
 *   <tr>
 *     <td>{@link GoalStatus#ACHIEVED}</td>
 *     <td>Contribution edited/removed</td>
 *     <td>{@code currentAmount < targetAmount}</td>
 *     <td>{@link GoalStatus#ACTIVE}</td>
 *     <td>Total contributions drop below target; goal reverts back to active.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link GoalStatus#ACHIEVED}</td>
 *     <td>Contribution added</td>
 *     <td>{@code currentAmount >= targetAmount}</td>
 *     <td>{@link GoalStatus#ACHIEVED}</td>
 *     <td>Additional contributions keep goal achieved (progress &gt; 100%).</td>
 *   </tr>
 * </table>
 *
 * <p><strong>Decision Q-008 Note:</strong> Preliminary drafts proposed 'Overdue' and 'Cancelled'
 * states. Per human decision Q-008 and Decision D-35, those states are deferred to post-v1.0.
 * Version 1.0 models {@code ACTIVE <--> ACHIEVED} transitions only.</p>
 */
public final class SavingsGoal {

    private final Long id;
    private final String name;
    private final Money targetAmount;
    private final LocalDate deadline;
    private final List<GoalContribution> contributions = new ArrayList<>();

    /**
     * Constructs a SavingsGoal with full persistent attributes.
     *
     * @param id persistent goal identifier, or null if unpersisted
     * @param name goal display name
     * @param targetAmount target savings amount (must be strictly positive)
     * @param deadline optional completion deadline date
     * @throws NullPointerException if name or targetAmount is null
     * @throws IllegalArgumentException if name is blank or targetAmount is non-positive (D-13)
     */
    public SavingsGoal(Long id, String name, Money targetAmount, LocalDate deadline) {
        this.id = id;
        this.name = validateName(name);
        this.targetAmount = Objects.requireNonNull(targetAmount, "targetAmount cannot be null");
        if (targetAmount.isZero()) {
            throw new IllegalArgumentException("Target amount must be strictly positive");
        }
        this.deadline = deadline;
    }

    /**
     * Constructs an unpersisted SavingsGoal with an optional deadline.
     *
     * @param name goal display name
     * @param targetAmount target savings amount
     * @param deadline optional completion deadline
     */
    public SavingsGoal(String name, Money targetAmount, LocalDate deadline) {
        this(null, name, targetAmount, deadline);
    }

    /**
     * Constructs an unpersisted SavingsGoal without a deadline.
     *
     * @param name goal display name
     * @param targetAmount target savings amount
     */
    public SavingsGoal(String name, Money targetAmount) {
        this(null, name, targetAmount, null);
    }

    /**
     * Adds a contribution towards this goal.
     *
     * @param contribution contribution to add
     * @throws NullPointerException if contribution is null
     */
    public void addContribution(GoalContribution contribution) {
        Objects.requireNonNull(contribution, "contribution cannot be null");
        contributions.add(contribution);
    }

    /**
     * Adds a general contribution towards this goal.
     *
     * @param contribution contribution to add
     * @throws NullPointerException if contribution is null
     */
    public void addContribution(Contribution contribution) {
        Objects.requireNonNull(contribution, "contribution cannot be null");
        addContribution(contribution.toGoalContribution());
    }

    /**
     * Convenience method to add a contribution by date and amount.
     *
     * @param date date of the contribution
     * @param amount positive money amount
     */
    public void addContribution(LocalDate date, Money amount) {
        addContribution(new GoalContribution(date, amount));
    }

    /**
     * Removes an existing contribution from this goal.
     *
     * @param contribution contribution to remove
     * @return true if the contribution was present and removed
     */
    public boolean removeContribution(GoalContribution contribution) {
        return contributions.remove(contribution);
    }

    /**
     * Calculates the total accumulated contributions towards this goal.
     *
     * @return current saved amount as {@link Money}
     */
    public Money currentAmount() {
        long totalMinor = 0;
        for (GoalContribution c : contributions) {
            totalMinor += c.amount().minorUnits();
        }
        return Money.ofMinor(totalMinor);
    }

    /**
     * Returns the lifecycle status based on current accumulated contributions (FR-12, D-35).
     *
     * @return {@link GoalStatus#ACHIEVED} if currentAmount &gt;= targetAmount, else {@link GoalStatus#ACTIVE}
     */
    public GoalStatus status() {
        return (currentAmount().minorUnits() >= targetAmount.minorUnits())
                ? GoalStatus.ACHIEVED
                : GoalStatus.ACTIVE;
    }

    /**
     * Checks if this savings goal has reached or exceeded its target amount.
     *
     * @return true if achieved, false otherwise
     */
    public boolean isAchieved() {
        return status() == GoalStatus.ACHIEVED;
    }

    /**
     * Calculates the savings progress percentage, rounded to two decimal places (FR-12).
     *
     * @return progress percentage (e.g. 0.0, 99.99, 100.0, 110.0)
     */
    public double progress() {
        if (targetAmount.isZero()) {
            return 0.0;
        }
        long currentMinor = currentAmount().minorUnits();
        long targetMinor = targetAmount.minorUnits();
        double pct = (currentMinor * 100.0) / targetMinor;
        return Math.round(pct * 100.0) / 100.0;
    }

    /**
     * Returns the progress percentage (alias for {@link #progress()}).
     *
     * @return progress percentage
     */
    public double progressPercentage() {
        return progress();
    }

    /**
     * Calculates the dimensionless progress ratio (FR-12).
     *
     * @return ratio (e.g. 0.0, 0.9999, 1.0, 1.10)
     */
    public double progressRatio() {
        if (targetAmount.isZero()) {
            return 0.0;
        }
        double ratio = (double) currentAmount().minorUnits() / targetAmount.minorUnits();
        return Math.round(ratio * 10000.0) / 10000.0;
    }

    /**
     * Returns the persistent identifier.
     *
     * @return goal ID, or null if unpersisted
     */
    public Long id() {
        return id;
    }

    /**
     * Returns the goal display name.
     *
     * @return name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the target savings amount.
     *
     * @return target amount
     */
    public Money targetAmount() {
        return targetAmount;
    }

    /**
     * Returns the optional completion deadline.
     *
     * @return deadline, or null if not set
     */
    public LocalDate deadline() {
        return deadline;
    }

    /**
     * Returns an unmodifiable list of all contributions towards this goal.
     *
     * @return contributions list
     */
    public List<GoalContribution> contributions() {
        return Collections.unmodifiableList(contributions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SavingsGoal that)) {
            return false;
        }
        return Objects.equals(id, that.id)
                && name.equals(that.name)
                && targetAmount.equals(that.targetAmount)
                && Objects.equals(deadline, that.deadline)
                && contributions.equals(that.contributions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, targetAmount, deadline, contributions);
    }

    @Override
    public String toString() {
        return "SavingsGoal{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", target=" + targetAmount
                + ", current=" + currentAmount()
                + ", status=" + status()
                + ", progress=" + progress() + "%"
                + '}';
    }

    private static String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Goal name cannot be null or empty");
        }
        return name.trim();
    }
}
