# StayEase - High Level Design (HLD)

> Foundation capstone: a hotel room booking web app. Guests browse hotels, check date-based room
> availability, and confirm bookings; Managers manage rooms; Admins manage the hotel directory.

**📖 Documentation:**  
- [Backend_Components.md](./Backend_Components.md) - Backend blueprint & component breakdown  
- [API_Guide.md](./API_Guide.md) - Complete API documentation  
- [Architecture.md](./Architecture.md) - Visual architecture diagrams  
- [Frontend_Components.md](./Frontend_Components.md) - Frontend UI design

---

## 1. Architecture Overview

A **single-page frontend** calling a **single Spring Boot backend service** that owns the database.
No service-to-service calls; no message brokers; no payment gateway.

```
┌──────────────────────────┐        HTTPS / REST (JSON)        ┌───────────────────────────────┐
│   Frontend (SPA)         │  ───────────────────────────────▶ │   StayEase Backend Service     │
│   React + Vite + TS      │      Authorization: Bearer <JWT>  │   Spring Boot 4.1.1            │
│   Axios · Context · Router│ ◀─────────────────────────────── │   (Spring MVC · Tomcat)        │
└──────────────────────────┘        JSON responses / errors    │  Controller → Service → Repo   │
                                                                │  Spring Data JPA / Hibernate   │
                                                                └───────────────┬───────────────┘
                                                                                │ JDBC
                                                                ┌───────────────▼───────────────┐
                                                                │  PostgreSQL Database           │
                                                                │  Flyway-managed migrations     │
                                                                └────────────────────────────────┘
```

- **Style:** modular monolith, layered (n-tier). Blocking I/O (Tomcat + JDBC).
- **Rationale:** UI → service → DB only, Foundation-scale load (1–10 concurrent). Blocking MVC + JPA
  matches the spec, keeps entity relationships, and simple tests.

---

## 2. Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React + Vite + TypeScript, React Router, Axios, Context API |
| Backend | Java 21, Spring Boot 4.1.1, Spring MVC, Spring Data JPA (Hibernate 7), Bean Validation, Spring Security |
| Auth | Stateless **JWT** (JJWT 0.12.6) + BCrypt password hashing |
| Database | PostgreSQL 18 (all environments), Flyway migrations |
| Docs/Test | Springdoc OpenAPI 2.8.6 (Swagger UI), JUnit5, Mockito, JaCoCo (80% min coverage) |
| Build/Tools | Maven, Git, Lombok, Docker, Spotless (code formatting) |

---

## 3. Package Structure

```
com.apexon.stayease
├── config/                # CorsConfig, OpenApiConfig
├── controller/            # AuthController, HotelController, RoomController,
│                          # BookingController, ManagerController
├── entity/                # User, Hotel, Room, Booking, HotelAmenity
├── enums/                 # UserRole, RoomType, BookingStatus
├── exception/             # GlobalExceptionHandler, ResourceNotFoundException,
│                          # EmailAlreadyExistsException, InvalidCredentialsException, ErrorResponse
├── model/                 # DTOs: RegisterRequest, LoginRequest, AuthResponse,
│                          # HotelRequest/Response, RoomRequest/Response,
│                          # BookingRequest/Response
├── repository/            # UserRepository, HotelRepository, RoomRepository,
│                          # BookingRepository, HotelAmenityRepository
├── security/              # SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter,
│                          # CustomUserDetails, CustomUserDetailsService
├── service/               # AuthService, HotelService, RoomService, BookingService
│   └── impl/              # AuthServiceImpl, HotelServiceImpl, RoomServiceImpl, BookingServiceImpl
├── util/                  # BookingReferenceGenerator
└── StayeaseBookingServiceApplication.java
```

**Layering rule:** `Controller → Service → Repository → DB`. Controllers stay thin (HTTP + validation);
business logic lives in services; repositories handle persistence. **Entities never cross the API boundary** —
always mapped to DTOs.

---

## 4. Component / Layer View

| Layer | Responsibility |
|-------|----------------|
| **Controller** | REST endpoints, request validation, OpenAPI annotations; thin |
| **Service** (Interface + Impl) | Business rules: availability, pricing, booking, role checks; `@Transactional` |
| **Repository** | Spring Data JPA persistence + custom JPQL queries |
| **Entity** | JPA-mapped domain model (User, Hotel, Room, Booking, HotelAmenity) |
| **Model (DTO)** | Request/Response objects; entities never cross the API boundary |
| **Security** | JWT filter, token provider, CustomUserDetails, method-level `@PreAuthorize` |
| **Exception** | `@RestControllerAdvice` → consistent error JSON |
| **Util** | BookingReferenceGenerator (Factory pattern) |

