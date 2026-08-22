# Authentication learning branches

This directory documents each authentication experiment in this repository. The implementations form a progression: start with the public endpoint, compare HTTP Basic with server-side sessions, then move to database users, JWTs, refresh-token revocation, Google OAuth2/OIDC, email verification, and password recovery.

## Branch and ref map

| Topic                           | Ref                                                       | Documentation                                                            |
| ------------------------------- | --------------------------------------------------------- | ------------------------------------------------------------------------ |
| Public baseline                 | `public-access` (`c5abb7e`)                               | [public-access](public-access/README.md)                                 |
| HTTP Basic                      | `http-basic-authentication` (`fab3ba0`)                   | [http-basic-authentication](http-basic-authentication/README.md)         |
| Session cookie                  | `session-cookie-authentication` (`1e5021c`)               | [session-cookie-authentication](session-cookie-authentication/README.md) |
| Session cookie + database       | `session+cookie-authentication-with-database` (`b9224ae`) | [session-cookie-database](session-cookie-database/README.md)             |
| JWT access and refresh tokens   | `jwt-auth` (`41f0601`)                                    | [jwt-auth](jwt-auth/README.md)                                           |
| Refresh rotation and revocation | `refresh-token-rotation-revocation` (`42140de`)           | [refresh-token-rotation](refresh-token-rotation/README.md)               |
| Google OAuth2/OIDC              | `OAuth-2.0-OpenID-Connec-with-Google-login` (`41b627a`)   | [google-oauth2-oidc](google-oauth2-oidc/README.md)                       |
| Email verification              | `email-verification-after-register` (`9f97c6f`)           | [email-verification](email-verification/README.md)                       |
| Forgot and reset password       | `forgot-password+reset-password` (`9cc17b9`)              | [password-recovery](password-recovery/README.md)                         |
| Current combined implementation | `main` (`f838dd3`)                                        | [main](main/README.md)                                                   |

`main` currently points at the latest linear history and contains the combined implementation. The branch-specific READMEs intentionally document the snapshot named by each ref, so use `git switch --detach <ref>` when following an older lesson.

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
