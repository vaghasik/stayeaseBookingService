# StayEase - Hotel Room Booking (Backend Service)

A hotel room booking web application where guests browse hotels, check room availability for selected
dates, and confirm bookings. Hotel managers manage their rooms and bookings; admins manage the hotel
directory. This repository is the **backend service** — a single Spring Boot application (UI → service → DB).

> Design docs: [HLD](./docs/HLD.md) · [Backend components](./docs/BACKEND_COMPONENTS.md) · [Frontend components](./docs/FRONTEND_COMPONENTS.md) · [Deployment](./docs/Deployment.md)

---

## Tech Stack

- **Java 21 or above**, **Spring Boot 4** (Spring MVC — blocking)
- **Spring Data JPA / Hibernate**, Bean Validation
- **H2** (in-memory) for `local`; **PostgreSQL** for `dev` / `prod`
- Auth: Spring Security + BCrypt + stateless **JWT**
- Build: **Maven** · Java 21

---

## Prerequisites

- **JDK 21+**
- **Maven 3.9+** (or use your IDE's bundled Maven)

Check:
```bash
java -version
mvn -version
```

---

## Getting Started

### 1. Clone & build
```bash
git clone <repo-url>
cd Capstone_FSEJAVA_Team45_stayease-hotelbooking-service
mvn clean install
```

### 2. Run (local profile — H2, seed data auto-loaded)
```bash
mvn spring-boot:run
```
The service starts on **http://localhost:8080**. The `local` profile is active by default, the schema
and seed data load automatically, and the H2 web console is enabled.

### 3. Run from IntelliJ / with a specific profile
Set the active profile via a VM option or env var:
```
# VM option
-Dspring.profiles.active=local

# or environment variable
SPRING_PROFILES_ACTIVE=local
```
Other profiles:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev    # PostgreSQL (requires a running Postgres)
mvn spring-boot:run -Dspring-boot.run.profiles=prod   # PostgreSQL via env vars
```

### 4. Run the packaged jar
```bash
mvn clean package
java -jar target/stayease-hotel-booking-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

**Run order (full stack):** Start DB → run this service → seed data → run the UI.
For `local`, the DB (H2) and seed data are automatic.

---

## Database & H2 Console (local)

Seed data + schema live in `src/main/resources/sql/` (`schema.sql`, `data.sql`) and load on startup in
the `local` profile.

Open the H2 web console:

| Setting | Value |
|---------|-------|
| URL | http://localhost:8080/h2-console |
| JDBC URL | `jdbc:h2:mem:stayease` |
| User | `sa` |
| Password | *(empty)* |

> Spring Boot 4 removed the built-in H2 console auto-config, so it is registered manually in
> `H2ConsoleConfig` (local profile only).

### Seed accounts
All seed accounts use the password **`password`** (BCrypt-hashed in the seed data).

| Role | Email | Notes |
|------|-------|-------|
| ADMIN | `admin@stayease.com` | Manages the hotel directory |
| MANAGER | `mgr1@stayease.com` | Manages Sunset Palms, City Inn (Goa), Garden Residency (Bengaluru) |
| MANAGER | `mgr2@stayease.com` | Manages Marine Bay Hotel (Mumbai) |
| GUEST | `guest1@stayease.com` | Has past + future bookings |
| GUEST | `guest2@stayease.com` | Has a cancelled + future booking |

Seed covers 4 hotels (Goa ×2, Mumbai, Bengaluru), 10 rooms (incl. one inactive), and 5 bookings
(incl. a cancelled one) so every acceptance-criteria scenario is testable.

---

## API Documentation (Swagger / OpenAPI)

| URL | Description |
|-----|-------------|
| http://localhost:8080/swagger-ui/index.html | Swagger UI (Interactive API Explorer) |
| http://localhost:8080/v3/api-docs | OpenAPI 3.0 JSON Spec |

> **Tip:** Use the **"Authorize"** button in Swagger UI to set your JWT token for protected APIs.

---

## API Endpoints

Base URL: `http://localhost:8080`  ·  All request/response bodies are JSON.

### Auth
| Method | Endpoint | Access | Body / Notes |
|--------|----------|--------|--------------|
| POST | `/api/auth/register` | Public | `{ "email", "password", "name" }` → 201 |
| POST | `/api/auth/login` | Public | `{ "email", "password" }` → 200 |
| POST | `/api/auth/logout` | Auth | → 204 |

> Auth response shape (flat, matches the UI's `AuthResponse` type):
> `{ "token", "type", "tokenType", "userId", "email", "name", "role" }`

### Hotels
| Method | Endpoint | Access | Notes |
|--------|----------|--------|-------|
| GET | `/api/hotels?city=` | Public | List hotels in a city |
| GET | `/api/hotels/all` | ADMIN | List every hotel, regardless of city |
| GET | `/api/hotels/{id}` | Public | Hotel details |
| GET | `/api/hotels/{id}/rooms?checkIn=&checkOut=` | Public | Available rooms for the date range |
| POST | `/api/hotels` | ADMIN | Create hotel |
| PUT | `/api/hotels/{id}` | ADMIN | Update hotel |
| DELETE | `/api/hotels/{id}` | ADMIN | Delete hotel |

### Rooms
| Method | Endpoint | Access | Notes |
|--------|----------|--------|-------|
| GET | `/api/rooms/{id}` | Public | Room details |
| POST | `/api/hotels/{id}/rooms` | MANAGER/ADMIN | Create room |
| PUT | `/api/rooms/{id}` | MANAGER/ADMIN | Update room |
| DELETE | `/api/rooms/{id}` | MANAGER/ADMIN | Delete room |
| PATCH | `/api/rooms/{id}/status` | MANAGER/ADMIN | Toggle `isActive` |

### Bookings
| Method | Endpoint | Access | Body / Notes |
|--------|----------|--------|--------------|
| POST | `/api/bookings` | GUEST | `{ "roomId", "checkInDate", "checkOutDate" }` → creates booking |
| GET | `/api/bookings/mine` | GUEST | Current user's bookings |
| PUT | `/api/bookings/{id}/cancel` | GUEST | Cancel a booking (frees the room) |

### Manager
| Method | Endpoint | Access | Notes |
|--------|----------|--------|-------|
| GET | `/api/manager/hotels` | MANAGER/ADMIN | Hotels managed by the current user |
| GET | `/api/manager/hotels/{id}/rooms` | MANAGER/ADMIN | All rooms for a hotel (inventory view, incl. inactive) |
| GET | `/api/manager/bookings` | MANAGER/ADMIN | Upcoming bookings for the manager's hotel(s) |

> Availability rule: a room is available if it is **active** and has **no overlapping non-cancelled
> booking** (`existing.checkIn < requested.checkOut AND existing.checkOut > requested.checkIn`).
> Total price = `nights × pricePerNight`.

### Consistent error format
```json
{
  "timestamp": "2025-09-12T12:00:00Z",
  "path": "/api/...",
  "error": "VALIDATION_ERROR",
  "message": "Field is required"
}
```
Authorization failures (wrong/insufficient role) return **403 Forbidden** with `"error": "ACCESS_DENIED"`.

---

## cURL Commands (Quick Reference)

### 🔓 Auth APIs (Public - No Token Required)

#### Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Dinesh Sahu","email":"dinesh@test.com","password":"Password@123"}'
```
**PowerShell:**
```powershell
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"name\":\"Dinesh Sahu\",\"email\":\"dinesh@test.com\",\"password\":\"Password@123\"}"
```

#### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"dinesh@test.com","password":"Password@123"}'
```
**PowerShell:**
```powershell
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"dinesh@test.com\",\"password\":\"Password@123\"}"
```

#### Logout
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer <TOKEN>"
```
**PowerShell:**
```powershell
curl -X POST http://localhost:8080/api/auth/logout -H "Authorization: Bearer <TOKEN>"
```

---

### 🏨 Hotel APIs

#### Search Hotels by City (Public)
```bash
curl http://localhost:8080/api/hotels?city=Goa
```

#### Get All Hotels (Admin Only)
```bash
curl http://localhost:8080/api/hotels/all \
  -H "Authorization: ******"
```

#### Get Hotel by ID (Public)
```bash
curl http://localhost:8080/api/hotels/{hotelId}
```

#### Get Available Rooms for a Hotel (Public)
```bash
curl "http://localhost:8080/api/hotels/{hotelId}/rooms?checkIn=2026-09-01&checkOut=2026-09-05"
```

#### Create Hotel (Admin Only)
```bash
curl -X POST http://localhost:8080/api/hotels \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"name":"Ocean View Resort","city":"Goa","starRating":4,"description":"Beachfront luxury resort","coverImageUrl":"https://example.com/hotel.jpg","managerId":"<MANAGER_UUID>"}'
```

#### Update Hotel (Admin Only)
```bash
curl -X PUT http://localhost:8080/api/hotels/{hotelId} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"name":"Ocean View Resort Updated","city":"Goa","starRating":5,"description":"Updated description"}'
```

#### Delete Hotel (Admin Only)
```bash
curl -X DELETE http://localhost:8080/api/hotels/{hotelId} \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

---

### 🛏️ Room APIs (Manager Only)

#### Get Room by ID (Public)
```bash
curl http://localhost:8080/api/rooms/{roomId}
```

#### Create Room
```bash
curl -X POST http://localhost:8080/api/hotels/{hotelId}/rooms \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MANAGER_TOKEN>" \
  -d '{"roomNumber":"301","roomType":"DOUBLE","pricePerNight":3500.00,"maxOccupancy":2,"description":"Sea view room","isActive":true}'
```

#### Update Room
```bash
curl -X PUT http://localhost:8080/api/rooms/{roomId} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MANAGER_TOKEN>" \
  -d '{"roomNumber":"301","roomType":"SUITE","pricePerNight":7000.00,"maxOccupancy":3}'
```

#### Delete Room
```bash
curl -X DELETE http://localhost:8080/api/rooms/{roomId} \
  -H "Authorization: Bearer <MANAGER_TOKEN>"
```

#### Toggle Room Active Status
```bash
curl -X PATCH http://localhost:8080/api/rooms/{roomId}/status \
  -H "Authorization: Bearer <MANAGER_TOKEN>"
```

---

### 📅 Booking APIs (Guest)

#### Create Booking
```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <GUEST_TOKEN>" \
  -d '{"roomId":"<ROOM_UUID>","checkInDate":"2026-09-01","checkOutDate":"2026-09-05"}'
```

#### Get My Bookings
```bash
curl http://localhost:8080/api/bookings/mine \
  -H "Authorization: Bearer <GUEST_TOKEN>"
```

#### Cancel Booking
```bash
curl -X PUT http://localhost:8080/api/bookings/{bookingId}/cancel \
  -H "Authorization: Bearer <GUEST_TOKEN>"
```

---

### 📋 Manager - View Bookings & Hotels

#### Get My Managed Hotels
```bash
curl http://localhost:8080/api/manager/hotels \
  -H "Authorization: ******"
```

#### Get All Rooms for a Managed Hotel (incl. inactive)
```bash
curl http://localhost:8080/api/manager/hotels/{hotelId}/rooms \
  -H "Authorization: ******"
```

#### Get Upcoming Bookings for Manager's Hotel
```bash
curl http://localhost:8080/api/manager/bookings \
  -H "Authorization: Bearer <MANAGER_TOKEN>"
```

---

### 🔍 Utility / Health Check

#### Health Check
```bash
curl http://localhost:8080/actuator/health
```

#### OpenAPI JSON Spec
```bash
curl http://localhost:8080/v3/api-docs
```

#### Swagger UI (Open in Browser)
```
http://localhost:8080/swagger-ui/index.html
```

---

### 🔑 Seed Account Credentials (for testing)

All seed accounts use password: **`password`**

| Role | Email | Use |
|------|-------|-----|
| ADMIN | `admin@stayease.com` | Hotel CRUD |
| MANAGER | `mgr1@stayease.com` | Room CRUD + View Bookings |
| MANAGER | `mgr2@stayease.com` | Room CRUD + View Bookings |
| GUEST | `guest1@stayease.com` | Browse + Book + Cancel |
| GUEST | `guest2@stayease.com` | Browse + Book + Cancel |

---

## Configuration & Profiles

Each `application-<env>.yml` imports the shared `application-common.yml`; only the datasource and
`sql.init.mode` differ per environment. Default profile is `local`.

| Profile | Database | Seed data |
|---------|----------|-----------|
| `local` | H2 in-memory | Loaded on every startup |
| `dev` | PostgreSQL (`stayease_dev`) | Off |
| `prod` | PostgreSQL (env vars) | Off |

---

## Testing

```bash
mvn test
```
Unit tests use **JUnit5 + Mockito** (target ≥ 80% line coverage). Coverage is enforced by **JaCoCo**.

### Run coverage report
```bash
mvn clean test
# Open: target/site/jacoco/index.html
```

### Code formatting
```bash
mvn spotless:apply
```

API testing is done manually via **Postman / cURL**.

---

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Port 8080 was already in use` | A previous `java.exe` process (e.g. a stale `mvn spring-boot:run`) is still holding the port | Find and stop it: `Get-NetTCPConnection -LocalPort 8080` → note the PID → `Stop-Process -Id <PID> -Force` |
| `403 Forbidden` / `AuthorizationDeniedException` on a request | The logged-in user's role doesn't have the required authority for that endpoint (e.g. `POST /api/bookings` requires `GUEST`; a MANAGER/ADMIN token will be rejected) | Log in with an account of the correct role for that endpoint — see [Seed accounts](#seed-accounts) |
| Login/register response fields don't match the UI | The UI expects the flat `AuthResponse` shape: `{ token, type, tokenType, userId, email, name, role }` (not nested under a `user` object) | Already implemented — see `AuthResponse.java` |
| Spotless build failure (`mvn test` fails on formatting) | Long lines / formatting violations caught by `spotless-maven-plugin` | Run `mvn spotless:apply`, then re-run `mvn test` |

---

## Deployment

For free hosting instructions (Render, with a public hostname for your team),
see **[docs/Deployment.md](./docs/Deployment.md)**.

---

## Project Structure

```
src/main/java/com/apexon/stayease/
├── StayeaseBookingServiceApplication.java
├── config/                     # CorsConfig
├── controller/                 # REST Controllers (AuthController)
├── entity/                     # JPA Entities (User, Hotel, Room, Booking, HotelAmenity)
├── enums/                      # Enums (UserRole, RoomType, BookingStatus)
├── exception/                  # GlobalExceptionHandler, custom exceptions, ErrorResponse
├── model/                      # DTOs (RegisterRequest, LoginRequest, AuthResponse)
├── repository/                 # Spring Data JPA Repositories
├── security/                   # JWT provider, auth filter, CustomUserDetails, SecurityConfig
└── service/                    # Service interfaces
    └── impl/                   # Service implementations (AuthServiceImpl)

src/test/java/com/apexon/stayease/
├── StayeaseBookingServiceApplicationTests.java
├── controller/                 # AuthControllerTest
├── exception/                  # GlobalExceptionHandlerTest
├── security/                   # JwtTokenProviderTest, JwtAuthenticationFilterTest, CustomUserDetailsServiceTest
└── service/                    # AuthServiceTest

src/main/resources/
├── application.yml             # Sets default profile = local
├── application-common.yml      # Shared JPA/logging/server config
├── application-{local,dev,prod}.yml
└── db/migration/               # Flyway SQL migrations (V1, V2, V3)
```

---

## License

Capstone project for certification — internal use.