---

## 5. Design Patterns

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| **Layered Architecture** | Controller → Service → Repository → Entity | Separation of concerns |
| **Repository Pattern** | `JpaRepository` interfaces with custom `@Query` methods | Abstract data access |
| **DTO Pattern** | Separate Request/Response classes in `model/` package | API contract isolation |
| **Service Layer Pattern** | Interface + `Impl` separation (`AuthService` / `AuthServiceImpl`) | Abstraction, testability |
| **Builder Pattern** | Lombok `@Builder` on all entities and DTOs | Fluent, readable object creation |
| **Factory Pattern** | `BookingReferenceGenerator.generate()` → `BK-YYYYMMDD-XXXXXXXX` | Centralized reference creation |
| **Strategy Pattern** | Pricing: `nights × pricePerNight`; extensible for future seasonal pricing | Extensible business rules |
| **Filter / Chain of Responsibility** | `JwtAuthenticationFilter` in the Spring Security filter chain | Pluggable authentication |
| **Singleton** | Spring-managed beans (`@Service`, `@Repository`, `@Component`) | Resource management |
| **Global Handler (Advice)** | `@RestControllerAdvice` in `GlobalExceptionHandler` | Consistent error responses |
| **Dependency Injection** | Constructor injection via Lombok `@RequiredArgsConstructor` | Loose coupling, testability |

---

## 6. Data Model (ER)

```
User (MANAGER) ──────< manages >──── Hotel ──────< has >──── Room
User (GUEST)   ──────< books  >──── Booking ────< for >──── Room
                                     Hotel  ──────< has >──── HotelAmenity
```

### Entities

| Entity | Key Fields | Table |
|--------|-----------|-------|
| **User** | userId (UUID PK), fullName, emailAddress (unique), passwordHash, userRole (enum), isActive, createdAt, updatedAt | `users` |
| **Hotel** | hotelId (UUID PK), hotelName, description, streetAddress, cityName, stateName, countryName, postalCode, contactPhone, contactEmail, starRating (1–5), coverImageUrl, isActive, managerId (FK→User) | `hotels` |
| **Room** | roomId (UUID PK), roomNumber (unique per hotel), roomType (enum), roomDescription, pricePerNight (BigDecimal), maxOccupancy, roomImageUrl, isAvailable, hotelId (FK→Hotel) | `rooms` |
| **Booking** | bookingId (UUID PK), bookingReference (unique, `BK-YYYYMMDD-XXXXXXXX`), checkInDate, checkOutDate, numberOfGuests, totalAmount (BigDecimal), bookingStatus (enum), specialRequests, customerId (FK→User), roomId (FK→Room) | `bookings` |
| **HotelAmenity** | hotelId + amenityName (composite PK), amenityDescription, hotelId (FK→Hotel) | `hotel_amenities` |

### Relationships
- `Hotel M—1 User` (manager)
- `Hotel 1—M Room` (cascade all, orphan removal)
- `Hotel 1—M HotelAmenity` (cascade all, orphan removal)
- `Room 1—M Booking`
- `User 1—M Booking` (as customer)

### Enums (stored as String)
- **UserRole**: `GUEST` | `MANAGER` | `ADMIN`
- **RoomType**: `SINGLE` | `DOUBLE` | `SUITE` | `DELUXE` | `FAMILY`
- **BookingStatus**: `PENDING` | `CONFIRMED` | `CANCELLED` | `COMPLETED`

### Database Migrations
Schema managed by **Flyway** (`src/main/resources/db/migration/`):
- `V1__create_stayease_base_schema.sql` — users, hotels, rooms, bookings tables + indexes
- `V2__create_hotel_amenities_table.sql` — hotel_amenities table with composite PK

---

## 7. API Outline (✅ All Implemented)

### Auth (Public)
| Method | Endpoint | Status | Description |
|--------|----------|--------|-------------|
| POST | `/api/auth/register` | 201 | Register new user (GUEST role) |
| POST | `/api/auth/login` | 200 | Login, returns JWT token |
| POST | `/api/auth/logout` | 204 | Logout (clears security context) |

