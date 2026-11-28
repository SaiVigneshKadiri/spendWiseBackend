-- Login, signup and admin user lookups use lower(email); the unique index on email cannot serve them.
CREATE INDEX idx_app_users_email_lower ON app_users (lower(email));

-- Nightly purge deletes by expires_at and revoked_at; without these it scans the whole table.
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
CREATE INDEX idx_refresh_tokens_revoked_at ON refresh_tokens (revoked_at) WHERE revoked_at IS NOT NULL;

-- Each of these is a column prefix of an index that already exists (or is unique per user), so it adds write cost only.
DROP INDEX idx_expenses_user_datetime;
DROP INDEX idx_budgets_user_month;
DROP INDEX idx_salary_user_date;
