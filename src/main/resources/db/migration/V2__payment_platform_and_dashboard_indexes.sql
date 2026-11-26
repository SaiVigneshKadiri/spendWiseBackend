ALTER TABLE expenses
    ADD COLUMN payment_platform VARCHAR(50);

CREATE INDEX idx_expenses_datetime_covering
    ON expenses (expense_datetime DESC)
    INCLUDE (user_id, amount, category);

CREATE INDEX idx_expenses_user_category_datetime
    ON expenses (user_id, category, expense_datetime DESC)
    INCLUDE (amount);

CREATE INDEX idx_budgets_month_user
    ON budgets (budget_month, user_id)
    INCLUDE (category, amount);

CREATE INDEX idx_app_users_created_at
    ON app_users (created_at DESC);
