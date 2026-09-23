# StayEase - Backend Components

> **Architecture:** a single Spring Boot service (modular monolith). The UI calls this service over
> REST, and this service talks directly to the database. There are **no downstream service calls**.
> This document is the backend blueprint the team develops against.
>
> **Companion docs:** [Frontend_Components.md](./Frontend_Components.md) · [High_Level_Design.md](./High_Level_Design.md)

---

## Stack

- **GroupId:** `com.apexon.stayease` · **Base package:** `com.apexon.stayease`
- **Java:** 21 · **Spring Boot 4.1.1** with **Spring MVC** (blocking, Tomcat)
- **Persistence:** **Spring Data JPA / Hibernate 7**
- **Database:** **PostgreSQL** (all environments), **Flyway** migrations
- **Config:** profile-per-env YAML (`local` / `dev` / `prod`), each importing `application-common.yml`
- **Security:** Spring Security + BCrypt password hashing + **JWT** (JJWT 0.12.6, stateless)
- **Validation:** Bean Validation (`jakarta.validation`)
- **Docs:** Springdoc OpenAPI 2.8.6 (Swagger UI)
- **Testing:** JUnit5 + Mockito, JaCoCo (80% min coverage)
- **Code Quality:** Spotless formatting, Lombok

> **Why a single blocking service (not reactive microservices)?** The flow is UI → service → DB with no
> service-to-service calls, and Foundation-scale load (1–10 concurrent local users). Blocking Spring MVC +
> JPA matches the spec, gives entity relationships and simpler tests. See
> [High_Level_Design.md](./High_Level_Design.md) §13 for the future microservices evolution path.

---

## 1. Package / Layer Structure

A **layered (n-tier) architecture** under the base package:

```
com.apexon.stayease
├── config/                # CorsConfig, OpenApiConfig
├── security/              # SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter,
│                          # CustomUserDetails, CustomUserDetailsService
├── controller/            # AuthController, HotelController, RoomController,
│                          # BookingController, ManagerController
├── service/               # AuthService, HotelService, RoomService, BookingService (interfaces)
│   └── impl/              # AuthServiceImpl, HotelServiceImpl, RoomServiceImpl, BookingServiceImpl
├── repository/            # UserRepository, HotelRepository, RoomRepository,
│                          # BookingRepository, HotelAmenityRepository
├── entity/                # User, Hotel, Room, Booking, HotelAmenity
├── enums/                 # UserRole, RoomType, BookingStatus
├── model/                 # DTOs: RegisterRequest, LoginRequest, AuthResponse,
│                          # HotelRequest/Response, RoomRequest/Response,
│                          # BookingRequest/Response
├── exception/             # GlobalExceptionHandler, ResourceNotFoundException,
│                          # EmailAlreadyExistsException, InvalidCredentialsException, ErrorResponse
├── util/                  # BookingReferenceGenerator
└── StayeaseBookingServiceApplication.java
```

**Layering rule:** `Controller → Service → Repository → DB`. Controllers stay thin (HTTP + validation);
business logic lives in services; repositories handle persistence. **Never expose JPA entities directly** —
always map to DTOs.

---

## 2. Design Patterns

| Pattern | Where / Why |
|---------|-------------|
| **Layered Architecture** | Controller → Service → Repository separation |
| **Repository** | Spring Data JPA `@Query` repositories abstract data access |
| **Service Layer** | Interface + Impl separation (`HotelService` / `HotelServiceImpl`) for abstraction & testability |
| **DTO** | Request/Response objects in `model/`; entities never cross the API boundary |
| **Builder** | Lombok `@Builder` on all entities and DTOs for fluent object creation |
| **Factory** | `BookingReferenceGenerator.generate()` → `BK-YYYYMMDD-XXXXXXXX` for booking creation |
| **Strategy** | Pricing: `nights × pricePerNight`, extensible for seasonal pricing |
| **Filter (Chain of Responsibility)** | `JwtAuthenticationFilter` in the Spring Security filter chain |
| **Singleton** | Spring-managed beans (`@Service`, `@Repository`, `@Component` — default scope) |
| **Global Handler (Advice)** | `@RestControllerAdvice` in `GlobalExceptionHandler` for consistent error JSON |
| **Dependency Injection** | Constructor injection via Lombok `@RequiredArgsConstructor` |

