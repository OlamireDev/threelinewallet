# Three Lines Wallet

A Spring Boot wallet service: users register/login with a JWT-backed session,
each new user gets a default NGN wallet, and authenticated users can transfer
funds between wallets and view transaction history.

## Tech stack

- Java / Spring Boot (Spring Security, Spring Data JPA)
- JJWT for token signing/parsing
- Lombok
- JUnit 5 + Mockito + AssertJ for tests

## Architecture

### Auth & session flow

1. `POST /api/v1/auth/register` (or `/login`) issues a JWT via
   `TokenGenerationService`.
2. `UserSessionManagementService` wraps token issuance/validation and tracks
   the *current* active token per user in an in-memory map — so an older,
   still-cryptographically-valid JWT for the same user is rejected once a
   newer session has been issued (single active session per user).
3. `ApplicationFilter` (a `OncePerRequestFilter`) reads the `Authorization:
   Bearer <token>` header on every request, validates it via
   `UserSessionManagementService.decodeSessionToken`, and populates
   `SecurityContextHolder` with the user id as principal.
4. `SecurityConfig` defines two ordered filter chains: `/api/v1/auth/**` and
   `/error` are public; everything else requires a valid session.

### Wallets

- `WalletService.createDefaultWalletForUser` is fired asynchronously
  (`CompletableFuture.runAsync`) right after registration, creating a
  zero-balance NGN wallet for the new user.
- `WalletRepository.findByIdForTransaction` takes a `PESSIMISTIC_WRITE` lock
  (3s timeout) on a single wallet row by id, and is used for both legs of a
  transfer in `TransactionService.doUserTransaction` so concurrent balance
  mutations on the same wallet serialize at the DB row level.
  `findWalletByForUser_IdAndCurrency` (used for read-only lookups like
  `GET /api/v1/wallet`) is unlocked.

### Transactions

- `POST /api/v1/transaction/start` issues a short-lived (5 min), single-use
  `TransactionKeyEntity` — a confirmation step the client must obtain before
  a transfer will be accepted.
- `POST /api/v1/transaction/debit` moves `amount` from `userWalletId` to
  `creditedWalletId`, validating: wallets differ, key is valid/unused/
  unexpired, debit wallet belongs to the caller, both wallets share a
  currency, and the debit wallet has sufficient balance. It writes a paired
  `TransactionEntity` (debit leg + credit leg, cross-linked via
  `linkedTransaction`).
- `POST /api/v1/transaction/history/{walletId}` returns the 100 most recent
  transactions for a wallet the caller owns.

## Getting started

```bash
./mvnw clean install
./mvnw spring-boot:run
```

Configure a datasource and JWT settings (issuer, signing secret/expiry) via
your usual `application.yml`/`application.properties` or environment
variables — see `JwtConfig` and your `spring.datasource.*` properties.

## API documentation

Live, interactive — once the app is running, open
http://localhost:8081/swagger-ui.html. It's generated directly from the
controllers via springdoc-openapi, so it stays
in sync with the actual routes. Click Authorize and paste a bearer
token (from /api/v1/auth/register or /login) to call protected
endpoints. The raw spec is served at /v3/api-docs. Both paths are public
— no token needed just to view the docs.

## Testing

Unit tests live under `src/test/java`, using Mockito for dependencies and
AssertJ for assertions. Notable coverage:

- `AuthServiceTest` — registration, password hashing, duplicate-email
  rejection, async default-wallet-creation trigger.
- `WalletServiceTest` — default wallet creation idempotency, balance lookup.
- `UserSessionManagementServiceTest` — session issuance and single-active-
  session enforcement.
- `TransactionServiceTest` — transfer validation chain, happy-path balance
  movement, insufficient-funds handling, transaction history.
- `TransactionControllerTest`, `WalletControllerTest`, `ApplicationFilterTest`
  — standalone `MockMvc` / filter-level tests.

Run with:

```bash
./mvnw test
```

## Things to Improve

These were found while reviewing recent changes. Items 1, 2, and 4 have since
been fixed in source; 3, 5, and 6 are still open.

1. `UserSessionManagementService`'s session map is an in-memory solution, something redis or database oriented would be ideal.
2. A better way to handle transactions that can account for failure at every point. Maybe adding an isApplied filed to Transaction entities
3. Currency conversion between wallets and creating other wallets
