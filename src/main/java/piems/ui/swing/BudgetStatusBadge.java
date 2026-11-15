package piems.ui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import piems.domain.BudgetStatus;

/**
 * Visual badge widget presenting budget status using both colour and text (FR-10).
 *
 * <p>Adheres to accessibility and design guidelines by pairing distinctive background/foreground
 * colour tones with explicit textual descriptors ([OK], [WARNING], [EXCEEDED]) (Decision D-40).</p>
 */
public class BudgetStatusBadge extends JPanel {

    private static final Color OK_BG = new Color(220, 252, 231);
    private static final Color OK_FG = new Color(22, 101, 52);
    private static final Color WARN_BG = new Color(254, 243, 199);
    private static final Color WARN_FG = new Color(180, 83, 9);
    private static final Color EXCEED_BG = new Color(254, 226, 226);
    private static final Color EXCEED_FG = new Color(185, 28, 28);
    private static final Color NEUTRAL_BG = new Color(243, 244, 246);
    private static final Color NEUTRAL_FG = new Color(75, 85, 99);

    private final JLabel label;
    private BudgetStatus currentStatus;

    /**
     * Constructs a budget status badge with default neutral styling.
     */
    public BudgetStatusBadge() {
        super(new BorderLayout());
        this.label = new JLabel("[NO BUDGET] Select category", SwingConstants.CENTER);
        this.label.setFont(this.label.getFont().deriveFont(Font.BOLD, 12.0f));
        this.label.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        add(label, BorderLayout.CENTER);
        setBorder(BorderFactory.createLineBorder(NEUTRAL_FG, 1));
        setBackground(NEUTRAL_BG);
    }

    /**
     * Updates the badge presentation with status description and appropriate colour tones.
     *
     * @param statusText text description of the budget evaluation
     * @param status     domain status enum, or null for inactive/unconfigured
     */
    public void updateStatus(String statusText, BudgetStatus status) {
        this.currentStatus = status;
        this.label.setText(statusText);
        Color bg = (status == null) ? NEUTRAL_BG : switch (status) {
            case OK -> OK_BG;
            case WARNING -> WARN_BG;
            case EXCEEDED -> EXCEED_BG;
        };
        Color fg = (status == null) ? NEUTRAL_FG : switch (status) {
            case OK -> OK_FG;
            case WARNING -> WARN_FG;
            case EXCEEDED -> EXCEED_FG;
        };
        setBackground(bg);
        label.setForeground(fg);
        setBorder(BorderFactory.createLineBorder(fg, 1));
        repaint();
    }

    /**
     * Returns the currently displayed text description.
     *
     * @return status text
     */
    public String getStatusText() {
        return label.getText();
    }

    /**
     * Returns the current budget status enum.
     *
     * @return current status or null
     */
    public BudgetStatus getCurrentStatus() {
        return currentStatus;
    }
}