---

## 3. Domain Entities (JPA)

| Entity | Table | Key Fields | Relationships |
|--------|-------|-----------|---------------|
| **User** | `users` | userId (UUID PK), fullName, emailAddress (unique), passwordHash, userRole (enum), isActive, createdAt, updatedAt | 1 User → M Booking (as customer); 1 User → M Hotel (as manager) |
| **Hotel** | `hotels` | hotelId (UUID PK), hotelName, description, streetAddress, cityName, stateName, countryName, postalCode, contactPhone, contactEmail, starRating (1–5), coverImageUrl, isActive | Hotel M→1 User (manager); 1 Hotel → M Room; 1 Hotel → M HotelAmenity |
| **Room** | `rooms` | roomId (UUID PK), roomNumber (unique per hotel), roomType (enum), roomDescription, pricePerNight (BigDecimal), maxOccupancy, roomImageUrl, isAvailable | Room M→1 Hotel; 1 Room → M Booking |
| **Booking** | `bookings` | bookingId (UUID PK), bookingReference (unique), checkInDate, checkOutDate, numberOfGuests, totalAmount (BigDecimal), bookingStatus (enum), specialRequests | Booking M→1 User (customer); Booking M→1 Room |
| **HotelAmenity** | `hotel_amenities` | hotelId + amenityName (composite PK), amenityDescription | HotelAmenity M→1 Hotel |

### Enums (stored as String)
- **UserRole**: `GUEST` | `MANAGER` | `ADMIN` (defaults to `GUEST` on registration)
- **RoomType**: `SINGLE` | `DOUBLE` | `SUITE` | `DELUXE` | `FAMILY`
- **BookingStatus**: `PENDING` | `CONFIRMED` | `CANCELLED` | `COMPLETED`

### Database Migrations (Flyway)
Schema managed by Flyway in `src/main/resources/db/migration/`:
- `V1__create_stayease_base_schema.sql` — users, hotels, rooms, bookings tables + indexes
- `V2__create_hotel_amenities_table.sql` — hotel_amenities table with composite PK

---

## 4. Component Breakdown (✅ = implemented)

### 4.1 Auth & Security
- ✅ `User` entity + `UserRole` enum · `UserRepository` (`findByEmailAddress`, `existsByEmailAddress`)
- ✅ `AuthService` / `AuthServiceImpl` — register (BCrypt hash), login (validate → issue JWT), logout
- ✅ `AuthController` — `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout`
- ✅ `SecurityConfig` — `PasswordEncoder` (BCrypt), `SecurityFilterChain`, public vs protected routes
- ✅ JWT: `JwtTokenProvider`, `JwtAuthenticationFilter`, `CustomUserDetails`, `CustomUserDetailsService`
- ✅ Role checks via `@PreAuthorize("hasAuthority('ADMIN')")`, `hasAnyAuthority('MANAGER', 'ADMIN')` etc.

### 4.2 Hotel Management (Admin)
- ✅ `Hotel` entity · `HotelRepository` (`findByCityNameAndIsActive`, `findByManagerId`, `searchByHotelName`)
- ✅ `HotelService` / `HotelServiceImpl` — search by city, get by ID, CRUD (**ADMIN** only)
- ✅ `HotelController`:
  - `GET /api/hotels?city=` — list active hotels (public)
  - `GET /api/hotels/all` — list all hotels (**ADMIN**)
  - `GET /api/hotels/{id}` — hotel details (public)
  - `POST /api/hotels` · `PUT /api/hotels/{id}` · `DELETE /api/hotels/{id}` — **ADMIN**

### 4.3 Room & Availability (Manager)
- ✅ `Room` entity + `RoomType` enum · `RoomRepository` (`findByHotelIdAndIsAvailable`, `findByHotelIdAndRoomNumber`)
- ✅ `RoomService` / `RoomServiceImpl` — list available rooms for dates, CRUD (**MANAGER/ADMIN**), toggle isAvailable
- ✅ Pricing: `total = ChronoUnit.DAYS.between(checkIn, checkOut) × pricePerNight`
- ✅ `RoomController`:
  - `GET /api/hotels/{id}/rooms?checkIn=&checkOut=` — available rooms (public)
  - `GET /api/rooms/{id}` — room details (public)
  - `POST /api/hotels/{id}/rooms` · `PUT /api/rooms/{id}` · `DELETE /api/rooms/{id}` — **MANAGER/ADMIN**
  - `PATCH /api/rooms/{id}/status?isAvailable=` — toggle availability — **MANAGER/ADMIN**

