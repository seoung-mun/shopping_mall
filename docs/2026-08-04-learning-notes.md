# Learning Notes — Q&A Log

Things I didn't know / asked about while building the Member + Auth slice, organized by
topic rather than chronologically. Meant as a personal reference, not a design doc — the
design decisions themselves live in `spec/`.

## Lombok & entities

- `@Getter` (class-level) generates getters for every field, including correctly naming a
  boolean field like `isDeleted` as `isDeleted()` (not `getIsDeleted()`).
- `@Setter` (field-level) applied selectively — only put it on fields that are legitimately
  mutable after creation (`memberName`, `telephone`, `email`). Fields like `grade`/`role`/
  `password`/`isDeleted` have no setter; they change only through dedicated methods
  (`recalculateGrade()`) or not at all from outside.
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` — JPA needs a no-arg constructor to
  proxy entities, but making it `public` would let anyone construct an empty, invalid
  `Member`. `PROTECTED` satisfies JPA while blocking that.
- Import collision trap: `@Value("${...}")` on a constructor parameter needs
  `org.springframework.beans.factory.annotation.Value` (config injection). Lombok also has
  a `@Value` (class-level immutable value object, like a lighter `record`). IDE
  autocomplete can silently grab the wrong one — it fails to compile since Lombok's
  `@Value` isn't valid on a parameter, but it's easy to not notice which one got imported.

## Records as DTOs

- Record accessors have **no `get` prefix** — `request.email()`, not `request.getEmail()`.
  Easy to typo when muscle memory is from regular getters.
- DTOs are plain data holders — never Spring beans, never injected via constructor.

## Bean Validation

- `@Valid` on a controller parameter is what actually triggers validation — the
  annotations on the DTO fields do nothing without it.
- `@NotBlank` only applies to `CharSequence` types, not primitives.
- Format validation (`@Email`, `@Pattern`) is worth it on **signup** (first time the data
  enters the system) but redundant on **login** — an invalid-format email just fails the
  lookup downstream anyway, so login DTOs only need `@NotBlank` (guard against null/blank
  reaching the service layer).

## Exceptions

- Two different categories, easy to conflate:
  - **Manually-detected** duplicates (service-layer `existsBy...` check before insert) →
    custom exception (`DuplicateMemberException`)
  - **DB-level race condition** (two requests slip past the service check simultaneously,
    unique constraint catches it at insert time) → Spring's own
    `org.springframework.dao.DataIntegrityViolationException`, which Hibernate throws
    automatically — never thrown manually, and should never be shadowed by a custom class
    of the same name (did this once, caused confusion about which one was "real").
- `@RestControllerAdvice` + `@ExceptionHandler` centralizes exception → HTTP status mapping
  in one place instead of try/catch in every controller method.

## `Optional`

- `Optional<T>` models "might legitimately be absent" as part of normal flow; exceptions
  model abnormal flow. `findById(id).orElseThrow(...)` chains the two together in one line,
  which is why the `Optional` is easy to miss when reading the code — it's never assigned
  to an intermediate variable.

## Controllers, DTOs, entities

- Controllers should never accept or return raw entities directly from an untrusted client
  — mass-assignment risk (a signup request could theoretically set fields like `role` or
  `isDeleted` if the entity were bound directly) and unwanted field exposure (e.g. leaking
  `password` in a response). DTOs exist specifically to define the actual public contract.
- PetClinic's `@ModelAttribute Owner` pattern (binding the entity directly) gets away with
  this because it's server-rendered MVC with no sensitive fields on `Owner` and no public,
  untrusted signup flow — not because the entity-binding pattern is generally safe.

## Dependency injection

- Constructors are for wiring dependencies only — no business logic inside them.

## Security design (login)

- **Generic error message on login failure** ("이메일 또는 비밀번호가 일치하지 않습니다"
  for both "email not found" and "wrong password") exists specifically to prevent **user
  enumeration**: if the messages differed, an attacker could probe emails one at a time and
  use the response difference to build a list of which emails are actually registered —
  useful for credential stuffing and, separately, a privacy leak on its own (confirming
  someone uses a given service).
  - Full defense also covers response-time consistency (BCrypt matching takes tens of ms;
    skipping straight to "not found" without that delay leaks the same info via timing) —
    not implemented yet, but worth knowing the message alone isn't a complete fix.
- SQL injection isn't something Bean Validation needs to guard against here: Spring Data
  JPA query-derivation methods (`findByEmail`, `existsByEmail`, ...) always compile to
  parameterized queries (`?` placeholders), so untrusted input can't alter query structure.
  This would only become a real risk if a query were built by string concatenation
  (`@Query` with manual string building, or a native query assembled from raw input) —
  not present anywhere in this codebase.

## JWT (jjwt 0.12.x)

- API changed from older jjwt versions — fluent builder: `Jwts.builder().subject(...)
  .claim(...).issuedAt(...).expiration(...).signWith(key).compact()`. Old tutorials using
  `Jwts.builder().setSubject(...)` etc. are for a different (older) API version.
- HS256 requires a secret key of at least 256 bits (32 bytes) — too short a key throws at
  startup, not at token-generation time.
- `@Value("${jwt.secret}")` / `@Value("${jwt.expiration-ms}")` inject config values into a
  constructor — different from `@Autowired`, which injects beans.
- Claims design trade-off (memberId-only vs memberId+role): embedding `role` avoids a DB
  lookup on every authenticated request, at the cost of a role change not taking effect
  until the old token expires. Chose memberId+role for this project ("B안").

## JWT parsing & the auth filter

- `Jwts.parser()` returns a *builder* (`JwtParserBuilder`), not `Claims` — easy first
  mistake. Full chain: `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
  .getPayload()`. `verifyWith(key)` is the mirror of `signWith(key)` used when generating
  the token — same `SecretKey`, verification checks the signature matches.
  `parseSignedClaims` is where signature/expiry are actually checked and where it throws.
- API design choice: `parseClaims(token)` as one method that throws on failure, instead of
  a `validateToken(token): boolean` + separate `getMemberId`/`getRole` getters. The
  boolean+getters split is a common tutorial pattern but parses the same token twice (once
  to validate, again per getter) and splits "is this valid" from "here are the values"
  into two calls that can drift out of sync. One parse, one `Claims` result, exceptions
  propagate to whoever calls it (the filter) to decide what to do.
- Deliberately **not** catching the parse exception inside `JwtProvider` — catching it
  there would hide *which* exception subtype occurred (`ExpiredJwtException` vs
  `SignatureException` vs `MalformedJwtException`) from the caller, who may want to react
  differently later.
- `OncePerRequestFilter`: guarantees a filter runs exactly once per request, unlike a raw
  `Filter`, which can re-run on internal forwards/includes. Right base class for
  auth logic, which must not double-run.
- The filter never rejects a request itself (no 401 thrown for a missing/invalid token) —
  it only *populates* `SecurityContextHolder` when a token is valid, and otherwise lets
  the request continue unauthenticated. Deciding which endpoints *require* authentication
  is `SecurityConfig`'s job, not the filter's — keeps the two concerns separate so the
  filter doesn't need to know the app's URL/permission rules.
- `SimpleGrantedAuthority` role strings must be prefixed exactly `"ROLE_"` (uppercase).
  Spring Security's `hasRole("ADMIN")` silently checks for `"ROLE_ADMIN"` under the hood —
  get the prefix's case or spelling wrong and `hasRole` just always returns false, no error
  anywhere to point at it.
- `UsernamePasswordAuthenticationToken` isn't only for username/password login — it's a
  general-purpose `Authentication` implementation, commonly reused to represent "already
  authenticated by some other means" (here, a validated JWT). The `credentials` argument
  (2nd constructor param) is `null` here since there's nothing left to verify at that
  point.
- Principal design choice: a custom `MemberPrincipal(memberId, role)` record instead of a
  bare `Long memberId`. Since Cart/Order/Product endpoints will repeatedly need `role` for
  authorization checks, bundling it into the principal avoids re-parsing the token or
  hitting the DB just to answer "is this an admin?" on every request. Also considered:
  implementing Spring's full `UserDetails` — rejected, since that interface's usual
  contract implies a DB-backed lookup, which conflicts with the project's already-chosen
  "trust the JWT claims, accept staleness until expiry" trade-off.
- Claim key names (`"role"`) are plain string literals duplicated between
  `JwtProvider.generateToken` (writes it) and the filter (reads it) — nothing at compile
  time enforces they match. Fine at two occurrences; worth promoting to a shared constant
  if more claims get added later.

## Local dev environment debugging

- `lsof -nP -iTCP:<port> -sTCP:LISTEN` — find what's listening on a port.
- `ps -p <PID> -o command` — identify what a given PID actually is.
- A Docker named volume persists across `docker compose up` runs; if it was initialized
  once with different credentials, Postgres won't re-run its init scripts on a later `up`
  even after `docker-compose.yml` changes — has to be `docker compose down -v` (destroys
  the volume) to get a clean re-init.
- On macOS, a native Homebrew Postgres service and a Docker Postgres container can both
  bind port 5432 simultaneously without erroring at startup — the native one binds
  `127.0.0.1`/`::1` specifically, which wins over Docker's `*:5432` for any connection made
  via `localhost`, silently shadowing the container. `docker exec ... psql` bypasses the
  network stack entirely and hits the container correctly, which is what made this
  confusing (same "role does not exist" error, but only when connecting via `localhost`).
- Adding `spring-boot-starter-security` to the classpath auto-locks every endpoint (401)
  the moment there's no custom `SecurityFilterChain` bean — confirmed via a "Using
  generated security password" log line. Needs an explicit (even if temporarily permissive)
  `SecurityFilterChain` bean to unblock testing.

## Testing tooling

- Postman: GUI, collections exportable as JSON for git (belongs under `backend/`, not the
  monorepo root, since it's backend-API-specific).
- curl: scriptable, good for automation/smoke tests (`-X`, `-H`, `-d`, `-s`, `-w
  "%{http_code}"`, `-i`, `-v`).
- `@WebMvcTest` + `MockMvc`: no real server/DB needed, fast, CI-friendly — different
  purpose from manual Postman/curl testing (automated regression vs exploratory/manual).
