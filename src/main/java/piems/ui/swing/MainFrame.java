package piems.ui.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableRowSorter;
import piems.domain.Account;
import piems.domain.BudgetStatus;
import piems.domain.CategoryComponent;
import piems.service.TransactionView;

/**
 * Main application window for the Swing graphical user interface.
 *
 * <p>Implements {@link MainView} with a two-pane layout: expense recording form
 * on the left and sortable/filterable transaction table on the right (Decision D-40).</p>
 */
public class MainFrame extends JFrame implements MainView {

    private final TransactionTableModel tableModel = new TransactionTableModel();
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<TransactionTableModel> sorter = new TableRowSorter<>(tableModel);

    private final JComboBox<AccountItem> accountCombo = new JComboBox<>();
    private final JComboBox<CategoryItem> categoryCombo = new JComboBox<>();
    private final JTextField amountField = new JTextField(8);
    private final JTextField dateField = new JTextField(8);
    private final JTextField noteField = new JTextField(12);
    private final JButton addExpenseButton = new JButton("Record Expense");
    private final BudgetStatusBadge budgetBadge = new BudgetStatusBadge();

    private final JTextField filterText = new JTextField(8);
    private final JComboBox<AccountItem> filterAccountCombo = new JComboBox<>();
    private final JComboBox<CategoryItem> filterCategoryCombo = new JComboBox<>();
    private final JButton filterButton = new JButton("Filter");
    private final JButton resetButton = new JButton("Reset");

    private final JLabel statusLabel = new JLabel("Ready");
    private final JProgressBar progressBar = new JProgressBar();
    private MainPresenter presenter;

    /**
     * Constructs and arranges the main frame layout.
     */
    public MainFrame() {
        super("PIEMS - Personal Income & Expense Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        table.setRowSorter(sorter);
        dateField.setText(LocalDate.now().toString());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildExpensePanel(), buildTablePanel());
        splitPane.setDividerLocation(360);
        add(splitPane, BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);
        attachActionListeners();
    }

    private JPanel buildExpensePanel() {
        JPanel form = new JPanel(new GridLayout(6, 2, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder("Record Expense"));
        form.add(new JLabel("Account:"));
        form.add(accountCombo);
        form.add(new JLabel("Category:"));
        form.add(categoryCombo);
        form.add(new JLabel("Amount:"));
        form.add(amountField);
        form.add(new JLabel("Date (yyyy-MM-dd):"));
        form.add(dateField);
        form.add(new JLabel("Note:"));
        form.add(noteField);
        form.add(new JLabel(""));
        form.add(addExpenseButton);

        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(form, BorderLayout.NORTH);
        panel.add(budgetBadge, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(buildFilterBar(), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        bar.setBorder(BorderFactory.createTitledBorder("Search & Filter"));
        bar.add(new JLabel("Text:"));
        bar.add(filterText);
        bar.add(new JLabel("Account:"));
        bar.add(filterAccountCombo);
        bar.add(new JLabel("Category:"));
        bar.add(filterCategoryCombo);
        bar.add(filterButton);
        bar.add(resetButton);
        return bar;
    }

    private JPanel buildBottomBar() {
        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        bottom.add(statusLabel, BorderLayout.WEST);
        bottom.add(progressBar, BorderLayout.EAST);
        return bottom;
    }

    private void attachActionListeners() {
        addExpenseButton.addActionListener(e -> onAddExpenseClicked());
        filterButton.addActionListener(e -> onFilterClicked());
        resetButton.addActionListener(e -> onResetFilterClicked());
        categoryCombo.addActionListener(e -> onCategorySelected());
    }

    private void onAddExpenseClicked() {
        if (presenter == null) {
            return;
        }
        AccountItem acc = (AccountItem) accountCombo.getSelectedItem();
        CategoryItem cat = (CategoryItem) categoryCombo.getSelectedItem();
        Long accId = (acc != null) ? acc.id() : null;
        Long catId = (cat != null && cat.id() != null && cat.id() > 0) ? cat.id() : null;
        LocalDate date = parseDateOrNull(dateField.getText());
        presenter.addExpense(accId, catId, amountField.getText(), date, noteField.getText());
    }

    private void onFilterClicked() {
        if (presenter == null) {
            return;
        }
        AccountItem acc = (AccountItem) filterAccountCombo.getSelectedItem();
        CategoryItem cat = (CategoryItem) filterCategoryCombo.getSelectedItem();
        Long accId = (acc != null && acc.id() != null && acc.id() > 0) ? acc.id() : null;
        Long catId = (cat != null && cat.id() != null && cat.id() > 0) ? cat.id() : null;
        presenter.applyFilter(filterText.getText(), accId, catId, null, null);
    }

    private void onResetFilterClicked() {
        filterText.setText("");
        filterAccountCombo.setSelectedIndex(0);
        filterCategoryCombo.setSelectedIndex(0);
        if (presenter != null) {
            presenter.applyFilter(null, null, null, null, null);
        }
    }

    private void onCategorySelected() {
        if (presenter == null) {
            return;
        }
        CategoryItem cat = (CategoryItem) categoryCombo.getSelectedItem();
        if (cat != null && cat.id() != null && cat.id() > 0) {
            LocalDate d = parseDateOrNull(dateField.getText());
            YearMonth ym = YearMonth.from(d != null ? d : LocalDate.now());
            presenter.checkBudgetStatus(cat.id(), ym);
        } else {
            budgetBadge.updateStatus("[NO BUDGET] Select category", null);
        }
    }

    private LocalDate parseDateOrNull(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void setPresenter(MainPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void showTransactions(List<TransactionView> transactions) {
        tableModel.setTransactions(transactions);
        statusLabel.setText("Loaded " + transactions.size() + " transactions.");
    }

    @Override
    public void setAccounts(List<Account> accounts) {
        accountCombo.removeAllItems();
        filterAccountCombo.removeAllItems();
        filterAccountCombo.addItem(new AccountItem(0L, "All Accounts"));
        for (Account a : accounts) {
            accountCombo.addItem(new AccountItem(a.id(), a.name()));
            filterAccountCombo.addItem(new AccountItem(a.id(), a.name()));
        }
    }

    @Override
    public void setCategories(List<CategoryComponent> categories) {
        categoryCombo.removeAllItems();
        filterCategoryCombo.removeAllItems();
        categoryCombo.addItem(new CategoryItem(0L, "(None)"));
        filterCategoryCombo.addItem(new CategoryItem(0L, "All Categories"));
        for (CategoryComponent c : categories) {
            categoryCombo.addItem(new CategoryItem(c.id(), c.name()));
            filterCategoryCombo.addItem(new CategoryItem(c.id(), c.name()));
        }
    }

    @Override
    public void showBudgetStatus(String statusText, BudgetStatus status) {
        budgetBadge.updateStatus(statusText, status);
    }

    @Override
    public void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void showSuccessMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void clearExpenseInputs() {
        amountField.setText("");
        noteField.setText("");
    }

    @Override
    public void setBusy(boolean busy) {
        progressBar.setVisible(busy);
        addExpenseButton.setEnabled(!busy);
        filterButton.setEnabled(!busy);
    }

    private record AccountItem(Long id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    private record CategoryItem(Long id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }
}
