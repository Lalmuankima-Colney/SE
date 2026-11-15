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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit and integration tests for {@link CashFlowReport}.
 *
 * <p>Verifies Requirement FR-15 and Decision D-10: multi-month cash-flow trend (TC-FR15-01),
 * generation of exactly one row per month in the range including empty months,
 * and range boundary validation.</p>
 */
class CashFlowReportTest {

    private final CashFlowReport report = new CashFlowReport();
    private final Account account = new BankAccount(1L, "Primary Bank", Money.ofMinor(5000000L));
    private final Category food = new Category(1L, "Food", "EXPENSE");

    @Test
    @DisplayName("TC-FR15-01: Six-month range produces exactly six rows including empty months")
    void sixMonthsProducesSixRowsIncludingEmptyMonths() {
        YearMonth start = YearMonth.of(2026, 1);
        YearMonth end = YearMonth.of(2026, 6);
        ReportRequest request = new ReportRequest(start, end);

        Transaction janInc = new Income(1L, LocalDate.of(2026, 1, 5), Money.ofMinor(1000000L), "Salary", account, 100L);
        Transaction janExp = new Expense(2L, LocalDate.of(2026, 1, 10), Money.ofMinor(400000L), "Food", account, food);
        Transaction marExp = new Expense(3L, LocalDate.of(2026, 3, 15), Money.ofMinor(300000L), "Food", account, food);
        Transaction mayInc = new Income(4L, LocalDate.of(2026, 5, 20), Money.ofMinor(1500000L), "Bonus", account, 100L);

        ReportResult result = report.generate(request, List.of(janInc, janExp, marExp, mayInc));

        assertThat(result.headers()).containsExactly("Month", "Income", "Expense", "Net Cash Flow");
        List<List<String>> rows = result.rows();
        assertThat(rows).hasSize(6);

        assertThat(rows.get(0)).containsExactly("2026-01", "10000.00", "4000.00", "6000.00");
        assertThat(rows.get(1)).containsExactly("2026-02", "0.00", "0.00", "0.00");
        assertThat(rows.get(2)).containsExactly("2026-03", "0.00", "3000.00", "-3000.00");
        assertThat(rows.get(3)).containsExactly("2026-04", "0.00", "0.00", "0.00");
        assertThat(rows.get(4)).containsExactly("2026-05", "15000.00", "0.00", "15000.00");
        assertThat(rows.get(5)).containsExactly("2026-06", "0.00", "0.00", "0.00");
    }

    @Test
    @DisplayName("Single-month range produces exactly one row")
    void singleMonthRangeProducesOneRow() {
        YearMonth month = YearMonth.of(2026, 9);
        ReportRequest request = new ReportRequest(month, month);

        Transaction inc = new Income(1L, LocalDate.of(2026, 9, 1), Money.ofMinor(500000L), "Freelance", account, 100L);
        ReportResult result = report.generate(request, List.of(inc));

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0)).containsExactly("2026-09", "5000.00", "0.00", "5000.00");
    }

    @Test
    @DisplayName("Range validation rejects start month after end month")
    void rangeValidationRejectsStartAfterEnd() {
        YearMonth start = YearMonth.of(2026, 12);
        YearMonth end = YearMonth.of(2026, 1);

        assertThrows(IllegalArgumentException.class, () -> new ReportRequest(start, end));
    }
}