### Hotels
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/hotels?city=` | Public | List active hotels in a city |
| GET | `/api/hotels/all` | ADMIN | List all hotels, regardless of city |
| GET | `/api/hotels/{id}` | Public | Get hotel details |
| POST | `/api/hotels` | ADMIN | Create hotel |
| PUT | `/api/hotels/{id}` | ADMIN | Update hotel |
| DELETE | `/api/hotels/{id}` | ADMIN | Delete hotel |

### Rooms
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/hotels/{id}/rooms?checkIn=&checkOut=` | Public | Available rooms for dates |
| GET | `/api/rooms/{id}` | Public | Get room details |
| POST | `/api/hotels/{id}/rooms` | MANAGER/ADMIN | Create room |
| PUT | `/api/rooms/{id}` | MANAGER/ADMIN | Update room |
| DELETE | `/api/rooms/{id}` | MANAGER/ADMIN | Delete room |
| PATCH | `/api/rooms/{id}/status?isAvailable=` | MANAGER/ADMIN | Toggle availability |

### Bookings
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/bookings` | GUEST | Create booking |
| GET | `/api/bookings/mine` | GUEST | List my bookings |
| PUT | `/api/bookings/{id}/cancel` | GUEST | Cancel booking |

### Manager
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/manager/hotels` | MANAGER/ADMIN | Hotels managed by current user |
| GET | `/api/manager/hotels/{id}/rooms` | MANAGER/ADMIN | All rooms for a hotel (inventory view) |
| GET | `/api/manager/bookings` | MANAGER/ADMIN | Upcoming bookings for manager's hotel |

**Total: 21 endpoints** (6 public, 15 authenticated)

---

## 8. Authentication & Authorization (Stateless JWT)

**Chosen: stateless JWT** (spec: "choose one; document trade-offs").

### Flow
1. Register → password hashed with **BCrypt**, stored; role defaults to `GUEST`.
2. Login → validate credentials → issue a signed **JWT** (HS256, subject = userId, expiry = 24h).
3. Client stores the token and sends `Authorization: Bearer <token>` on each request.
4. `JwtAuthenticationFilter` validates the token and populates the `SecurityContext`.
5. Authorization via method security: `@PreAuthorize("hasAuthority('ADMIN')")`, `hasAnyAuthority('MANAGER', 'ADMIN')`, etc.
6. Logout clears server security context; client discards the token.

### Security Rules (SecurityConfig)
```
Public:        /api/auth/**, GET /api/hotels/**, /swagger-ui/**, /v3/api-docs/**, /actuator/**
Authenticated: All other requests
```

### Trade-offs

| | JWT (chosen) | Session cookie |
|--|--------------|----------------|
| State | Stateless — no server session store; scales trivially | Server holds session state |
| Frontend | Simple for SPA + mobile; token in header | CSRF handling needed |
| Revocation | Harder (mitigate with short expiry) | Easy (invalidate session) |
| Fit | Matches SPA + REST + localhost Foundation scope | Heavier for a stateless REST API |

---

## 9. Key Request Flows

### 9.1 Search → results
```
UI SearchPanel → GET /api/hotels?city=New York
  HotelController.getHotelsByCity(city)
    → HotelService.getHotelsByCity(city)
      → HotelRepository.findByCityNameAndIsActive(city, true)
        → List<HotelResponse>
```

### 9.2 Availability
```
UI Hotel Detail → GET /api/hotels/{id}/rooms?checkIn=2026-09-01&checkOut=2026-09-05
  RoomController.getAvailableRooms(hotelId, checkIn, checkOut)
    → RoomService.getAvailableRooms(hotelId, checkIn, checkOut)
      → RoomRepository.findByHotelIdAndIsAvailable(hotelId, true)
      → BookingRepository.findConflictingBookings(roomId, checkIn, checkOut, excludeStatuses)
        → filter rooms with no conflicts → List<RoomResponse>
```
Overlap rule: `existing.checkIn <= requested.checkOut AND existing.checkOut >= requested.checkIn`.

### 9.3 Booking (transactional)
```
UI [Book] (auth GUEST) → POST /api/bookings {roomId, checkInDate, checkOutDate, numberOfGuests}
  BookingController.createBooking(request, userDetails)
    → BookingService.createBooking(request, customerId) @Transactional
      1. validate customer & room exist
      2. validate room.isAvailable == true
      3. validate dates (future, checkOut > checkIn, max 30 nights)
      4. validate numberOfGuests <= room.maxOccupancy
      5. check no conflicting bookings (exclude CANCELLED)
      6. total = ChronoUnit.DAYS.between(checkIn, checkOut) × pricePerNight
      7. bookingRef = BookingReferenceGenerator.generate()  → "BK-20260825-A3F5K7M2"
      8. save(status = CONFIRMED) → BookingResponse
```

