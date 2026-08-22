# Authentication learning

This document combines the guides for every authentication experiment in this repository into a single reference. The implementations form a progression: start with the public endpoint, compare HTTP Basic with server-side sessions, then move to database users, JWTs, refresh-token revocation, Google OAuth2/OIDC, email verification, and password recovery.

## Branch and ref map

| Topic                           | Ref                                                       | Section                                                                                  |
| ------------------------------- | --------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| Public baseline                 | `public-access` (`c5abb7e`)                               | [1. Public access baseline](#1-public-access-baseline)                                   |
| HTTP Basic                      | `http-basic-authentication` (`fab3ba0`)                   | [2. HTTP Basic authentication](#2-http-basic-authentication)                             |
| Session cookie                  | `session-cookie-authentication` (`1e5021c`)               | [3. Session-cookie authentication](#3-session-cookie-authentication)                     |
| Session cookie + database       | `session+cookie-authentication-with-database` (`b9224ae`) | [4. Database-backed session cookies](#4-database-backed-session-cookies)                 |
| JWT access and refresh tokens   | `jwt-auth` (`41f0601`)                                    | [5. JWT authentication](#5-jwt-authentication)                                           |
| Refresh rotation and revocation | `refresh-token-rotation-revocation` (`42140de`)           | [6. Refresh-token rotation and database revocation](#6-refresh-token-rotation-and-database-revocation) |
| Google OAuth2/OIDC              | `OAuth-2.0-OpenID-Connec-with-Google-login` (`41b627a`)   | [7. Google OAuth2 and OpenID Connect login](#7-google-oauth2-and-openid-connect-login)   |
| Email verification              | `email-verification-after-register` (`9f97c6f`)           | [8. Email verification after registration](#8-email-verification-after-registration)     |
| Forgot and reset password       | `forgot-password+reset-password` (`9cc17b9`)              | [9. Forgot and reset password](#9-forgot-and-reset-password)                             |
| Current combined implementation | `main` (`f838dd3`)                                        | [10. Current combined implementation](#10-current-combined-implementation)               |

`main` currently points at the latest linear history and contains the combined implementation. Each numbered section below intentionally documents the snapshot named by its ref, so use `git switch --detach <ref>` when following an older lesson.

## Common setup

Prerequisites: Java 21, Node.js/npm, and a terminal opened at the repository root.

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

In another terminal:

```powershell
cd frontend
npm install
npm run dev
```

The backend listens on `http://localhost:8080`; Vite listens on `http://localhost:5173`. The default database is an in-memory H2 database named `authdb`, so data disappears when the backend stops. The H2 console is available at `http://localhost:8080/h2-console` for local learning only.

## Shared security cautions

- Do not use the demo secrets, H2 console, permissive CORS, or HTTP credentials in production.
- Use HTTPS for any credential or token flow.
- Keep JWT, OAuth, SMTP, and database credentials outside source control.
- Validate request bodies and use generic responses for registration and password-reset requests to reduce account enumeration.
- Review token expiry, one-time use, revocation, session invalidation, and account-linking behavior before treating an experiment as production-ready.

---

## 1. Public access baseline

Ref: `public-access` (`c5abb7e`)

This is the starting point before authentication is introduced. It is useful as a control case: compare an unrestricted request with the authorization rules added in later refs.

### Run

Use the [common setup](#common-setup). The public API is:

```text
GET http://localhost:8080/api/public
```

No username, password, cookie, token, database user, or external provider is required. The frontend can call the endpoint directly.

### What to learn

The important change in the next lesson is not the controller itself; it is the `SecurityFilterChain`. Once security is enabled, routes must be explicitly permitted or authenticated, and tests should cover both anonymous and authenticated requests.

---

## 2. HTTP Basic authentication

Ref: `http-basic-authentication` (`fab3ba0`)

This lesson adds Spring Security HTTP Basic authentication. The client sends an `Authorization: Basic <base64(username:password)>` header on each protected request. There is no application login endpoint and no server-side login session.

### Run and try it

Follow the [common setup](#common-setup), then call:

```powershell
curl http://localhost:8080/api/public
curl -u user:password http://localhost:8080/api/profile
curl -u admin:admin123 http://localhost:8080/api/admin
```

The exact in-memory users and roles are defined in the historical `security/SecurityConfig.java`. The profile request in `frontend/src/App.jsx` demonstrates constructing the same header with `btoa`.

### Concepts

- `SecurityFilterChain` enables `.httpBasic(...)` and protects non-public routes.
- `InMemoryUserDetailsManager` supplies users for the exercise.
- Roles protect `/api/admin`; authenticated users can access `/api/profile`.
- Basic authentication is stateless from the server's point of view, but the browser retains and resends credentials.

### Limitations

Basic credentials are reusable and must only cross an HTTPS connection. In-memory users are not suitable for a real application, and logout is generally a client credential-discard operation rather than server-side token revocation. This branch is intentionally a minimal comparison point for the session-cookie branch.

---

## 3. Session-cookie authentication

Ref: `session-cookie-authentication` (`1e5021c`)

This lesson replaces repeated Basic credentials with a server-side HTTP session. The login request authenticates username and password, Spring Security stores the `SecurityContext` in the session, and subsequent requests use the `JSESSIONID` cookie.

### Flow

1. `GET /auth/csrf` obtains a CSRF token and sets the cookie used by the client.
2. `POST /auth/login` accepts `{ "username": "...", "password": "..." }`.
3. The client sends `credentials: "include"` on protected requests.
4. `GET /api/profile` uses the authenticated session.
5. `POST /api/profile/update` demonstrates CSRF protection for a state-changing request.
6. `POST /auth/logout` invalidates the session.

Other routes are `GET /api/public` and role-protected `GET /api/admin`.

### Implementation

`controller/session_cookie/AuthController.java` calls `AuthenticationManager`, places the resulting authentication in `SecurityContextHolder`, and saves it under `SPRING_SECURITY_CONTEXT_KEY` in the HTTP session. `SecurityConfig.java` configures `CookieCsrfTokenRepository.withHttpOnlyFalse()`, CORS for `http://localhost:5173`, and session logout.

### Try it

Use the [common setup](#common-setup), then use the frontend buttons or send requests with a cookie jar. The backend still uses the simple learning-stage user source from this ref; database-backed users arrive in the next snapshot.

### Limitations

CSRF tokens are required because authentication is cookie-based. The H2 console and broad local CORS settings are for learning only. The manual security-context persistence is educational and should be compared with Spring Security's standard authentication flow before reuse.

---

## 4. Database-backed session cookies

Ref: `session+cookie-authentication-with-database` (`b9224ae`)

This lesson keeps the session-cookie protocol and replaces the in-memory identity store with JPA/H2 persistence.

### Flow and endpoints

- `POST /auth/register` creates a user with role `USER` and a BCrypt password.
- `POST /auth/login` authenticates through `AuthenticationManager` and creates the session.
- `GET /auth/csrf` returns the CSRF token.
- `POST /auth/logout` clears authentication and invalidates the session.
- `GET /api/public` is anonymous.
- `GET /api/profile` requires authentication.
- `POST /api/profile/update` requires authentication and CSRF.
- `GET /api/admin` requires `ROLE_ADMIN`.

### Implementation

`AppUser` maps to the `users` table with `id`, unique `username`, BCrypt `password`, and `role`. `UserRepository` provides `findByUsername` and `existsByUsername`; `CustomUserDetailsService` loads users for Spring Security. `SecurityConfig.java` owns authorization, CORS, CSRF, password encoding, and logout.

### Run

Use the [common setup](#common-setup). Register through the React UI or send JSON to `/auth/register`, fetch a CSRF token before every state-changing request, and preserve cookies with `credentials: "include"`.

### Limitations

The default H2 database is in memory. There is no input validation, email verification, or password-recovery flow in this snapshot. The frontend's older profile call still contains a Basic header even though this branch's intended mechanism is the session cookie.

---

## 5. JWT authentication

Ref: `jwt-auth` (`41f0601`)

This lesson changes protected API requests from session cookies to stateless JSON Web Tokens. Login authenticates credentials and returns an access token plus a refresh token; the client sends the access token as `Authorization: Bearer <token>`.

### Endpoints

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /api/public`
- `GET /api/profile` and `POST /api/profile/update`
- `GET /api/admin` for `ROLE_ADMIN`

Login and refresh response/request shapes are defined by the branch's DTOs and `AuthController.java`; inspect them when practicing rather than assuming cookie or CSRF behavior from the previous lesson.

### Implementation

`JwtService.java` creates signed tokens. `SecurityConfig.java` configures the resource server and JWT decoder/converter. `AppUser` remains database-backed. The frontend updates its API calls to hold and attach bearer tokens instead of relying on a login session.

### Configuration

The branch reads `jwt.secret`. Supply a long random secret through an external configuration source, for example `JWT_SECRET` mapped to `jwt.secret`; never use the sample literal committed in the learning snapshot.

### Security lessons

JWT logout cannot retract an already-issued access token unless a revocation strategy exists. Keep access tokens short-lived, protect refresh tokens, avoid browser local storage where possible, and define rotation and reuse handling. This is the reason for the next branch.

---

## 6. Refresh-token rotation and database revocation

Ref: `refresh-token-rotation-revocation` (`42140de`)

This lesson extends the JWT branch with a persistent refresh-token record. A refresh operation issues a new refresh token and revokes the previous one, allowing the server to reject replayed or logged-out refresh tokens.

### Flow

1. `POST /auth/login` returns an access token and refresh token.
2. The client calls `POST /auth/refresh` with the refresh token when the access token expires.
3. `RefreshTokenService` validates ownership, expiry, and revocation, then rotates the token.
4. `POST /auth/logout` revokes the server-side refresh-token state.
5. Protected API calls use `Authorization: Bearer <access-token>`.

The public, profile, update, and admin endpoints remain from the JWT lesson.

### Implementation

`RefreshToken` is the new entity, with its repository and `RefreshTokenService`. `AppUser` and `UserRepository` remain the identity store; `JwtService` continues to sign access and refresh JWTs. The exact request and response records are in the branch's `AuthController.java` and DTOs.

### Configuration and run

Use the [common setup](#common-setup), and provide an external `jwt.secret`. H2 stores revocation state only for the lifetime of the process unless its datasource is changed.

### Security lessons

Rotation is useful only when old tokens are rejected and concurrent refresh behavior is defined. Prefer storing a hash of an opaque refresh token rather than a raw bearer value, detect reuse, expire records, and invalidate all relevant tokens after a password change.

---

## 7. Google OAuth2 and OpenID Connect login

Ref: `OAuth-2.0-OpenID-Connec-with-Google-login` (`41b627a`)

This lesson adds Google as an OpenID Connect identity provider while retaining JWT-protected API access. Spring Security handles the authorization-code exchange and a custom success handler maps the Google identity to the local `AppUser` model.

### Browser flow

1. Configure a Google OAuth client with the backend callback URI.
2. Navigate to `http://localhost:8080/oauth2/authorization/google`.
3. Google authenticates and redirects to `/login/oauth2/code/google`.
4. `OAuth2LoginSuccessHandler` handles the authenticated `OAuth2User` and redirects/establishes the application's local authentication flow.
5. Use the resulting application token for `/api/profile` and other protected APIs.

### Configuration

Set these environment variables before starting the backend:

```text
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
JWT_SECRET=<long random value>
```

The branch requests scopes `openid,profile,email`. Register the exact redirect URI with Google. Do not commit client secrets or use the sample JWT secret.

### Implementation

`SecurityConfig.java` enables both `oauth2Login` and JWT resource-server support. `OAuth2LoginSuccessHandler.java` owns post-login behavior; `AppUser` stores provider-related identity information. The frontend includes a Google login action that navigates to the authorization endpoint.

### Security lessons

Validate the issuer, audience, redirect URI, state, nonce, and verified-email policy. Account linking by email needs an explicit trust rule. Keep provider secrets server-side and use HTTPS outside localhost.

---

## 8. Email verification after registration

Ref: `email-verification-after-register` (`9f97c6f`)

This lesson adds an email-verification gate to the combined JWT/OAuth implementation. A newly registered account receives a one-time verification link and cannot complete normal login until its email is verified.

### Endpoints and flow

- `POST /auth/register` creates the account and sends a verification email.
- `GET /auth/verify-email` consumes the verification token and marks the account verified.
- `POST /auth/login` checks the verified state before issuing tokens.
- `POST /auth/refresh` and `POST /auth/logout` retain the JWT lifecycle.
- Profile, update, public, and admin endpoints retain their previous authorization rules.

The verification token parameter and response wording are defined in `AuthController.java`; inspect that mapping when trying requests manually.

### Implementation

`EmailVerificationToken` and its repository persist the token and expiry. `EmailVerificationService` creates and consumes tokens. `EmailService` sends mail through Spring's `JavaMailSender`; `AppUser` gains verification state.

### Configuration

Set SMTP credentials externally:

```text
MAIL_USERNAME=...
MAIL_PASSWORD=...
JWT_SECRET=<long random value>
```

The branch configures Gmail SMTP on port 587 with STARTTLS. Use a provider app password where required. H2 remains in memory by default.

### Security lessons

Verification tokens should be random, single-use, short-lived, and absent from logs. Return generic registration responses where account enumeration matters, and make the verification URL use a trusted configured origin.

---

## 9. Forgot and reset password

Ref: `forgot-password+reset-password` (`9cc17b9`)

This lesson adds email-based password recovery to the JWT, OAuth, and email-verification flow.

### Endpoints and flow

1. `POST /auth/forgot-password` accepts the account identifier and sends a reset link.
2. The user opens the link and submits a new password.
3. `POST /auth/reset-password` validates the token, validates the new password, hashes it with BCrypt, and invalidates the reset token.
4. Existing JWT refresh state should be revoked after a successful reset; verify the branch behavior when experimenting.

The branch also retains `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/verify-email`, and the public/profile/admin endpoints.

### Implementation

`PasswordResetToken` and `PasswordResetTokenRepository` persist reset state. `PasswordResetService` owns creation and consumption. `ForgotPasswordRequest` and `ResetPasswordRequest` define the payloads, while `PasswordValidator` checks the replacement password. `EmailService` sends the reset message.

### Configuration and run

Set `MAIL_USERNAME`, `MAIL_PASSWORD`, and an external `JWT_SECRET`, then use the [common setup](#common-setup). Reset links depend on the configured local mail server/provider; H2 token state disappears on restart.

### Security lessons

Always return a generic forgot-password response, use random single-use expiring tokens, prefer hashed token storage, avoid logging links, validate the destination origin, and revoke existing sessions/tokens after reset. Treat password-reset email access as equivalent to account access.

---

## 10. Current combined implementation

Ref: `main` (`f838dd3`)

`main` is the current learning endpoint: it contains the latest combined authentication implementation from the linear history. Read the focused lesson sections above to study each change in isolation, then use this guide to run the complete application.

### Included mechanisms

- Database-backed users with BCrypt passwords.
- JWT access and refresh tokens.
- Database-backed refresh-token rotation and revocation.
- Google OAuth2/OpenID Connect login.
- Email verification after registration.
- Forgot-password and reset-password email flow.
- Role-based authorization for `/api/admin`.
- A React client for the authentication flows.

### Main endpoints

```text
POST /auth/register
POST /auth/login
POST /auth/refresh
POST /auth/logout
GET  /auth/verify-email
POST /auth/forgot-password
POST /auth/reset-password
GET  /oauth2/authorization/google
GET  /api/public
GET  /api/profile
POST /api/profile/update
GET  /api/admin
```

JWT-protected API calls use `Authorization: Bearer <access-token>`. Google login uses Spring Security's callback at `/login/oauth2/code/google`.

### Configuration

Provide these values through environment variables or an equivalent external configuration source:

```text
JWT_SECRET=<long random value>
GOOGLE_CLIENT_ID=<Google OAuth client id>
GOOGLE_CLIENT_SECRET=<Google OAuth client secret>
MAIL_USERNAME=<SMTP username>
MAIL_PASSWORD=<SMTP password or app password>
```

Google's registered redirect URI must match the local backend callback. Gmail SMTP is configured for port 587 with STARTTLS.

### Run

Follow [common setup](#common-setup). The default H2 database is in memory, so users and token revocation records disappear after restart. Use the frontend at `http://localhost:5173` and the backend at `http://localhost:8080`.

### Important learning gaps

This is an educational composition, not a production security baseline. Review token hashing and reuse detection, password-reset session invalidation, account-linking rules, request validation, secret management, HTTPS, and the permissive local CORS/H2-console configuration before adapting it.
