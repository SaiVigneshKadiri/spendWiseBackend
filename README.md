<<<<<<< HEAD
# SpendWise API

Spring Boot 3 / Java 21 REST API with PostgreSQL, Flyway migrations, BCrypt passwords, short-lived JWT access tokens, and rotating hashed refresh tokens.

## Run locally

From this directory, create a local `.env` file from the template:

```powershell
Copy-Item .env.example .env
```

Generate a base64-encoded JWT key and copy it to the `JWT_SECRET` line in `.env`:

```powershell
$random = [Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 48
$random.GetBytes($bytes)
$jwtSecret = [Convert]::ToBase64String($bytes)
$random.Dispose()
Set-Clipboard $jwtSecret
```

Set `PGPASSWORD` in `.env` to a strong local development password. `PGDATABASE` and `PGUSER` configure the local database name and user. To create an admin login, uncomment and set both `APP_ADMIN_EMAIL` and `APP_ADMIN_PASSWORD`; passwords must have at least 6 characters and include an uppercase letter, a lowercase letter, a number, and a special character. `.env` is ignored by Git; do not commit it or share its values.

If you already have a `.env` from the previous configuration, rename
`DATABASE_PASSWORD` to `PGPASSWORD` and add `PGDATABASE=spendwise` and
`PGUSER=spendwise`. Keep the existing password value so it continues to match
the current Docker database volume.

### Easiest: Docker

Start Docker Desktop, then from this directory run:

```powershell
docker compose up --build
```

This starts PostgreSQL and the API. Flyway applies the database schema automatically. The API listens on `http://localhost:8080`; Swagger UI is at `http://localhost:8080/swagger-ui/index.html`. Stop with `Ctrl+C`; run `docker compose down` to stop and remove the containers while retaining database data.

### Run the API with Maven instead

Keep the `.env` file, then load it into the current PowerShell session and start PostgreSQL:

```powershell
Get-Content .env | Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*=' } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    if (-not $name.StartsWith('#')) { [Environment]::SetEnvironmentVariable($name, $value, 'Process') }
}
$env:PGHOST = 'localhost'
$env:PGPORT = '5432'
$env:PGSSLMODE = 'disable'
docker compose up -d postgres
mvn spring-boot:run
```

Java 21 and Maven are required for this option. Docker is still used for PostgreSQL.

Anyone can create a regular account with POST /api/v1/auth/signup (the app's Create account screen uses it); the response includes tokens, so the user is signed in immediately. Administrators can also provision regular accounts from the protected user-management screen. To provision an administrator on startup, set both `APP_ADMIN_EMAIL` and `APP_ADMIN_PASSWORD` in the trusted runtime environment; newly created accounts must meet the password policy described above. The account is created only if that email does not exist. Never expose or commit bootstrap credentials or JWT secrets.

### Deploy to Railway

Deploy the backend service from this directory (or set the Railway service root
directory to `backend`) and add a PostgreSQL service to the same Railway
project. On the backend service, add references to the PostgreSQL service's
`PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD` variables. Also set
a strong, random `JWT_SECRET`. Optionally set both `APP_ADMIN_EMAIL` and
`APP_ADMIN_PASSWORD` to bootstrap the first administrator. Do not copy secret
values into the repository or a properties file. The application requires its
database and JWT variables at startup and has no hard-coded database credential
fallbacks. Use the database service's private-network variables; Railway's
provided `DATABASE_URL` is a `postgresql://` URL, while the application builds
its JDBC URL from the `PG*` variables.

### Run the local API against Neon

The Neon CLI writes `DATABASE_URL_UNPOOLED` to the project-root `.env.local`.
After setting `JWT_SECRET` in `backend/.env`, run this from PowerShell:

```powershell
.\run-neon.ps1
```

The script reads the Neon direct connection URL without printing it, extracts
the PostgreSQL connection values for the API process, requires TLS, builds the
backend image, and starts the API on port `8080`. The port is published on host
interfaces so an Android phone on the same trusted Wi-Fi can reach it. Allow
inbound port `8080` through Windows Firewall for the Private profile only; do
not expose this development API to public networks. Run the app with your PC's
Wi-Fi IPv4 address:

```powershell
flutter run -d <device-id> --dart-define=API_BASE_URL=http://<pc-wifi-ip>:8080/api/v1
```

It runs Flyway migrations against the selected Neon branch at startup. Stop it
with `Ctrl+C`. Do not run the local Compose API at the same time; Compose
intentionally uses the local `postgres` service instead.

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
=======
# spendWiseBackend
>>>>>>> e595383918fcd74fe719e087f539547403453964
