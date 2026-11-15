package piems.report;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import piems.domain.Money;
import piems.domain.Transaction;

/**
 * Strategy generating a multi-month cash-flow trend report.
 *
 * <p>Implements Requirement FR-15 and Decision D-10. Produces one row per calendar month
 * within the requested range, including inactive months with zero transactions.</p>
 */
public class CashFlowReport implements ReportStrategy {

    private static final String STRATEGY_NAME = "Cash-Flow Trend";

    @Override
    public String name() {
        return STRATEGY_NAME;
    }

    @Override
    public ReportResult generate(ReportRequest request, List<Transaction> transactions) {
        Objects.requireNonNull(request, "Request cannot be null");
        Objects.requireNonNull(transactions, "Transactions cannot be null");

        List<List<String>> rows = new ArrayList<>();
        Map<String, Object> data = new LinkedHashMap<>();
        List<String> headers = List.of("Month", "Income", "Expense", "Net Cash Flow");

        YearMonth current = request.startMonth();
        while (!current.isAfter(request.endMonth())) {
            generateMonthRow(current, transactions, rows, data);
            current = current.plusMonths(1);
        }

        String title = "Cash-Flow Trend - " + request.startMonth() + " to " + request.endMonth();
        return new ReportResult(title, headers, rows, data);
    }

    private void generateMonthRow(
            YearMonth month,
            List<Transaction> transactions,
            List<List<String>> rows,
            Map<String, Object> data) {
        long incomeMinor = 0L;
        long expenseMinor = 0L;

        for (Transaction tx : transactions) {
            if (tx != null && YearMonth.from(tx.date()).equals(month)) {
                if ("INCOME".equals(tx.kind())) {
                    incomeMinor += tx.amount().minorUnits();
                } else if ("EXPENSE".equals(tx.kind())) {
                    expenseMinor += tx.amount().minorUnits();
                }
            }
        }

        long netMinor = incomeMinor - expenseMinor;
        String netStr = formatSignedMoney(netMinor);

        rows.add(List.of(
                month.toString(),
                Money.ofMinor(incomeMinor).toString(),
                Money.ofMinor(expenseMinor).toString(),
                netStr));

        data.put(month.toString() + ".income", incomeMinor);
        data.put(month.toString() + ".expense", expenseMinor);
        data.put(month.toString() + ".net", netMinor);
    }

    private String formatSignedMoney(long minorUnits) {
        if (minorUnits >= 0L) {
            return Money.ofMinor(minorUnits).toString();
        }
        return "-" + Money.ofMinor(-minorUnits).toString();
    }
}
