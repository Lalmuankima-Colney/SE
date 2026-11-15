package piems.ui.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
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
import piems.domain.Money;
import piems.service.TransactionView;

/**
 * Main application window for the Swing graphical user interface.
 *
 * <p>Implements {@link MainView} with a two-pane layout: balance summary and expense recording form
 * on the left and sortable/filterable transaction table on the right (Decision D-40, D-49).</p>
 */
public class MainFrame extends JFrame implements MainView {

    private final TransactionTableModel tableModel = new TransactionTableModel();
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<TransactionTableModel> sorter = new TableRowSorter<>(tableModel);

    private final JLabel netWorthLabel = new JLabel("Net Worth: ₹0.00");
    private final DefaultListModel<String> balancesListModel = new DefaultListModel<>();
    private final JList<String> balancesList = new JList<>(balancesListModel);
    private final Map<Long, Money> accountBalanceCache = new HashMap<>();

    private final JComboBox<AccountItem> accountCombo = new JComboBox<>();
    private final JButton newAccountButton = new JButton("+ New");
    private final JLabel availableBalanceLabel = new JLabel("Available: -");

    private final JComboBox<CategoryItem> categoryCombo = new JComboBox<>();
    private final JButton newCategoryButton = new JButton("+ New");

    private final JTextField amountField = new JTextField(8);
    private final JTextField dateField = new JTextField(8);
    private final JTextField noteField = new JTextField(12);
    private final JButton addExpenseButton = new JButton("Record Expense");
    private final JButton clearExpenseButton = new JButton("Clear Form");
    private final BudgetStatusBadge budgetBadge = new BudgetStatusBadge();

    private final JTextField filterText = new JTextField(8);
    private final JComboBox<AccountItem> filterAccountCombo = new JComboBox<>();
    private final JComboBox<CategoryItem> filterCategoryCombo = new JComboBox<>();
    private final JButton filterButton = new JButton("Filter");
    private final JButton resetButton = new JButton("Reset");

    private final JLabel statusLabel = new JLabel("Ready");
    private final JButton loadDemoButton = new JButton("Load demo data");
    private final JProgressBar progressBar = new JProgressBar();
    private MainPresenter presenter;

    /**
     * Constructs and arranges the main frame layout.
     */
    public MainFrame() {
        super("PIEMS - Personal Income & Expense Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1040, 640);
        setLocationRelativeTo(null);
        table.setRowSorter(sorter);
        dateField.setText(LocalDate.now().toString());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildExpensePanel(), buildTablePanel());
        splitPane.setDividerLocation(380);
        add(splitPane, BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);
        attachActionListeners();
    }