### 9.4 Cancel
```
PUT /api/bookings/{id}/cancel (auth GUEST, must own the booking)
  BookingService.cancelBooking(bookingId, customerId) @Transactional
    → validate ownership, not already CANCELLED/COMPLETED, checkIn not in past
    → status = CANCELLED → room reappears as available (availability query ignores CANCELLED)
```

---

## 10. Consistent Error Format
```json
{
  "timestamp": "2026-08-25T12:30:00",
  "path": "/api/bookings",
  "error": "INVALID_REQUEST",
  "message": "Check-out date must be after check-in date",
  "details": { "field": "checkOutDate" }
}
```

Exception types handled by `GlobalExceptionHandler` (`@RestControllerAdvice`):

| Exception | HTTP Status | Error Code |
|-----------|-------------|------------|
| `ResourceNotFoundException` | 404 | RESOURCE_NOT_FOUND |
| `EmailAlreadyExistsException` | 409 | EMAIL_ALREADY_EXISTS |
| `InvalidCredentialsException` / `BadCredentialsException` | 401 | AUTHENTICATION_FAILED |
| `MethodArgumentNotValidException` | 400 | VALIDATION_ERROR |
| `IllegalArgumentException` | 400 | INVALID_REQUEST |
| `IllegalStateException` | 400 | INVALID_STATE |
| Other `Exception` | 500 | INTERNAL_SERVER_ERROR |

---

## 11. NFRs & SLAs

- **Performance:** API ≤ 2 s under light load (1–5 concurrent); UI initial load ≤ 3 s on localhost.
- **Security:** BCrypt hashing; JWT HS256 signed + 24h expiry; server-side validation on all writes.
- **Code Quality:** JaCoCo minimum 80% coverage; Spotless code formatting; SonarQube integration.
- **Concurrency:** verified with 5–10 concurrent requests (Postman Runner). No load tools.
- **Error handling:** friendly UI toasts from structured API error JSON.
- **Logging:** SLF4J (Logback) console logs; `@Slf4j` on all services & controllers.
- **Documentation:** Swagger/OpenAPI at `/swagger-ui.html`, comprehensive docs in `docs/`.

---

## 12. Environments, Config & Deployment

- **Profiles:** `local`, `dev`, `prod` — each imports `application-common.yml`.
- **Database:** PostgreSQL for all environments. Flyway manages schema migrations.
- **Config pattern:** each `application-<env>.yml` overrides datasource settings.
- **Default profile:** `local`.
- **Run order:** Start PostgreSQL → run Spring Boot (Flyway auto-migrates) → run UI.
- **Deployment:** Docker + docker-compose for local; Railway for cloud deployment.
- **CORS:** configured for frontend origins (`localhost:3000`, `localhost:5173`).

---

## 13. Future Evolution (out of scope)

If scale/team boundaries later demand it, the modular monolith can split along its feature packages
(auth / hotel / room / booking) into independent services behind an API gateway, with a service registry
and per-service databases, and reactive I/O where a service fans out to others. **Not required for
Foundation** — the current single blocking service is the right fit for the stated scope.

Potential enhancements:
- Payment integration
- Email notifications for booking confirmation
- Redis caching for hotel search
- Pagination for list endpoints
- Review & rating system

---

## 14. Testing Strategy

- **Unit:** JUnit5 + Mockito for services; target ≥ 80% method coverage (JaCoCo enforced).
- **Controller tests:** `@WebMvcTest` style with mocked services.
- **Security tests:** `CustomUserDetailsServiceTest`, `JwtTokenProviderTest`, `JwtAuthenticationFilterTest`.
- **Exception tests:** `GlobalExceptionHandlerTest` for all error scenarios.
- **Manual/API:** Swagger UI at `/swagger-ui.html`, Postman collection.
- **Code quality:** Spotless formatting, JaCoCo coverage reports at `target/site/jacoco/`.

---

## Documentation Reference

📖 **[API_Guide.md](./API_Guide.md)** — Complete API documentation, security, design patterns, all endpoint details

🏗️ **[Architecture.md](./Architecture.md)** — Visual architecture diagrams, component relationships, data flow

🔧 **[Backend_Components.md](./Backend_Components.md)** — Backend blueprint, package structure, entities, booking flow, security config

📋 **[Frontend_Components.md](./Frontend_Components.md)** — Frontend UI component design & routing

---

**Author**: Dinesh Sahu · **Email**: dinesh.sahu@apexon.com · **ID**: 206755  
**Team**: Team 45 · **Date**: August 25, 2026
