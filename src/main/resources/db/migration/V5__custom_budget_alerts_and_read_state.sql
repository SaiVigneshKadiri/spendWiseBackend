ALTER TABLE budgets
    ADD COLUMN alert_percentage INTEGER NOT NULL DEFAULT 80
        CHECK (alert_percentage BETWEEN 1 AND 100);

ALTER TABLE budget_alerts
    DROP CONSTRAINT budget_alerts_threshold_percentage_check,
    ADD CONSTRAINT ck_budget_alert_threshold_percentage
        CHECK (threshold_percentage BETWEEN 1 AND 100),
    ADD COLUMN read_at TIMESTAMPTZ;
