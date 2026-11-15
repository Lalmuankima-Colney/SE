/**
 * Swing Graphical User Interface components for PIEMS.
 *
 * <p>Implements the Model-View-Presenter (MVP) pattern where presentation logic
 * is encapsulated in {@code MainPresenter} and interacts exclusively with {@code FinanceService}.
 * Contains no business logic, executes long-running operations off the Swing Event Dispatch Thread (EDT),
 * and handles all errors through a top-level error handler (Decisions D-05, D-15, D-40).</p>
 */
package piems.ui.swing;
