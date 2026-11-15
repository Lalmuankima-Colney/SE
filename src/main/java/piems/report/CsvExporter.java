package piems.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Concrete exporter formatting report results and transaction records as RFC 4180 CSV (D-10, FR-16).
 *
 * <p>Implements the Template Method pattern defined in {@link Exporter}. Escapes fields
 * containing commas, double quotes, or newlines by wrapping them in double quotes and
 * doubling inner quotation marks.</p>
 */
public class CsvExporter extends Exporter {

    /** Standard 7-column header defined in SRS Section 3.3 and Decision D-30. */
    public static final String TRANSACTION_CSV_HEADER = "date,kind,account,to_account,category,amount,note";

    private static final String FORMULA_PREFIXES = "=+-@\t\r'";

    @Override
    protected String formatHeader(ReportResult report) {
        if (report.headers().isEmpty()) {
            return "";
        }
        return formatCsvRow(report.headers()) + "\n";
    }

    @Override
    protected String formatRows(ReportResult report) {
        StringBuilder sb = new StringBuilder();
        for (List<String> row : report.rows()) {
            sb.append(formatCsvRow(row)).append("\n");
        }
        return sb.toString();
    }

    @Override
    protected String formatFooter(ReportResult report) {
        return "";
    }

    /**
     * Serializes a list of transaction rows into the standard 7-column CSV format (FR-16).
     *
     * <p>Neutralizes spreadsheet formula trigger characters on free-text columns
     * (account, toAccount, category, note) by prepending an apostrophe (D-44, P-304).</p>
     *
     * @param rows list of transaction export records
     * @return complete RFC 4180 compliant CSV document string
     */
    public String exportTransactions(List<TransactionExportRow> rows) {
        Objects.requireNonNull(rows, "Transaction rows cannot be null");
        StringBuilder sb = new StringBuilder();
        sb.append(TRANSACTION_CSV_HEADER).append("\n");
        for (TransactionExportRow row : rows) {
            List<String> values = new ArrayList<>();
            values.add(row.date().toString());
            values.add(row.kind());
            values.add(neutralizeFormula(row.account()));
            values.add(neutralizeFormula(row.toAccount()));
            values.add(neutralizeFormula(row.category()));
            values.add(row.amount().toString());
            values.add(neutralizeFormula(row.note()));
            sb.append(formatCsvRow(values)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Neutralizes leading spreadsheet formula trigger characters (=, +, -, @, \t, \r, ')
     * on free-text columns by prepending an apostrophe (D-44, P-304).
     *
     * @param text raw free-text field value
     * @return neutralized string value
     */
    public static String neutralizeFormula(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        if (FORMULA_PREFIXES.indexOf(text.charAt(0)) >= 0) {
            return "'" + text;
        }
        return text;
    }

    /**
     * Serializes a single list of cell values into a comma-separated row string.
     *
     * @param values column values
     * @return comma-separated formatted row without trailing newline
     */
    public static String formatCsvRow(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(escapeCsvField(values.get(i)));
        }
        return sb.toString();
    }

    /**
     * Escapes a single CSV field value according to RFC 4180 rules.
     *
     * @param field raw string value
     * @return escaped string value
     */
    public static String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        boolean needsQuotes = field.contains(",") || field.contains("\"")
                || field.contains("\n") || field.contains("\r");
        if (!needsQuotes) {
            return field;
        }
        return "\"" + field.replace("\"", "\"\"") + "\"";
    }
}
