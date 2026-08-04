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

### Auth (complete through the filter; not yet wired into SecurityConfig)
- `LoginRequest(email, password)` / `LoginResponse(token)` DTOs, both with `@NotBlank`
  (deliberately no format validation — invalid format just fails lookup/match downstream;
  SQL injection is not a validation concern since Spring Data query methods are always
  parameterized)
- `InvalidCredentialsException` — one exception, one message, reused for both "email not
  found" and "wrong password" to avoid leaking account existence (user enumeration)
- `JwtProvider.generateToken(memberId, role)` — issues a token; claims = memberId + role
  ("B안": accept staleness-until-expiry in exchange for not hitting the DB on every request)
- `Login` service — **done.** Injects `PasswordEncoder` + `JwtProvider` +
  `MemberRepository`, `findByEmail(...).orElseThrow(InvalidCredentialsException)`,
  `passwordEncoder.matches(...)` check (same exception/message on failure), returns
  `new LoginResponse(token)`.
- `JwtProvider.parseClaims(token)` — single method, throws (doesn't swallow) on bad
  signature/expiry; chosen over a `validateToken(boolean)` + separate getter pair to avoid
  parsing the same token twice and to keep "valid" and "here are the claims" as one fact
  instead of two that can drift apart.
- `MemberPrincipal(memberId, role)` record — the `Authentication` principal type. Chosen
  over a bare `Long memberId` because Cart/Order/Product endpoints will need `role` on
  hand for authorization checks without re-parsing the token or hitting the DB.
- `JwtAuthenticationFilter` (`OncePerRequestFilter`, `@Component`) — **done.** Reads
  `Authorization: Bearer <token>`, on missing/invalid header or failed parse just calls
  `filterChain.doFilter(...)` and returns *unauthenticated* (no 401 thrown here — that
  decision is `SecurityConfig`'s job, not the filter's); on success builds
  `MemberPrincipal` + `ROLE_<role>` authority and sets it on
  `SecurityContextHolder`. Gotcha hit while building this: `SimpleGrantedAuthority` must
  be prefixed exactly `"ROLE_"` (uppercase) — Spring's `hasRole(...)` matches on that
  prefix literally, silently always-false otherwise.

## Remaining roadmap

1. **`AuthController`** — `POST /api/auth/login`, calls `Login` service.
2. **Rewire `SecurityConfig`** — replace `anyRequest().permitAll()` with real rules
   (public: signup/login; everything else requires the filter's authentication) and
   register `JwtAuthenticationFilter` (already a `@Component`, so autowire it into
   `SecurityConfig` and `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`).
   This is the step that makes auth actually enforced end-to-end.
3. **Repeat the Model → Repository → Service → DTO → Controller pattern for `Product`**
   (simplest — no auth needed for reads), then `Cart`/`CartItem` (needs the authenticated
   `memberId` from step 2, so must come after it), then `Order`/`OrderItem`.
   - `Order` creation: read the caller's cart, create `Order` + `OrderItem`s (with
     `priceAtOrder` snapshotted at order time, not looked up later), decrement
     `Product.stockQuantity`, all in one transaction — build this first with a plain
     unlocked update, defer the locking strategy to the experiment below.
4. **Experiment point** (scoped in `backend/spec/2026-08-03-backend-design.md` section 5):
   optimistic vs pessimistic locking on stock decrement. **Decision (2026-08-04): do both**,
   not just one — this is the project's one deliberately-not-skipped experiment. Needs an
   integration test firing concurrent order requests at limited stock (e.g. stock=5,
   10 concurrent) and a written comparison of what actually happened under each strategy.
5. **Frontend**: only the backend-integration parts (fetch calls, request/response
   shapes, auth token handling) get the same tutor-and-review treatment as the backend.
   Layout/styling/component structure is out of that scope by the user's own choice
   ("vibe-coded") — not reviewed here.
- `Wishlist`/`WishlistItem` — deferred indefinitely; not in the v1 scope per the root
  design doc (`spec/2026-08-03-shopping-mall-design.md` section 2), listed here only as a
  stretch item if time remains after the above.
