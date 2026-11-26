WITH ranked_salary_records AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY user_id
               ORDER BY effective_from DESC, created_at DESC, id DESC
           ) AS position
    FROM salary_records
)
DELETE FROM salary_records
WHERE id IN (
    SELECT id
    FROM ranked_salary_records
    WHERE position > 1
);

CREATE UNIQUE INDEX uq_salary_records_user
    ON salary_records (user_id);
