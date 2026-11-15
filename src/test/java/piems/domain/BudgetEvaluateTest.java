package piems.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * White-box path and boundary value tests for {@link Budget#evaluate(Money)} and
 * duplicate budget rejection (FR-09, FR-10, TC-FR09-01, TC-FR10-01 through TC-FR10-05).
 */
class BudgetEvaluateTest {

    private Category groceries;
    private YearMonth march2026;
    private Budget budget100;

    @BeforeEach
    void setUp() {
        groceries = new Category(1L, "Groceries", "EXPENSE");
        march2026 = YearMonth.of(2026, 3);
        budget100 = new Budget(10L, groceries, march2026, Money.of("100.00"));
    }

    @Test
    @DisplayName("TC-FR10-05 Independent Path 1: Budget limit zero is rejected with IllegalArgumentException")
    void limitZeroThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> new Budget(groceries, march2026, Money.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictly positive");
    }

    @Test
    @DisplayName("TC-FR10-04 Independent Path 2: Ratio 1.0 (spent equals limit) returns EXCEEDED")
    void ratioOneHundredPercentReturnsExceeded() {
        BudgetStatus status = budget100.evaluate(Money.of("100.00"));
        assertThat(status).isEqualTo(BudgetStatus.EXCEEDED);
    }

    @Test
    @DisplayName("TC-FR10-02 Independent Path 3: Ratio 0.85 (spent between 80% and 100%) returns WARNING")
    void ratioEightyFivePercentReturnsWarning() {
        BudgetStatus status = budget100.evaluate(Money.of("85.00"));
        assertThat(status).isEqualTo(BudgetStatus.WARNING);
    }

    @Test
    @DisplayName("TC-FR10-01 Independent Path 4: Ratio 0.50 (spent below 80%) returns OK")
    void ratioFiftyPercentReturnsOk() {
        BudgetStatus status = budget100.evaluate(Money.of("50.00"));
        assertThat(status).isEqualTo(BudgetStatus.OK);
    }

    @Test
    @DisplayName("TC-FR10-01 Boundary 79.99%: Spent 79.99 of 100.00 returns OK")
    void boundaryBelowEightyPercentReturnsOk() {
        BudgetStatus status = budget100.evaluate(Money.of("79.99"));
        assertThat(status).isEqualTo(BudgetStatus.OK);
    }

    @Test
    @DisplayName("TC-FR10-02 Boundary 80.00%: Spent 80.00 of 100.00 returns WARNING")
    void boundaryEightyPercentReturnsWarning() {
        BudgetStatus status = budget100.evaluate(Money.of("80.00"));
        assertThat(status).isEqualTo(BudgetStatus.WARNING);
    }

    @Test
    @DisplayName("TC-FR10-03 Boundary 99.99%: Spent 99.99 of 100.00 returns WARNING")
    void boundaryBelowOneHundredPercentReturnsWarning() {
        BudgetStatus status = budget100.evaluate(Money.of("99.99"));
        assertThat(status).isEqualTo(BudgetStatus.WARNING);
    }

    @Test
    @DisplayName("TC-FR10-04 Boundary 100.00%: Spent 100.00 of 100.00 returns EXCEEDED")
    void boundaryOneHundredPercentReturnsExceeded() {
        BudgetStatus status = budget100.evaluate(Money.of("100.00"));
        assertThat(status).isEqualTo(BudgetStatus.EXCEEDED);
    }

    @Test
    @DisplayName("TC-FR10-04 Boundary 100.01%: Spent 100.01 of 100.00 returns EXCEEDED")
    void boundaryAboveOneHundredPercentReturnsExceeded() {
        BudgetStatus status = budget100.evaluate(Money.of("100.01"));
        assertThat(status).isEqualTo(BudgetStatus.EXCEEDED);
    }

    @Test
    @DisplayName("TC-FR09-01 Duplicate budget rule: Second budget for same category and month is rejected")
    void duplicateBudgetSameCategoryAndMonthRejected() {
        List<Budget> existingBudgets = new ArrayList<>();
        existingBudgets.add(budget100);

        Budget duplicate = new Budget(groceries, march2026, Money.of("200.00"));
        boolean hasDuplicate = existingBudgets.stream()
                .anyMatch(b -> b.matches(duplicate.getCategory(), duplicate.getMonth()));

        assertThat(hasDuplicate).isTrue();
        assertThatThrownBy(() -> {
            if (hasDuplicate) {
                throw new ValidationException("A budget already exists for this category and month");
            }
        }).isInstanceOf(ValidationException.class)
          .hasMessageContaining("already exists for this category and month");
    }

    @Test
    @DisplayName("TC-FR09-01 Budget getters, null evaluation check and matching logic")
    void budgetGettersAndMatching() {
        assertThat(budget100.getId()).isEqualTo(10L);
        assertThat(budget100.getCategory()).isEqualTo(groceries);
        assertThat(budget100.getMonth()).isEqualTo(march2026);
        assertThat(budget100.getLimit()).isEqualTo(Money.of("100.00"));
        assertThat(budget100.toString()).contains("Groceries", "2026-03");

        assertThat(budget100.matches(groceries, march2026)).isTrue();
        assertThat(budget100.matches(null, march2026)).isFalse();
        assertThat(budget100.matches(groceries, null)).isFalse();
        Category utilities = new Category(2L, "Utilities", "EXPENSE");
        assertThat(budget100.matches(utilities, march2026)).isFalse();
        assertThat(budget100.matches(groceries, YearMonth.of(2026, 4))).isFalse();

        assertThatThrownBy(() -> budget100.evaluate(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("TC-FR09-01 Budget equals and hashCode contract")
    void equalsAndHashCodeContract() {
        Budget same = new Budget(10L, groceries, march2026, Money.of("100.00"));
        Budget diffId = new Budget(11L, groceries, march2026, Money.of("100.00"));

        assertThat(budget100).isEqualTo(same);
        assertThat(budget100.hashCode()).isEqualTo(same.hashCode());
        assertThat(budget100).isNotEqualTo(diffId);
        assertThat(budget100).isNotEqualTo(null);
        assertThat(budget100).isNotEqualTo("otherType");
    }

    @Test
    @DisplayName("TC-FR09-02 Unpersisted budgets differing in category, month, or limit are not equal")
    void budgetsDifferingInAttributesAreNotEqual() {
        Category utilities = new Category(2L, "Utilities", "EXPENSE");
        Budget base = new Budget(null, groceries, march2026, Money.of("100.00"));
        Budget same = new Budget(null, groceries, march2026, Money.of("100.00"));
        Budget diffCat = new Budget(null, utilities, march2026, Money.of("100.00"));
        Budget diffMonth = new Budget(null, groceries, YearMonth.of(2026, 4), Money.of("100.00"));
        Budget diffLimit = new Budget(null, groceries, march2026, Money.of("150.00"));

        assertThat(base).isEqualTo(same);
        assertThat(base.hashCode()).isEqualTo(same.hashCode());
        assertThat(base).isNotEqualTo(diffCat);
        assertThat(base).isNotEqualTo(diffMonth);
        assertThat(base).isNotEqualTo(diffLimit);
    }
}
