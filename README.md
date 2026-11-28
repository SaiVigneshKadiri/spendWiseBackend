# SpendWise API

Spring Boot 3 / Java 21 REST API with PostgreSQL, Flyway migrations, BCrypt passwords, short-lived JWT access tokens, and rotating hashed refresh tokens.

## Run locally (Neon database)

The API uses the Neon Postgres database. There is no local database container.

1. Create `backend/.env` (git-ignored) with these values:

   ```
   JWT_SECRET=<base64 random key, see below>
   PGHOST=<Neon host>
   PGPORT=5432
   PGDATABASE=<database name>
   PGUSER=<database user>
   PGPASSWORD=<database password>
   PGSSLMODE=require
   ```

   Use the **direct (unpooled)** host. Copy the values from `DATABASE_URL_UNPOOLED` in the project-root `.env.local` (written by `neon env pull`). Optionally set `APP_ADMIN_EMAIL` and `APP_ADMIN_PASSWORD` to bootstrap an administrator.

2. Generate a base64-encoded JWT key and paste it into `JWT_SECRET`:

   ```powershell
   $random = [Security.Cryptography.RandomNumberGenerator]::Create()
   $bytes = New-Object byte[] 48
   $random.GetBytes($bytes)
   $jwtSecret = [Convert]::ToBase64String($bytes)
   $random.Dispose()
   Set-Clipboard $jwtSecret
   ```

   Passwords must have at least 6 characters and include an uppercase letter, a lowercase letter, a number, and a special character. `.env` is ignored by Git; do not commit it or share its values.

### Run with Docker

Start Docker Desktop, then from this directory run:

```powershell
docker compose up --build
```

The API listens on `http://localhost:8081`, and Swagger UI is at `http://localhost:8081/swagger-ui/index.html`. Flyway checks the Neon schema on startup. Stop with `Ctrl+C`, or run `docker compose down` to remove the container.

### Run with Maven instead

Java 21 and Maven are required; Docker is not needed. Load `.env` into the current PowerShell session, then start the API:

```powershell
Get-Content .env | Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*=' } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}
mvn spring-boot:run
```

Anyone can create a regular account with POST /api/v1/auth/signup (the app's Create account screen uses it); the response includes tokens, so the user is signed in immediately. Administrators can also provision regular accounts from the protected user-management screen. To provision an administrator on startup, set both `APP_ADMIN_EMAIL` and `APP_ADMIN_PASSWORD` in the trusted runtime environment; newly created accounts must meet the password policy described above. The account is created only if that email does not exist. Never expose or commit bootstrap credentials or JWT secrets.

### Deploy to Railway

Deploy this directory as the backend service (set the service root directory to `backend`). Set `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`, and `PGSSLMODE=require` to the Neon direct (unpooled) connection values, plus a strong random `JWT_SECRET`. Optionally set `APP_ADMIN_EMAIL` and `APP_ADMIN_PASSWORD` to bootstrap the first administrator. Set all of these as Railway variables; never put them in the repository.
## Endpoints

- `POST /api/v1/auth/signup` (public), `POST /api/v1/auth/login`, `/refresh`, `/logout`; authenticated `GET|PUT /api/v1/auth/me` for account details/password (self-service password changes verify the current password)
- `GET|POST /api/v1/users/me/expenses`; `PUT|DELETE /api/v1/users/me/expenses/{id}`
- `GET|POST /api/v1/users/me/budgets`; `PUT|DELETE /api/v1/users/me/budgets/{id}`
- `GET /api/v1/users/me/budgets/alerts`; `PUT /api/v1/users/me/budgets/alerts/{id}/read` (persisted, per-budget configurable threshold notifications)
- `GET|POST /api/v1/users/me/salaries`; `PUT /api/v1/users/me/salaries/{id}`. Each regular account has one salary record; edit it instead of creating another.
- `GET /api/v1/users/me/dashboard`, `/analytics`, `/reports/expenses.csv`. Analytics accepts optional `paymentMethod` (`CASH`, `CARD`, `UPI`, `BANK_TRANSFER`) and `paymentPlatform` (`GOOGLE_PAY`, `PHONEPE`, `PAYTM`, `BHIM`, `AMAZON_PAY`, `OTHER`) filters; selecting a platform limits results to UPI.
- ADMIN-only: `GET /api/v1/admin/dashboard` returns 30-day account activity, monthly account growth, budget risk/utilization, six-month spending trend, current-month category breakdown, top spending accounts, and recent expense activity. It does not expose a single all-user monthly-spend total metric.
- ADMIN-only: `GET /api/v1/admin/users`
- ADMIN-only: `PUT /api/v1/admin/users/{userId}` updates another regular user's name, email, and optionally resets their password without requiring the current password
- ADMIN-only, selected-user scoped: `GET /api/v1/admin/users/{userId}/expenses` and `/analytics` (supports the same optional payment filters); `POST|PUT|DELETE /api/v1/admin/users/{userId}/expenses[/{expenseId}]`
- ADMIN-only: `POST /api/v1/admin/users` creates a regular user account

Money fields use `BigDecimal` and are stored as PostgreSQL `NUMERIC(12,2)`. UPI expenses include a selected payment app (`GOOGLE_PAY`, `PHONEPE`, `PAYTM`, `BHIM`, `AMAZON_PAY`, or `OTHER`). Expenses, budgets, and salaries are scoped to their authenticated owner. Migration V4 retains the most recently effective salary record per account and removes older salary-history records before enforcing one record per account. Migration V5 adds configurable budget alert percentages and notification read state.
