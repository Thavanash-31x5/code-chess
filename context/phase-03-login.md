# Phase 3: Login

## Purpose

This phase added email-and-password login to the plain Java web application while preserving the existing registration feature.

The implementation remains within the existing architecture:

- HTML, CSS, and Vanilla JavaScript frontend
- JDK `HttpServer`
- Java service classes
- JDBC
- MySQL
- Gradle
- PBKDF2 password hashing

No web framework, ORM, session system, JWT, cookie authentication, or authorization system was added.

## Login Flow

The login flow is:

1. The browser opens `/`, `/login`, or the login page is loaded directly.
2. The login form collects an email address and password.
3. Login JavaScript sends a JSON `POST` request to `/api/login`.
4. Java parses the JSON request body.
5. `LoginService` validates and normalizes the email address.
6. JDBC queries the `users` table for the stored `password_hash`.
7. `PasswordHasher.verify(...)` checks the submitted password against the stored PBKDF2 hash.
8. The server returns an HTTP response based on the result.

## Frontend Structure

The static resources are separated into independent login and registration folders:

```text
src/main/resources/static/
  login/
    index.html
    styles.css
    app.js
  registration/
    index.html
    styles.css
    app.js
```

### Login Page

`static/login/index.html` contains:

- Email input
- Password input
- Login form
- Login status message
- Link to `/registration`
- References to `/login/styles.css` and `/login/app.js`

`static/login/app.js` submits JSON using `fetch()`:

```text
POST /api/login
Content-Type: application/json
```

The JSON body contains:

```json
{
  "email": "user@example.com",
  "password": "submitted-password"
}
```

The login page displays a success or error message based on the HTTP response.

### Registration Page

The original registration files were moved into `static/registration/`.

The registration page includes a Sign in link to:

```text
/login
```

Its resource references use absolute paths:

```html
<link rel="stylesheet" href="/registration/styles.css">
<script src="/registration/app.js"></script>
```

The absolute paths are required because the page URL is `/registration`. Without them, the browser requests `/styles.css` and `/app.js`, which are not the registration resource paths.

Registration continues to submit URL-encoded data to `/api/register`.

## Java Routing

`app.Application` creates these page contexts:

```text
/             -> login page
/login        -> login page
/registration -> registration page
```

The static asset routes are:

```text
/login/styles.css
/login/app.js
/registration/styles.css
/registration/app.js
```

The existing registration API remains available:

```text
POST /api/register
```

The new login API is:

```text
POST /api/login
```

The root handler loads resources from the classpath using:

```java
Application.class.getResourceAsStream(resourcePath)
```

Content types are selected according to the file extension:

- `.html` -> `text/html; charset=UTF-8`
- `.css` -> `text/css; charset=UTF-8`
- `.js` -> `text/javascript; charset=UTF-8`

## Login Backend Components

### `LoginRequest`

`app.login.LoginRequest` is a Java record containing:

```text
email
password
```

### `LoginService`

`app.login.LoginService`:

- Validates that email and password are present.
- Normalizes email to lowercase.
- Validates the email format.
- Queries the `users` table using a prepared statement.
- Retrieves the `password_hash` column.
- Calls `PasswordHasher.verify(...)`.
- Returns `true` for valid credentials.
- Returns `false` for an unknown email or incorrect password.

The database query is equivalent to:

```sql
SELECT password_hash
FROM users
WHERE email = ?
```

The query does not expose whether an email exists to the HTTP client because both unknown emails and incorrect passwords produce failed authentication.

## Password Verification

`PasswordHasher` already generated PBKDF2 hashes for registration. This phase added verification support.

The hashing format is:

```text
pbkdf2-sha256$iterations$salt$hash
```

`PasswordHasher.verify(...)`:

1. Parses the stored hash.
2. Reads the stored iteration count.
3. Decodes the stored salt and expected hash.
4. Derives a hash from the submitted password using the stored salt and iterations.
5. Compares the derived hash with the stored hash using `MessageDigest.isEqual(...)`.
6. Returns `false` for malformed hashes or invalid credentials.

Passwords are not stored in plaintext.

## HTTP Response Behavior

The login handler accepts only `POST` requests.

Expected responses:

```text
Valid credentials    -> 200 OK with an empty response body
Invalid credentials  -> 401 Unauthorized
Invalid JSON         -> 400 Bad Request
Invalid fields       -> 400 Bad Request
Wrong HTTP method    -> 405 Method Not Allowed
Unexpected failure   -> 500 Internal Server Error
```

The successful response uses an empty body so the frontend can identify success from the status code alone.

Registration response behavior remains:

```text
Successful registration -> 201 Created
Invalid registration    -> 400 Bad Request
Duplicate email         -> 409 Conflict
Unexpected failure      -> 500 Internal Server Error
```

Successful registration does not redirect to login. The registration form remains on the registration page and resets after success.

## Database Integration

No database schema change was required.

The existing `users` table already contains the required login fields:

```sql
email VARCHAR(254) NOT NULL,
password_hash VARCHAR(255) NOT NULL,
UNIQUE KEY uk_users_email (email)
```

Database connections continue to use:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

`DatabaseConfig` supplies the configuration and `JdbcConnectionFactory` opens JDBC connections.

## Files Added or Modified

Expected login-related files:

```text
src/main/java/app/Application.java
src/main/java/app/login/LoginRequest.java
src/main/java/app/login/LoginService.java
src/main/java/app/security/PasswordHasher.java
src/main/resources/static/login/index.html
src/main/resources/static/login/styles.css
src/main/resources/static/login/app.js
src/main/resources/static/registration/index.html
src/main/resources/static/registration/styles.css
src/main/resources/static/registration/app.js
```

The database schema and existing registration service were not structurally changed.

## Verification Plan

Run the build:

```bash
gradle clean build
```

Start the application with valid database environment variables:

```bash
gradle run
```

Verify these pages:

```text
/
/login
/registration
```

Verify these assets load without `404` responses:

```text
/login/styles.css
/login/app.js
/registration/styles.css
/registration/app.js
```

Verify the login API with valid credentials:

```bash
curl -i \\
  -X POST http://localhost:8080/api/login \\
  -H "Content-Type: application/json" \\
  -d '{"email":"user@example.com","password":"correct-password"}'
```

The successful response must be `200 OK` with an empty body.

Also test:

- Wrong password
- Unknown email
- Empty fields
- Malformed JSON
- Unsupported HTTP methods
- Existing registration
- Duplicate registration

A successful Gradle build alone does not prove MySQL connectivity, valid credentials, browser behavior, or successful end-to-end authentication. Those require runtime verification.

## Current Limitations

This phase verifies credentials only. It does not create an authenticated session after login.

There is currently no:

- Session cookie
- Server-side session store
- JWT
- Logout endpoint
- Authorization or protected route
- Account lockout or rate limiting

Those concerns belong to a later authentication or authorization phase if required.
