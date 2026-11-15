package piems.ui;

import piems.repository.JdbcTransactionRepository;

/**
 * Deliberate violation: UI importing repository.
 */
public class BadUi {
    private JdbcTransactionRepository repo;
}
