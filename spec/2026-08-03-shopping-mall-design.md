# Shopping Mall — Project Design

## 1. Goal

This is a learning sandbox, not a production build. The prior mentoring feedback was:
implementation is safe/correct but shows no individual judgment — every task was solved
by following the spec, adding defensive error handling, and optimizing, then stopping.
Peers instead tried alternate approaches on parts of the spec and let some of them break.

This project deliberately makes room for that: most of it stays a straightforward,
safely-implemented CRUD flow, but specific points (see backend spec, section 5) are called
out as places to try more than one approach, compare them, and record *why* one was chosen
over the other — including cases where the first attempt breaks.

## 2. Scope (v1 / MVP)

Core flow: browse products → add to cart → place an order. Payment is mocked (order status
flips `PENDING → PAID`, no real PG integration).

Out of scope for v1: real payment gateway, refresh tokens, deployment automation,
reviews/coupons/category hierarchy.

## 3. Structure

```
shoppingMall/
  frontend/        React (Vite) SPA, calls backend REST API
  backend/         Spring Boot API (see backend/spec for details)
  spec/            this folder — project-level specs
```

Kept separate from the existing `spring_study` (petclinic clone) and `spring-petclinic`
folders in this repo — no shared code or dependencies.

## 4. Domain (high level)

- Member — signup/login
- Product — catalog with stock quantity
- Cart / CartItem — one cart per member
- Order / OrderItem — created from cart contents, decrements stock

## 5. API flow (high level)

```
POST /api/members/signup
POST /api/auth/login          -> JWT
GET  /api/products
GET  /api/products/{id}
POST /api/cart/items
PATCH/DELETE /api/cart/items/{id}
POST /api/orders               -> creates order from cart, decrements stock
GET  /api/orders/{id}
```

Full request/response shapes, entity fields, and auth internals live in
`backend/spec/` since they're Spring-specific.

## 6. Frontend

Minimal React (Vite) SPA, plain `fetch` against the API, minimal styling. Purpose is to
exercise the API end-to-end, not to showcase UI/UX design.

## 7. Auth approach

Email + password, hashed with BCrypt, JWT (access token only, short expiry, no refresh
token for v1) issued on login. Implemented directly rather than delegating to an OAuth
provider (e.g. Google), since the point is to understand how the auth flow works
internally, not just wire one up.
