# JWT Authentication — Design Spec
_2026-04-27_

## Goal
Implement a working JWT login chain for a demo. User can register, log in, receive a token, and access a protected endpoint. React frontend shows a login page and a protected dashboard.

---

## Backend

### Dependencies (add to `build.gradle`)
- `org.springframework.boot:spring-boot-starter-security`
- `io.jsonwebtoken:jjwt-api:0.12.6`
- `io.jsonwebtoken:jjwt-impl:0.12.6`
- `io.jsonwebtoken:jjwt-jackson:0.12.6`

### Package structure
```
com.piotrcapecki.bakelivery/
├── controller/
│   └── AuthController.java       POST /api/auth/register, /login, /me
├── service/
│   └── AuthService.java          register + login logic, UserDetailsService impl
├── repository/
│   └── UserRepository.java       findByEmail
├── model/
│   └── User.java                 @Entity: id (UUID), email, passwordHash, role (enum USER/ADMIN)
├── dto/
│   ├── LoginRequest.java         { email, password }
│   ├── RegisterRequest.java      { email, password }
│   └── AuthResponse.java         { token, email }
└── config/
    ├── SecurityConfig.java       permit /api/auth/**, require auth on everything else, CORS
    ├── JwtUtil.java              generate token (HS256, 24h TTL), validate, extract email
    └── JwtAuthFilter.java        OncePerRequestFilter — reads Bearer token, sets SecurityContext
```

### Endpoints
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | public | create user, return token |
| POST | `/api/auth/login` | public | validate credentials, return token |
| GET | `/api/auth/me` | Bearer token | return `{ email }` of logged-in user |

### User entity fields
- `id` — UUID, generated
- `email` — unique, not null
- `passwordHash` — BCrypt
- `role` — enum `Role { USER, ADMIN }`, default USER

### JWT
- Algorithm: HS256
- Expiry: 24 hours
- Secret: stored in `application.yaml` as `jwt.secret` (min 256-bit string)
- Claims: `sub` = email

---

## Frontend

### Setup
- Vite + React in `frontend/` folder
- Proxy `/api` → `http://localhost:8080` in `vite.config.js` (avoids CORS issues during demo)

### Structure
```
frontend/src/
├── pages/
│   ├── LoginPage.jsx       email + password form, calls POST /api/auth/login
│   └── DashboardPage.jsx   shows "Zalogowano jako: {email}", logout button
├── api/
│   └── auth.js             login(email, password), getMe(), logout() — token in localStorage
└── App.jsx                 React Router: / → LoginPage, /dashboard → DashboardPage (guarded)
```

### Auth flow
1. User submits email + password on `/`
2. Frontend calls `POST /api/auth/login`
3. On success: saves token to `localStorage`, navigates to `/dashboard`
4. Dashboard calls `GET /api/auth/me` with `Authorization: Bearer <token>` header
5. Logout: clears `localStorage`, redirects to `/`

---

## Out of scope (for now)
- Refresh tokens
- Role-based access control beyond `USER`/`ADMIN` enum
- Password reset / email verification
- Registration page in frontend (only backend endpoint — demo uses `/register` directly)
