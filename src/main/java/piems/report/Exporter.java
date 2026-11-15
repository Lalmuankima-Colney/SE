package piems.report;

import java.util.Objects;

/**
 * Abstract base class providing a template method for report and data export (D-10, FR-16).
 *
 * <p>Defines the invariant skeleton for exporting report data (header, body rows, footer)
 * while delegating concrete serialization and formatting to subclasses such as
 * {@link CsvExporter} and {@link TextExporter}.</p>
 */
public abstract class Exporter {

    /**
     * Executes the export algorithm following the Template Method pattern.
     *
     * @param report structured report result to export
     * @return formatted report representation
     */
    public final String export(ReportResult report) {
        Objects.requireNonNull(report, "ReportResult cannot be null");
        StringBuilder sb = new StringBuilder();
        String header = formatHeader(report);
        if (header != null && !header.isEmpty()) {
            sb.append(header);
        }
        String rows = formatRows(report);
        if (rows != null && !rows.isEmpty()) {
            sb.append(rows);
        }
        String footer = formatFooter(report);
        if (footer != null && !footer.isEmpty()) {
            sb.append(footer);
        }
        return sb.toString();
    }

    /**
     * Formats the report header section.
     *
     * @param report structured report result
     * @return formatted header string
     */
    protected abstract String formatHeader(ReportResult report);

    /**
     * Formats the report tabular rows section.
     *
     * @param report structured report result
     * @return formatted rows string
     */
    protected abstract String formatRows(ReportResult report);

    /**
     * Formats the report footer section.
     *
     * @param report structured report result
     * @return formatted footer string
     */
    protected abstract String formatFooter(ReportResult report);
}
