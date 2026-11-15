package piems.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SavingsGoal}, {@link GoalContribution}, and progress calculations
 * (FR-12, UC-09, Q-008, D-35, TC-FR12-01).
 */
class SavingsGoalTest {

    private final LocalDate today = LocalDate.of(2026, 6, 1);
    private final LocalDate deadline = LocalDate.of(2026, 12, 31);

    @Test
    @DisplayName("TC-FR12-01: Status transitions from Active to Achieved when contributions reach target")
    void becomesAchievedWhenContributionsReachTarget() {
        SavingsGoal goal = new SavingsGoal(1L, "Emergency Fund", Money.of("1000.00"), deadline);

        assertThat(goal.status()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(goal.isAchieved()).isFalse();
        assertThat(goal.currentAmount()).isEqualTo(Money.zero());
        assertThat(goal.progress()).isEqualTo(0.0);

        goal.addContribution(new GoalContribution(today, Money.of("600.00")));
        assertThat(goal.status()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(goal.isAchieved()).isFalse();
        assertThat(goal.currentAmount()).isEqualTo(Money.of("600.00"));
        assertThat(goal.progress()).isEqualTo(60.0);

        goal.addContribution(new Contribution(today.plusDays(5), Money.of("400.00")));
        assertThat(goal.status()).isEqualTo(GoalStatus.ACHIEVED);
        assertThat(goal.isAchieved()).isTrue();
        assertThat(goal.currentAmount()).isEqualTo(Money.of("1000.00"));
        assertThat(goal.progress()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("TC-FR12-01 Boundary 0.00%: No contributions yields 0% progress and Active status")
    void progressBoundaryZeroPercent() {
        SavingsGoal goal = new SavingsGoal("New Car", Money.of("100.00"));
        assertThat(goal.progress()).isEqualTo(0.0);
        assertThat(goal.progressPercentage()).isEqualTo(0.0);
        assertThat(goal.progressRatio()).isEqualTo(0.0);
        assertThat(goal.status()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(goal.isAchieved()).isFalse();
    }

    @Test
    @DisplayName("TC-FR12-01 Boundary 99.99%: 99.99 of 100.00 yields 99.99% progress and Active status")
    void progressBoundaryNinetyNinePointNinetyNinePercent() {
        SavingsGoal goal = new SavingsGoal("Vacation", Money.of("100.00"));
        goal.addContribution(today, Money.of("99.99"));

        assertThat(goal.progress()).isEqualTo(99.99);
        assertThat(goal.progressRatio()).isEqualTo(0.9999);
        assertThat(goal.status()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(goal.isAchieved()).isFalse();
    }

    @Test
    @DisplayName("TC-FR12-01 Boundary 100.00%: 100.00 of 100.00 yields 100.0% progress and Achieved status")
    void progressBoundaryOneHundredPercent() {
        SavingsGoal goal = new SavingsGoal("Laptop", Money.of("100.00"));
        goal.addContribution(today, Money.of("100.00"));

        assertThat(goal.progress()).isEqualTo(100.0);
        assertThat(goal.progressRatio()).isEqualTo(1.0);
        assertThat(goal.status()).isEqualTo(GoalStatus.ACHIEVED);
        assertThat(goal.isAchieved()).isTrue();
    }

    @Test
    @DisplayName("TC-FR12-01 Boundary 110.00%: Over 100% yields >100% progress and Achieved status")
    void progressBoundaryAboveOneHundredPercent() {
        SavingsGoal goal = new SavingsGoal("Retirement", Money.of("100.00"));
        goal.addContribution(today, Money.of("110.00"));

        assertThat(goal.progress()).isEqualTo(110.0);
        assertThat(goal.progressRatio()).isEqualTo(1.1);
        assertThat(goal.status()).isEqualTo(GoalStatus.ACHIEVED);
        assertThat(goal.isAchieved()).isTrue();
    }

    @Test
    @DisplayName("TC-FR12-01: Removing contribution below target reverts status from Achieved back to Active")
    void achievedGoalRevertsToActiveWhenContributionRemoved() {
        SavingsGoal goal = new SavingsGoal("House", Money.of("100.00"));
        GoalContribution contribution = new GoalContribution(today, Money.of("100.00"));
        goal.addContribution(contribution);
        assertThat(goal.status()).isEqualTo(GoalStatus.ACHIEVED);

        boolean removed = goal.removeContribution(contribution);
        assertThat(removed).isTrue();
        assertThat(goal.currentAmount()).isEqualTo(Money.zero());
        assertThat(goal.status()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(goal.isAchieved()).isFalse();
    }

    @Test
    @DisplayName("Decision D-13: Target amount zero or negative is rejected with IllegalArgumentException")
    void targetAmountMustBeStrictlyPositive() {
        assertThatThrownBy(() -> new SavingsGoal("Invalid", Money.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictly positive");
    }

    @Test
    @DisplayName("Decision D-13: Contribution amount zero is rejected with IllegalArgumentException")
    void contributionAmountMustBeStrictlyPositive() {
        assertThatThrownBy(() -> new GoalContribution(today, Money.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictly positive");

        assertThatThrownBy(() -> new Contribution(today, Money.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictly positive");
    }

    @Test
    @DisplayName("Goal name cannot be null or blank")
    void goalNameValidation() {
        assertThatThrownBy(() -> new SavingsGoal(null, Money.of("100.00")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SavingsGoal("   ", Money.of("100.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("contributions list provides defensive unmodifiable view")
    void defensiveCopyingOfContributions() {
        SavingsGoal goal = new SavingsGoal("College", Money.of("500.00"));
        GoalContribution c = new GoalContribution(today, Money.of("50.00"));
        goal.addContribution(c);

        assertThatThrownBy(() -> goal.contributions().add(new GoalContribution(today, Money.of("20.00"))))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("SavingsGoal equals, hashCode, and toString contracts")
    void equalsAndHashCodeContract() {
        SavingsGoal g1 = new SavingsGoal(1L, "Bike", Money.of("500.00"), deadline);
        SavingsGoal g2 = new SavingsGoal(1L, "Bike", Money.of("500.00"), deadline);
        SavingsGoal g3 = new SavingsGoal(2L, "Bike", Money.of("500.00"), deadline);

        assertThat(g1).isEqualTo(g2);
        assertThat(g1.hashCode()).isEqualTo(g2.hashCode());
        assertThat(g1).isNotEqualTo(g3);
        assertThat(g1).isNotEqualTo(null);
        assertThat(g1).isNotEqualTo("otherObject");
        assertThat(g1.toString()).contains("Bike", "500.00", "ACTIVE");
        assertThat(g1.id()).isEqualTo(1L);
        assertThat(g1.name()).isEqualTo("Bike");
        assertThat(g1.targetAmount()).isEqualTo(Money.of("500.00"));
        assertThat(g1.deadline()).isEqualTo(deadline);
    }

    @Test
    @DisplayName("TC-FR12-02 Unpersisted goals with identical attributes are equal")
    void goalsWithIdenticalAttributesAreEqual() {
        SavingsGoal g1 = new SavingsGoal(null, "Laptop", Money.of("1200.00"), deadline);
        SavingsGoal g2 = new SavingsGoal(null, "Laptop", Money.of("1200.00"), deadline);
        assertThat(g1).isEqualTo(g2);
        assertThat(g1.hashCode()).isEqualTo(g2.hashCode());
    }

    @Test
    @DisplayName("TC-FR12-03 Unpersisted goals differing in any attribute are not equal")
    void goalsDifferingInAnyAttributeAreNotEqual() {
        SavingsGoal base = new SavingsGoal(null, "Laptop", Money.of("1200.00"), deadline);
        assertThat(base).isNotEqualTo(new SavingsGoal(null, "Phone", Money.of("1200.00"), deadline));
        assertThat(base).isNotEqualTo(new SavingsGoal(null, "Laptop", Money.of("1500.00"), deadline));
        assertThat(base).isNotEqualTo(new SavingsGoal(null, "Laptop", Money.of("1200.00"), deadline.plusDays(1)));
    }

    @Test
    @DisplayName("TC-FR12-04 Constructor rejects zero target amount")
    void goalProgressReturnsZeroWhenTargetIsZero() {
        assertThatThrownBy(() -> new SavingsGoal(null, "Car", Money.zero(), deadline))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target amount must be strictly positive");
    }
}
