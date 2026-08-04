# Backend — Progress & Roadmap

Snapshot of what's built so far and what's left, tracked separately from the design docs
(`2026-08-03-shopping-mall-design.md`, `backend/spec/2026-08-03-backend-design.md`) since
those describe the target shape, not current status.

## Done

### Member vertical slice (complete, tested end-to-end)
- `Member` entity — Lombok `@Getter` + selective `@Setter` (only `memberName`/`telephone`/
  `email` are mutable; `grade`/`role`/`isDeleted`/`password` are not), `@NoArgsConstructor(PROTECTED)`
  for JPA, business methods `isAdmin()` / `recalculateGrade()`
- `MemberRepository` — `existsByEmail`, `existsByMemberHandler`, `findByEmail`
- `SignUpRequest`/`SignUpResponse`/`MemberResponse` DTOs (records) with Bean Validation
- `SignUp` service — duplicate check at service layer (`existsBy...`) backed by DB
  `unique` constraints as a last line of defense against race conditions
- `DuplicateMemberException` (service-layer duplicate) / `MemberNotFoundException`, plus
  Spring's own `DataIntegrityViolationException` mapped for the DB-level race case
- `GlobalExceptionHandler` (`@RestControllerAdvice`) mapping all of the above to HTTP status
- `MemberController` — `POST /api/members/signup`, `GET /api/members/{id}`
- `SecurityConfig` — `PasswordEncoder` bean (BCrypt); `SecurityFilterChain` currently
  **`permitAll()` on everything** as a placeholder, marked with a `TODO` to replace once
  auth exists (see below)
- Verified via curl/Postman against a real Postgres instance: signup succeeds (201),
  duplicate signup correctly 409s, GET by id 200/404 as expected

### Auth — in progress
- `LoginRequest(email, password)` / `LoginResponse(token)` DTOs, both with `@NotBlank`
  (deliberately no format validation — invalid format just fails lookup/match downstream;
  SQL injection is not a validation concern since Spring Data query methods are always
  parameterized)
- `InvalidCredentialsException` — one exception, one message, reused for both "email not
  found" and "wrong password" to avoid leaking account existence (user enumeration)
- `JwtProvider.generateToken(memberId, role)` — issues a token; claims = memberId + role
  ("B안": accept staleness-until-expiry in exchange for not hitting the DB on every request)
- `Login` service — **skeleton only, not working yet.** Still needs: `PasswordEncoder` +
  `JwtProvider` injected into the constructor, `findByEmail(...).orElseThrow(...)` to get
  the `Member`, a `passwordEncoder.matches(...)` check (same exception/message on failure),
  and a `new LoginResponse(token)` return.

## Remaining roadmap

1. **Finish `Login` service** (in progress now)
2. **`JwtProvider`: add validation/parsing** — `validateToken`, extract memberId/role from
   a token. Nothing calls this yet; it's what the auth filter (next item) will use.
3. **JWT authentication filter** — `OncePerRequestFilter` subclass: read
   `Authorization: Bearer <token>` header, validate via `JwtProvider`, populate the Spring
   Security context so downstream code knows who's calling.
4. **`AuthController`** — `POST /api/auth/login`, calls `Login` service.
5. **Rewire `SecurityConfig`** — replace `anyRequest().permitAll()` with real rules
   (public: signup/login; everything else requires the filter's authentication) and
   register the filter from step 3. This is the step that makes auth actually enforced.
6. **Repeat the Model → Repository → Service → DTO → Controller pattern for `Product`**,
   then `Order`/`Cart`, then (deferred until those exist) `Wishlist`/`WishlistItem`.
7. **Experiment point** (already scoped in `backend/spec/2026-08-03-backend-design.md`
   section 5): optimistic vs pessimistic locking on stock decrement — happens once `Order`
   exists.
