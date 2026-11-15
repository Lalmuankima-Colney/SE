package piems.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Concrete exporter formatting report results and transaction records as an aligned text table (D-10).
 *
 * <p>Implements the Template Method pattern defined in {@link Exporter}. Computes column
 * widths dynamically to align cells cleanly with borders and dividers.</p>
 */
public class TextExporter extends Exporter {

    @Override
    protected String formatHeader(ReportResult report) {
        StringBuilder sb = new StringBuilder();
        if (report.title() != null && !report.title().isBlank()) {
            sb.append(report.title()).append("\n");
        }
        if (report.headers().isEmpty()) {
            return sb.toString();
        }
        List<Integer> widths = computeColumnWidths(report.headers(), report.rows());
        String divider = buildDivider(widths);
        sb.append(divider).append("\n");
        sb.append(formatPaddedRow(report.headers(), widths)).append("\n");
        sb.append(divider).append("\n");
        return sb.toString();
    }

    @Override
    protected String formatRows(ReportResult report) {
        if (report.rows().isEmpty()) {
            return "";
        }
        List<Integer> widths = computeColumnWidths(report.headers(), report.rows());
        StringBuilder sb = new StringBuilder();
        for (List<String> row : report.rows()) {
            sb.append(formatPaddedRow(row, widths)).append("\n");
        }
        return sb.toString();
    }

    @Override
    protected String formatFooter(ReportResult report) {
        if (report.headers().isEmpty() && report.rows().isEmpty()) {
            return "";
        }
        List<Integer> widths = computeColumnWidths(report.headers(), report.rows());
        return buildDivider(widths) + "\n";
    }

    /**
     * Serializes a list of transaction rows into an aligned tabular text representation.
     *
     * @param rows list of transaction export records
     * @return formatted plain text table string
     */
    public String exportTransactions(List<TransactionExportRow> rows) {
        Objects.requireNonNull(rows, "Transaction rows cannot be null");
        List<String> headers = List.of("Date", "Kind", "Account", "To Account", "Category", "Amount", "Note");
        List<List<String>> dataRows = new ArrayList<>();
        for (TransactionExportRow row : rows) {
            dataRows.add(List.of(
                    row.date().toString(),
                    row.kind(),
                    row.account(),
                    row.toAccount(),
                    row.category(),
                    row.amount().toString(),
                    row.note()
            ));
        }
        ReportResult report = new ReportResult("Transactions", headers, dataRows, java.util.Map.of());
        return export(report);
    }

    private List<Integer> computeColumnWidths(List<String> headers, List<List<String>> rows) {
        int colCount = headers.size();
        for (List<String> row : rows) {
            colCount = Math.max(colCount, row.size());
        }
        List<Integer> widths = new ArrayList<>();
        for (int c = 0; c < colCount; c++) {
            int max = (c < headers.size()) ? headers.get(c).length() : 0;
            for (List<String> row : rows) {
                if (c < row.size() && row.get(c) != null) {
                    max = Math.max(max, row.get(c).length());
                }
            }
            widths.add(Math.max(max, 3));
        }
        return widths;
    }

    private String formatPaddedRow(List<String> cells, List<Integer> widths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < widths.size(); i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            String cell = (i < cells.size() && cells.get(i) != null) ? cells.get(i) : "";
            int width = widths.get(i);
            sb.append(cell).append(repeatChar(' ', Math.max(0, width - cell.length())));
        }
        return sb.toString();
    }

    private String buildDivider(List<Integer> widths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < widths.size(); i++) {
            if (i > 0) {
                sb.append("-+-");
            }
            sb.append(repeatChar('-', widths.get(i)));
        }
        return sb.toString();
    }

    private String repeatChar(char ch, int count) {
        if (count <= 0) {
            return "";
        }
        return String.valueOf(ch).repeat(count);
    }
}
