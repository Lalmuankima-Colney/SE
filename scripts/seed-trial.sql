-- PIEMS Acceptance Trial Seed Data (Proposal P-309, Decision D-44)
-- Provides a clean, reproducible baseline for user acceptance evaluation.
-- Account balances are derived via D-25 without storing a balance column.

INSERT INTO accounts (name, type, opening_minor, credit_limit_minor, archived) VALUES
    ('Cash Wallet', 'CASH', 10000, 0, 0),
    ('Checking', 'BANK', 150000, 0, 0),
    ('Visa Card', 'CREDIT_CARD', 0, 100000, 0);

INSERT INTO categories (name, parent_id, kind, archived) VALUES
    ('Groceries', NULL, 'EXPENSE', 0),
    ('Dining Out', NULL, 'EXPENSE', 0),
    ('Utilities', NULL, 'EXPENSE', 0),
    ('Salary', NULL, 'INCOME', 0);
