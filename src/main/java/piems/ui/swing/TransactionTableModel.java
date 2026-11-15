package piems.ui.swing;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import piems.service.TransactionView;

/**
 * Table model for rendering transactions in a sortable Swing table.
 *
 * <p>Exposes six tabular columns with typed values supporting automatic column
 * sorting via {@link javax.swing.table.TableRowSorter} (Decision D-40).</p>
 */
public class TransactionTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = {
        "Date", "Kind", "Account", "Category", "Amount", "Note"
    };

    private static final Class<?>[] COLUMN_CLASSES = {
        LocalDate.class, String.class, String.class, String.class, String.class, String.class
    };

    private List<TransactionView> transactions = Collections.emptyList();

    /**
     * Constructs an empty transaction table model.
     */
    public TransactionTableModel() {
        // Default constructor
    }

    /**
     * Updates the displayed transactions and notifies table listeners.
     *
     * @param transactions new list of transaction views
     */
    public void setTransactions(List<TransactionView> transactions) {
        this.transactions = (transactions != null) ? new ArrayList<>(transactions) : Collections.emptyList();
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return transactions.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return COLUMN_CLASSES[columnIndex];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= transactions.size()) {
            return null;
        }
        TransactionView tx = transactions.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> tx.date();
            case 1 -> tx.kind();
            case 2 -> (tx.accountName() != null && !tx.accountName().isBlank())
                    ? tx.accountName()
                    : String.valueOf(tx.accountId());
            case 3 -> (tx.categoryName() != null && !tx.categoryName().isBlank())
                    ? tx.categoryName()
                    : "(none)";
            case 4 -> tx.amount().toString();
            case 5 -> (tx.note() != null) ? tx.note() : "";
            default -> null;
        };
    }

    /**
     * Returns the transaction view at the specified row index.
     *
     * @param rowIndex model row index
     * @return transaction view or null if index is out of bounds
     */
    public TransactionView getTransactionAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < transactions.size()) {
            return transactions.get(rowIndex);
        }
        return null;
    }
}