### 4.4 Booking
- ✅ `Booking` entity + `BookingStatus` enum · `BookingRepository` (`findByCustomerId`, `findConflictingBookings`, `findByHotelId`)
- ✅ `BookingReferenceGenerator` — generates `BK-YYYYMMDD-XXXXXXXX` (Factory pattern)
- ✅ `BookingService` / `BookingServiceImpl` — create (validate + compute total, `@Transactional`), list mine, cancel
- ✅ `BookingController`:
  - `POST /api/bookings` `{roomId, checkInDate, checkOutDate, numberOfGuests}` — **GUEST**
  - `GET /api/bookings/mine` — **GUEST**
  - `PUT /api/bookings/{id}/cancel` — **GUEST** (must own booking)
- ✅ `ManagerController`:
  - `GET /api/manager/hotels` — hotels managed by current user — **MANAGER/ADMIN**
  - `GET /api/manager/hotels/{id}/rooms` — all rooms for a hotel, inventory view (incl. inactive) — **MANAGER/ADMIN**
  - `GET /api/manager/bookings` — upcoming bookings for manager's hotels — **MANAGER/ADMIN**

### 4.5 Cross-cutting
- ✅ `GlobalExceptionHandler` (`@RestControllerAdvice`) → consistent error JSON (§5)
- ✅ Exceptions: `ResourceNotFoundException`, `EmailAlreadyExistsException`, `InvalidCredentialsException`
- ✅ Business validation: `IllegalArgumentException`, `IllegalStateException` for booking rules
- ✅ `OpenApiConfig` (Springdoc) + Swagger UI at `/swagger-ui.html`
- ✅ `CorsConfig` for frontend origins (`localhost:3000`, `localhost:5173`)
- ✅ Profiles: `local` / `dev` / `prod` + `application-common.yml`
- ✅ Flyway migrations for schema management

---

## 5. Consistent Error Format

```json
{
  "timestamp": "2026-08-25T12:30:00",
  "path": "/api/bookings",
  "error": "INVALID_REQUEST",
  "message": "Check-out date must be after check-in date",
  "details": { "field": "checkOutDate" }
}
```

Produced centrally by `GlobalExceptionHandler`; each exception maps to an HTTP status + `error` code:

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

## 6. Availability Logic (core)

A room is available if it is **active** (`isAvailable = true`) and has **no overlapping non-cancelled booking**:

```java
// RoomServiceImpl — availability check flow:
1. Get all rooms: roomRepository.findByHotelIdAndIsAvailable(hotelId, true)
2. For each room, check conflicts:
   bookingRepository.findConflictingBookings(roomId, checkIn, checkOut, List.of(CANCELLED))
3. Return rooms with ZERO conflicts
```

**Conflict query** (BookingRepository):
```java
@Query("SELECT b FROM Booking b WHERE b.room.roomId = :roomId "
     + "AND b.bookingStatus NOT IN :excludeStatuses "
     + "AND ((b.checkInDate <= :checkOutDate AND b.checkOutDate >= :checkInDate))")
List<Booking> findConflictingBookings(UUID roomId, LocalDate checkInDate,
                                      LocalDate checkOutDate, List<BookingStatus> excludeStatuses);
```

Overlap rule: `existing.checkIn <= requested.checkOut AND existing.checkOut >= requested.checkIn`.

**Total price:** `nights = ChronoUnit.DAYS.between(checkIn, checkOut)` → `total = nights × pricePerNight`

---

## 7. Booking Flow (transactional)

