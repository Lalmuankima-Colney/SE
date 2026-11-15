package piems.ui.swing;

import java.util.Objects;
import javax.swing.SwingUtilities;
import piems.service.FinanceService;

/**
 * Bootstrap launcher for the PIEMS Swing graphical desktop interface (Decision D-40).
 *
 * <p>Configures a top-level uncaught exception handler, instantiates the {@link MainFrame}
 * and {@link MainPresenter}, and schedules window display onto the Swing Event Dispatch Thread (EDT).</p>
 */
public final class SwingApp {

    private SwingApp() {
        // Utility launcher class
    }

    /**
     * Bootstraps and launches the Swing desktop application.
     *
     * @param service configured finance service facade
     */
    public static void launch(FinanceService service) {
        Objects.requireNonNull(service, "FinanceService cannot be null");
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Uncaught exception in " + thread.getName() + ": " + throwable.getMessage());
        });
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            MainPresenter presenter = new MainPresenter(service, frame);
            presenter.initialize();
            frame.setVisible(true);
        });
    }
}
