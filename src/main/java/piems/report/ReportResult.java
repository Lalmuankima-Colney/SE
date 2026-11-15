package piems.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Structured report output container produced by {@link ReportStrategy}.
 *
 * <p>Holds report metadata, tabular headers, textual presentation rows, and
 * raw programmatic calculation data for downstream exporters and UI consumers.</p>
 */
public final class ReportResult {

    private final String title;
    private final List<String> headers;
    private final List<List<String>> rows;
    private final Map<String, Object> data;

    /**
     * Constructs a complete ReportResult instance.
     *
     * @param title   human-readable report title
     * @param headers column heading labels
     * @param rows    tabular rows of formatted text values
     * @param data    key-value map of underlying quantitative metrics
     */
    public ReportResult(
            String title,
            List<String> headers,
            List<List<String>> rows,
            Map<String, Object> data) {
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        Objects.requireNonNull(headers, "Headers cannot be null");
        Objects.requireNonNull(rows, "Rows cannot be null");
        Objects.requireNonNull(data, "Data map cannot be null");

        this.headers = Collections.unmodifiableList(new ArrayList<>(headers));
        List<List<String>> rowCopy = new ArrayList<>();
        for (List<String> row : rows) {
            rowCopy.add(Collections.unmodifiableList(new ArrayList<>(row)));
        }
        this.rows = Collections.unmodifiableList(rowCopy);
        this.data = Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }

    /**
     * Returns the title of the report.
     *
     * @return report title
     */
    public String title() {
        return title;
    }

    /**
     * Returns unmodifiable column header labels.
     *
     * @return list of header strings
     */
    public List<String> headers() {
        return headers;
    }

    /**
     * Returns unmodifiable tabular content rows.
     *
     * @return list of rows, each containing string cell values
     */
    public List<List<String>> rows() {
        return rows;
    }

    /**
     * Returns unmodifiable programmatic data metrics.
     *
     * @return key-value map of metric objects
     */
    public Map<String, Object> data() {
        return data;
    }

    /**
     * Retrieves a named raw data attribute.
     *
     * @param key attribute name
     * @return attribute value, or null if absent
     */
    public Object getData(String key) {
        return data.get(key);
    }
}
