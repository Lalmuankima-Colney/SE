package piems.domain;

import piems.repository.JdbcTransactionRepository;

/**
 * Deliberate violation: domain importing repository.
 */
public class BadDomain {
    private JdbcTransactionRepository repo;
}
