CREATE INDEX idx_expenses_user_payment_method_datetime
    ON expenses (user_id, payment_method, expense_datetime DESC)
    INCLUDE (payment_platform, amount, category);

CREATE INDEX idx_expenses_user_datetime_payment_filters
    ON expenses (user_id, expense_datetime DESC)
    INCLUDE (payment_method, payment_platform, amount, category);