```
POST /api/bookings  (authenticated GUEST, extracted via @AuthenticationPrincipal)
  BookingService.createBooking(request, customerId)  @Transactional
    1. validate customer exists (UserRepository)
    2. load Room (404 if missing)
    3. validate room.isAvailable == true
    4. validate dates: future, checkOut > checkIn, max 30 nights
    5. validate numberOfGuests <= room.maxOccupancy
    6. re-check availability for the date window (findConflictingBookings)
         └─ if overlap exists → IllegalStateException ("Room is not available for the selected dates")
    7. total = ChronoUnit.DAYS.between(checkIn, checkOut) × pricePerNight
    8. bookingRef = BookingReferenceGenerator.generate()  → "BK-20260825-A3F5K7M2"
    9. bookingRepository.save(status = CONFIRMED)
   10. return BookingResponse (bookingRef, total, hotelName, roomNumber, ...)
```

**Cancel:**
```
PUT /api/bookings/{id}/cancel  (authenticated GUEST, must own the booking)
  BookingService.cancelBooking(bookingId, customerId)  @Transactional
    → validate ownership (customer matches)
    → validate not already CANCELLED or COMPLETED
    → validate checkInDate not in past
    → status = CANCELLED
    → room reappears as available (availability query ignores CANCELLED)
```

---

## 8. Security Configuration

```java
// SecurityConfig — URL-based rules:
Public:        /api/auth/**
               GET /api/hotels/**    (search + rooms)
               /swagger-ui/**, /v3/api-docs/**
               /actuator/**
Authenticated: Everything else

// Method-level authorization:
@PreAuthorize("hasAuthority('ADMIN')")           → Hotel CRUD
@PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')") → Room CRUD, Manager bookings
@PreAuthorize("hasAuthority('GUEST')")           → Booking create/view/cancel
```

- **JWT Filter:** `JwtAuthenticationFilter` extracts token → validates → loads `CustomUserDetails` → sets `SecurityContext`
- **Password:** BCrypt encoder (default 10 rounds)
- **Session:** Stateless (`SessionCreationPolicy.STATELESS`)
- **CORS:** Configured for frontend origins

---

## 9. Testing

- **Unit:** JUnit5 + Mockito for services (target ≥ 80% coverage via JaCoCo)
- **Existing tests:**
  - `AuthControllerTest` — register, login, logout
  - `AuthServiceTest` — registration, authentication
  - `CustomUserDetailsServiceTest` — user loading, authorities
  - `JwtTokenProviderTest` — token generation, validation
  - `JwtAuthenticationFilterTest` — filter chain
  - `GlobalExceptionHandlerTest` — all error scenarios
  - `StayeaseBookingServiceApplicationTests` — context loads
- **Manual/API:** Swagger UI at `/swagger-ui.html`
- **Code quality:** Spotless formatting, JaCoCo coverage reports at `target/site/jacoco/`

---

## 10. Dependencies (pom.xml)

**Already configured:**
- `spring-boot-starter-web` — Spring MVC + Tomcat
- `spring-boot-starter-data-jpa` — JPA + Hibernate
- `spring-boot-starter-validation` — Jakarta Bean Validation
- `spring-boot-starter-security` — Spring Security
- `org.postgresql:postgresql` — PostgreSQL driver
- `org.flywaydb:flyway-core` + `flyway-database-postgresql` — Flyway migrations
- `io.jsonwebtoken:jjwt-api/impl/jackson` (0.12.6) — JWT
- `org.springdoc:springdoc-openapi-starter-webmvc-ui` (2.8.6) — Swagger
- `org.projectlombok:lombok` — Boilerplate reduction
- `spring-boot-starter-test` — JUnit5 + Mockito
- `org.jacoco:jacoco-maven-plugin` (0.8.12) — Code coverage
- Spotless — Code formatting

---

## 11. Build & Run

```bash
# Local (PostgreSQL, default profile)
mvn spring-boot:run

# With specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Tests
mvn test

# Build JAR
mvn clean package -DskipTests

# Run JAR
java -jar target/stayease-booking-service-0.0.1-SNAPSHOT.jar
```

**Run order:** Start PostgreSQL → Run Spring Boot (Flyway auto-migrates) → Run frontend UI.

**Key URLs:**
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

---

See [High_Level_Design.md](./High_Level_Design.md) for the end-to-end architecture, sequence diagrams,
NFRs, and the microservices evolution path.

---

**Author**: Dinesh Sahu · **Email**: dinesh.sahu@apexon.com · **ID**: 206755  
**Team**: Team 45 · **Date**: August 25, 2026