    private JPanel buildExpensePanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(buildBalancesPanel(), BorderLayout.NORTH);
        panel.add(buildExpenseForm(), BorderLayout.CENTER);
        panel.add(budgetBadge, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildBalancesPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Account Balances & Net Worth"));
        netWorthLabel.setFont(netWorthLabel.getFont().deriveFont(Font.BOLD, 13f));
        panel.add(netWorthLabel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(balancesList);
        scroll.setPreferredSize(new Dimension(340, 95));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildExpenseForm() {
        JPanel form = new JPanel(new GridLayout(7, 2, 4, 4));
        form.setBorder(BorderFactory.createTitledBorder("Record Expense"));

        JPanel accRow = new JPanel(new BorderLayout(4, 0));
        accRow.add(accountCombo, BorderLayout.CENTER);
        accRow.add(newAccountButton, BorderLayout.EAST);

        JPanel catRow = new JPanel(new BorderLayout(4, 0));
        catRow.add(categoryCombo, BorderLayout.CENTER);
        catRow.add(newCategoryButton, BorderLayout.EAST);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        btnRow.add(addExpenseButton);
        btnRow.add(clearExpenseButton);

        form.add(new JLabel("Account:"));
        form.add(accRow);
        form.add(new JLabel("Balance:"));
        form.add(availableBalanceLabel);
        form.add(new JLabel("Category:"));
        form.add(catRow);
        form.add(new JLabel("Amount:"));
        form.add(amountField);
        form.add(new JLabel("Date (yyyy-MM-dd):"));
        form.add(dateField);
        form.add(new JLabel("Note:"));
        form.add(noteField);
        form.add(new JLabel(""));
        form.add(btnRow);
        return form;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(buildFilterBar(), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildFilterBar() {
        JPanel bar = new JPanel(new BorderLayout(4, 2));
        bar.setBorder(BorderFactory.createTitledBorder("Search & Filter"));

        JPanel inputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        inputs.add(new JLabel("Text:"));
        inputs.add(filterText);
        inputs.add(new JLabel("Account:"));
        inputs.add(filterAccountCombo);
        inputs.add(new JLabel("Category:"));
        inputs.add(filterCategoryCombo);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        actions.add(filterButton);
        actions.add(resetButton);

        bar.add(inputs, BorderLayout.CENTER);
        bar.add(actions, BorderLayout.SOUTH);
        return bar;
    }

    private JPanel buildBottomBar() {
        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.add(statusLabel);
        left.add(loadDemoButton);

        bottom.add(left, BorderLayout.WEST);
        bottom.add(progressBar, BorderLayout.EAST);
        return bottom;
    }

    private void attachActionListeners() {
        addExpenseButton.addActionListener(e -> onAddExpenseClicked());
        clearExpenseButton.addActionListener(e -> onClearExpenseClicked());
        newAccountButton.addActionListener(e -> onAddAccountClicked());
        newCategoryButton.addActionListener(e -> onAddCategoryClicked());
        accountCombo.addActionListener(e -> onAccountSelected());
        categoryCombo.addActionListener(e -> onCategorySelected());
        filterButton.addActionListener(e -> onFilterClicked());
        resetButton.addActionListener(e -> onResetFilterClicked());
        loadDemoButton.addActionListener(e -> onLoadDemoClicked());
    }

    private void onAccountSelected() {
        AccountItem item = (AccountItem) accountCombo.getSelectedItem();
        if (item == null || item.id() == null || item.id() == 0L) {
            availableBalanceLabel.setText("Available: -");
            return;
        }
        Money bal = accountBalanceCache.get(item.id());
        availableBalanceLabel.setText("Available: " + (bal != null ? bal.toString() : "-"));
    }

    private void onClearExpenseClicked() {
        if (presenter != null) {
            presenter.clearExpenseForm();
        } else {
            clearExpenseInputs();
        }
    }

    private void onAddAccountClicked() {
        if (presenter == null) {
            return;
        }
        JTextField nameFld = new JTextField(10);
        JComboBox<String> typeCb = new JComboBox<>(new String[]{"BANK", "CASH", "CREDIT_CARD"});
        JTextField balFld = new JTextField("0.00", 8);

        JPanel p = new JPanel(new GridLayout(3, 2, 4, 4));
        p.add(new JLabel("Name:"));
        p.add(nameFld);
        p.add(new JLabel("Type:"));
        p.add(typeCb);
        p.add(new JLabel("Opening / Limit:"));
        p.add(balFld);

        int opt = JOptionPane.showConfirmDialog(this, p, "Create New Account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt == JOptionPane.OK_OPTION) {
            presenter.addAccount(nameFld.getText(), (String) typeCb.getSelectedItem(), balFld.getText());
        }
    }

    private void onAddCategoryClicked() {
        if (presenter == null) {
            return;
        }
        JTextField nameFld = new JTextField(10);
        JComboBox<String> kindCb = new JComboBox<>(new String[]{"EXPENSE", "INCOME"});

        JPanel p = new JPanel(new GridLayout(2, 2, 4, 4));
        p.add(new JLabel("Category Name:"));
        p.add(nameFld);
        p.add(new JLabel("Kind:"));
        p.add(kindCb);

        int opt = JOptionPane.showConfirmDialog(this, p, "Create New Category",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt == JOptionPane.OK_OPTION) {
            presenter.addCategory(nameFld.getText(), (String) kindCb.getSelectedItem(), null);
        }
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
        if (filterAccountCombo.getItemCount() > 0) {
            filterAccountCombo.setSelectedIndex(0);
        }
        if (filterCategoryCombo.getItemCount() > 0) {
            filterCategoryCombo.setSelectedIndex(0);
        }
        if (presenter != null) {
            presenter.applyFilter(null, null, null, null, null);
            statusLabel.setText("Filters reset.");
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

    private void onLoadDemoClicked() {
        if (presenter == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "This will populate the system with synthetic demonstration accounts, categories,\n"
                        + "transactions, and budgets for evaluation purposes.\n\n"
                        + "Note: This operation can only be performed on an empty ledger.\n\n"
                        + "Do you wish to proceed?",
                "Load Demonstration Data",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            presenter.loadDemoData();
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
    public void showAccountBalances(List<AccountBalanceItem> balances, Money netWorth) {
        accountBalanceCache.clear();
        balancesListModel.clear();
        netWorthLabel.setText("Net Worth: " + (netWorth != null ? netWorth.toString() : "0.00"));
        for (AccountBalanceItem item : balances) {
            accountBalanceCache.put(item.id(), item.balance());
            balancesListModel.addElement(String.format("• %s (%s): %s", item.name(), item.type(), item.balance()));
        }
        onAccountSelected();
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
        onAccountSelected();
    }

    @Override
    public void setCategories(List<CategoryComponent> categories) {
        categoryCombo.removeAllItems();
        filterCategoryCombo.removeAllItems();
        categoryCombo.addItem(new CategoryItem(0L, "(None)"));
        filterCategoryCombo.addItem(new CategoryItem(0L, "All Categories"));
        for (CategoryComponent c : categories) {
            populateCategoryHierarchy(c, "");
        }
    }

    private void populateCategoryHierarchy(CategoryComponent comp, String prefix) {
        String displayName = prefix.isEmpty() ? comp.name() : prefix + " > " + comp.name();
        categoryCombo.addItem(new CategoryItem(comp.id(), displayName));
        filterCategoryCombo.addItem(new CategoryItem(comp.id(), displayName));
        for (CategoryComponent child : comp.children()) {
            populateCategoryHierarchy(child, displayName);
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
        dateField.setText(LocalDate.now().toString());
        if (categoryCombo.getItemCount() > 0) {
            categoryCombo.setSelectedIndex(0);
        }
    }

    @Override
    public void setBusy(boolean busy) {
        progressBar.setVisible(busy);
        addExpenseButton.setEnabled(!busy);
        clearExpenseButton.setEnabled(!busy);
        newAccountButton.setEnabled(!busy);
        newCategoryButton.setEnabled(!busy);
        filterButton.setEnabled(!busy);
        resetButton.setEnabled(!busy);
        loadDemoButton.setEnabled(!busy);
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
