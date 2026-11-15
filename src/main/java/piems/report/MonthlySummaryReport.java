package piems.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import piems.domain.Money;
import piems.domain.Transaction;

/**
 * Strategy generating a monthly financial summary report.
 *
 * <p>Implements Requirement FR-13 and Decision D-10, D-36 (Q-013). Computes total income,
 * total expenditure, net savings, and savings percentage. Excludes transfers. Displays
 * "N/A" for savings rate when monthly income is zero.</p>
 */
public class MonthlySummaryReport implements ReportStrategy {

    private static final String STRATEGY_NAME = "Monthly Summary";

    @Override
    public String name() {
        return STRATEGY_NAME;
    }

    @Override
    public ReportResult generate(ReportRequest request, List<Transaction> transactions) {
        Objects.requireNonNull(request, "Request cannot be null");
        Objects.requireNonNull(transactions, "Transactions cannot be null");

        long incomeMinor = 0L;
        long expenseMinor = 0L;
        for (Transaction tx : transactions) {
            if (tx != null && request.matchesMonth(tx.date())) {
                if ("INCOME".equals(tx.kind())) {
                    incomeMinor += tx.amount().minorUnits();
                } else if ("EXPENSE".equals(tx.kind())) {
                    expenseMinor += tx.amount().minorUnits();
                }
            }
        }
        return buildResult(request, incomeMinor, expenseMinor);
    }

    private ReportResult buildResult(ReportRequest request, long incomeMinor, long expenseMinor) {
        long netMinor = incomeMinor - expenseMinor;
        String netFormatted = formatSignedMoney(netMinor);
        String savingsRateStr = calculateSavingsRate(incomeMinor, netMinor);

        List<String> headers = List.of("Metric", "Amount");
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Total Income", Money.ofMinor(incomeMinor).toString()));
        rows.add(List.of("Total Expense", Money.ofMinor(expenseMinor).toString()));
        rows.add(List.of("Net Savings", netFormatted));
        rows.add(List.of("Savings Rate", savingsRateStr));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalIncomeMinor", incomeMinor);
        data.put("totalExpenseMinor", expenseMinor);
        data.put("netSavingsMinor", netMinor);
        data.put("savingsRate", savingsRateStr);

        return new ReportResult("Monthly Summary - " + request.month(), headers, rows, data);
    }

    private String calculateSavingsRate(long incomeMinor, long netMinor) {
        if (incomeMinor == 0L) {
            return "N/A";
        }
        long tenths = (netMinor * 1000L) / incomeMinor;
        long whole = tenths / 10L;
        long fraction = Math.abs(tenths % 10L);
        return whole + "." + fraction + "%";
    }

    private String formatSignedMoney(long minorUnits) {
        if (minorUnits >= 0L) {
            return Money.ofMinor(minorUnits).toString();
        }
        return "-" + Money.ofMinor(-minorUnits).toString();
    }
}
