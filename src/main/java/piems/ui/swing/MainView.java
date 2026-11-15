package piems.ui.swing;

import java.util.List;
import piems.domain.Account;
import piems.domain.BudgetStatus;
import piems.domain.CategoryComponent;
import piems.domain.Money;
import piems.service.TransactionView;

/**
 * View interface in the Model-View-Presenter (MVP) architecture for the Swing desktop interface.
 *
 * <p>Exposes visual presentation contracts to {@link MainPresenter} without coupling
 * presenter logic to concrete Swing widgets or rendering lifecycles (Decision D-40).</p>
 */
public interface MainView {

    /**
     * Binds the presenter to this view.
     *
     * @param presenter presenter handling user actions and facade coordination
     */
    void setPresenter(MainPresenter presenter);

    /**
     * Displays the given list of transaction view records in the transaction table.
     *
     * @param transactions list of transactions to display
     */
    void showTransactions(List<TransactionView> transactions);

    /**
     * Populates the available account options in account selection controls.
     *
     * @param accounts list of active user accounts
     */
    void setAccounts(List<Account> accounts);

    /**
     * Populates the available category options in category selection controls.
     *
     * @param categories list of leaf categories
     */
    void setCategories(List<CategoryComponent> categories);

    /**
     * Displays category budget evaluation status using colour and text.
     *
     * @param statusText human-readable status description
     * @param status     domain budget status enum (OK, WARNING, EXCEEDED) or null
     */
    void showBudgetStatus(String statusText, BudgetStatus status);

    /**
     * Displays a top-level error notification to the user.
     *
     * @param message error message
     */
    void showErrorMessage(String message);

    /**
     * Displays an informational success notification to the user.
     *
     * @param message success message
     */
    void showSuccessMessage(String message);

    /**
     * Clears input fields on the expense recording form.
     */
    void clearExpenseInputs();

    /**
     * Displays derived account balances and aggregate net worth (FR-07).
     *
     * @param balances list of account balance view models
     * @param netWorth aggregate net worth across accounts
     */
    void showAccountBalances(List<AccountBalanceItem> balances, Money netWorth);

    /**
     * Toggles the busy/loading state indicator on the view.
     *
     * @param busy true if an asynchronous operation is in progress
     */
    void setBusy(boolean busy);
}
