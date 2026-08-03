# Shopping Mall Backend — Spring Design

Project-level goals and scope live in the root `shoppingMall/spec/` design doc. This
covers the Spring-specific implementation.

## 1. Tech stack

- Spring Boot + Spring Web (REST) + Spring Data JPA
- Spring Security + JWT (custom filter, no OAuth)
- PostgreSQL, run via docker-compose locally
- Gradle, Java 21 (matches `spring_study`)

## 2. Entities

- `Member` — id, memberHandler (unique, public-facing), memberName (real name, for
  shipping), telephone, email (unique), passwordHash, address (optional, filled in at
  first order), grade (enum: BRONZE/SILVER/GOLD/PLATINUM/VIP, recalculated from lifetime
  purchase total), role (enum: USER/ADMIN — ADMIN acts as seller, USER as buyer-only;
  simplified from a Seller/Buyer composition model), created, isDeleted
- `Product` — id, name, price, stockQuantity
- `Cart` — id, member (1:1)
- `CartItem` — id, cart, product, quantity
- `Order` — id, member, status (`PENDING` / `PAID`), createdAt
- `OrderItem` — id, order, product, quantity, priceAtOrder

## 3. Auth implementation

- `POST /api/members/signup` — hash password with BCrypt, store Member
- `POST /api/auth/login` — verify credentials, issue JWT access token (short expiry,
  e.g. 30 min; no refresh token in v1 — noted as a deliberate scope cut, not an oversight)
- Custom `OncePerRequestFilter` validates the JWT and populates the security context
- Passwords, hashing, and token validation are hand-rolled — this is the point of doing
  it directly instead of delegating to Google OAuth

## 4. Order flow & stock decrement

`POST /api/orders` reads the caller's cart, creates an `Order` + `OrderItem`s from its
contents, and decrements `Product.stockQuantity` for each line — all in one transaction.

## 5. Experiment point: concurrent stock decrement

This is the deliberate "try more than one approach" part of the project (see root spec
section 1). Implement stock decrement twice, on separate branches or behind a toggle:

1. **Optimistic locking** — `@Version` on `Product`, catch
   `OptimisticLockingFailureException`, retry or fail the order.
2. **Pessimistic locking** — `SELECT ... FOR UPDATE` via
   `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the product read.

Write an integration test that fires concurrent order requests against the same product
with limited stock (e.g. stock = 5, 10 concurrent requests) and observe what actually
happens under each strategy — including failures/retries, not just the happy path.
Record the outcome and the final choice (with reasoning) in this doc once done.

## 6. Error handling & testing

- Standard error response shape via `@RestControllerAdvice` (validation errors, 404s,
  auth failures) — kept conventional, this isn't an experiment point
- Slice tests (`@DataJpaTest`, `@WebMvcTest`) for repositories/controllers
- The concurrency experiment (section 5) gets its own integration test with real
  concurrent requests, since that's the part meant to be verified by observation rather
  than assumed safe

## 7. Local setup

`docker-compose.yml` in `backend/` brings up PostgreSQL. `application.yml` profiles:
`local` (docker postgres) and `test` (can stay on H2 for fast slice tests, since the
concurrency experiment is the only place real Postgres locking behavior matters).
