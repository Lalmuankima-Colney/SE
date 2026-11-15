package piems.report;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import piems.domain.Account;
import piems.domain.BankAccount;
import piems.domain.Category;
import piems.domain.Expense;
import piems.domain.Income;
import piems.domain.Money;
import piems.domain.Transaction;
import piems.domain.Transfer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit and integration tests for {@link MonthlySummaryReport}.
 *
 * <p>Verifies Requirement FR-13, Decision D-10, D-36, and Scenario S1:
 * hand-calculated monthly totals (TC-FR13-01), "N/A" savings rate when income is zero (TC-FR13-02),
 * and exclusion of transfers from income and expenditure.</p>
 */
class MonthlySummaryReportTest {

    private final MonthlySummaryReport report = new MonthlySummaryReport();
    private final Account account = new BankAccount(1L, "Primary Checking", Money.ofMinor(10000000L));
    private final Category food = new Category(10L, "Food", "EXPENSE");
    private final Category rent = new Category(20L, "Rent", "EXPENSE");

    @Test
    @DisplayName("TC-FR13-01 / Scenario S1: Figures match hand-calculated month and exclude transfers")
    void matchesHandCalculatedMonth() {
        YearMonth targetMonth = YearMonth.of(2026, 1);
        ReportRequest request = new ReportRequest(targetMonth);

        Transaction salary = new Income(1L, LocalDate.of(2026, 1, 5), Money.ofMinor(5000000L), "Salary", account, 100L);
        Transaction foodExp = new Expense(2L, LocalDate.of(2026, 1, 10), Money.ofMinor(2000000L), "Groceries", account, food);
        Transaction rentExp = new Expense(3L, LocalDate.of(2026, 1, 15), Money.ofMinor(1000000L), "Apartment", account, rent);
        Transaction transfer = new Transfer(4L, LocalDate.of(2026, 1, 20), Money.ofMinor(500000L), "To Savings", account, "uuid-1");
        Transaction nextMonth = new Income(5L, LocalDate.of(2026, 2, 1), Money.ofMinor(1000000L), "Bonus", account, 100L);

        List<Transaction> transactions = List.of(salary, foodExp, rentExp, transfer, nextMonth);

        ReportResult result = report.generate(request, transactions);

        assertThat(result.title()).isEqualTo("Monthly Summary - 2026-01");
        assertThat(result.getData("totalIncomeMinor")).isEqualTo(5000000L);
        assertThat(result.getData("totalExpenseMinor")).isEqualTo(3000000L);
        assertThat(result.getData("netSavingsMinor")).isEqualTo(2000000L);
        assertThat(result.getData("savingsRate")).isEqualTo("40.0%");

        List<List<String>> rows = result.rows();
        assertThat(rows).containsExactly(
                List.of("Total Income", "50000.00"),
                List.of("Total Expense", "30000.00"),
                List.of("Net Savings", "20000.00"),
                List.of("Savings Rate", "40.0%")
        );
    }

    @Test
    @DisplayName("TC-FR13-02: Zero income displays 'N/A' for savings rate without division by zero")
    void zeroIncomeDisplaysNotApplicableSavingsRate() {
        YearMonth targetMonth = YearMonth.of(2026, 3);
        ReportRequest request = new ReportRequest(targetMonth);

        Transaction exp = new Expense(1L, LocalDate.of(2026, 3, 12), Money.ofMinor(250000L), "Gym", account, food);
        ReportResult result = report.generate(request, List.of(exp));

        assertThat(result.getData("totalIncomeMinor")).isEqualTo(0L);
        assertThat(result.getData("totalExpenseMinor")).isEqualTo(250000L);
        assertThat(result.getData("netSavingsMinor")).isEqualTo(-250000L);
        assertThat(result.getData("savingsRate")).isEqualTo("N/A");

        List<List<String>> rows = result.rows();
        assertThat(rows.get(2)).containsExactly("Net Savings", "-2500.00");
        assertThat(rows.get(3)).containsExactly("Savings Rate", "N/A");
    }

    @Test
    @DisplayName("Empty month produces zero income and expense with N/A savings rate")
    void emptyMonthProducesZeroTotals() {
        ReportRequest request = new ReportRequest(YearMonth.of(2026, 4));
        ReportResult result = report.generate(request, List.of());

        assertThat(result.getData("totalIncomeMinor")).isEqualTo(0L);
        assertThat(result.getData("totalExpenseMinor")).isEqualTo(0L);
        assertThat(result.getData("netSavingsMinor")).isEqualTo(0L);
        assertThat(result.getData("savingsRate")).isEqualTo("N/A");
    }

    @Test
    @DisplayName("Deficit month produces negative net savings and negative savings rate")
    void deficitMonthProducesNegativeSavingsRate() {
        YearMonth targetMonth = YearMonth.of(2026, 5);
        ReportRequest request = new ReportRequest(targetMonth);

        Transaction inc = new Income(1L, LocalDate.of(2026, 5, 1), Money.ofMinor(1000000L), "Stipend", account, 100L);
        Transaction exp = new Expense(2L, LocalDate.of(2026, 5, 10), Money.ofMinor(1500000L), "Tuition", account, rent);

        ReportResult result = report.generate(request, List.of(inc, exp));
        assertThat(result.getData("netSavingsMinor")).isEqualTo(-500000L);
        assertThat(result.getData("savingsRate")).isEqualTo("-50.0%");
    }
}
