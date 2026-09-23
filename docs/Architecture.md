# Architecture Overview - StayEase Hotel Booking Service

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │  Guest   │  │ Manager  │  │  Admin   │  │ Swagger  │       │
│  │  App     │  │   App    │  │  Portal  │  │    UI    │       │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘       │
└───────┼─────────────┼─────────────┼─────────────┼──────────────┘
        │             │             │             │
        └─────────────┴───────────
        ──┴─────────────┘
                      │
                HTTP/REST (JSON)
                      │
┌─────────────────────▼─────────────────────────────────────────────┐
│                     Security Layer                                 │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │  JWT Authentication Filter                                │    │
│  │  - Token validation                                       │    │
│  │  - User context extraction                                │    │
│  └──────────────────────────────────────────────────────────┘    │
└────────────────────────┬──────────────────────────────────────────┘
                         │
┌────────────────────────▼──────────────────────────────────────────┐
│                     Controller Layer                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐         │
│  │  Auth    │  │  Hotel   │  │   Room   │  │ Booking  │         │
│  │Controller│  │Controller│  │Controller│  │Controller│         │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘         │
│       │             │              │             │                │
│  @RestController   @RestController                                │
│  @PreAuthorize     Role-based access control                      │
└───────┼─────────────┼──────────────┼─────────────┼────────────────┘
        │             │              │             │
┌───────▼─────────────▼──────────────▼─────────────▼────────────────┐
│                     Service Layer                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐         │
│  │  Auth    │  │  Hotel   │  │   Room   │  │ Booking  │         │
│  │ Service  │  │ Service  │  │ Service  │  │ Service  │         │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘         │
│       │             │              │             │                │
│  Business Logic, Validation, Transaction Management               │
└───────┼─────────────┼──────────────┼─────────────┼────────────────┘
        │             │              │             │
┌───────▼─────────────▼──────────────▼─────────────▼────────────────┐
│                   Repository Layer                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐         │
│  │   User   │  │  Hotel   │  │   Room   │  │ Booking  │         │
│  │   Repo   │  │   Repo   │  │   Repo   │  │   Repo   │         │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘         │
│       │             │              │             │                │
│  Spring Data JPA - Custom Queries                                 │
└───────┼─────────────┼──────────────┼─────────────┼────────────────┘
        │             │              │             │
┌───────▼─────────────▼──────────────▼─────────────▼────────────────┐
│                     Entity Layer                                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐         │
│  │   User   │  │  Hotel   │  │   Room   │  │ Booking  │         │
│  │  Entity  │  │  Entity  │  │  Entity  │  │  Entity  │         │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘         │
│       │             │              │             │                │
│  JPA Entities with relationships                                  │
└───────┼─────────────┼──────────────┼─────────────┼────────────────┘
        │             │              │             │
┌───────▼─────────────▼──────────────▼─────────────▼────────────────┐
│                   Database Layer                                   │
│                  PostgreSQL Database                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐         │
│  │  users   │  │  hotels  │  │  rooms   │  │ bookings │         │
│  │  table   │  │  table   │  │  table   │  │  table   │         │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘         │
│                                                                    │
│  Flyway Migrations - Version Control                              │
└────────────────────────────────────────────────────────────────────┘
```

## API Request Flow

### Example: Guest Creates a Booking

```
┌─────────┐
│  Guest  │
│  Client │
└────┬────┘
     │
     │ 1. POST /api/bookings
     │    Authorization: Bearer {token}
     │    Body: { roomId, checkInDate, checkOutDate, guests }
     │
     ▼
┌────────────────┐
│  Spring Boot   │
│  Application   │
└────┬───────────┘
     │
     │ 2. JWT Filter validates token
     │    Extracts user context
     │
     ▼
┌────────────────────┐
│ BookingController  │
│  @PreAuthorize     │
│  ("GUEST")         │
└────┬───────────────┘
     │
     │ 3. Validates input
     │    Calls service
     │
     ▼
┌──────────────────┐
│ BookingService   │
│ Impl             │
└────┬─────────────┘
     │
     │ 4. Business Logic:
     │    ├─ Validate customer exists
     │    ├─ Validate room exists
     │    ├─ Check room availability
     │    ├─ Validate dates
     │    ├─ Check guest count
     │    ├─ Check booking conflicts
     │    ├─ Calculate total amount
     │    └─ Generate booking reference
     │
     ▼
┌───────────────────┐
│ BookingRepository │
└────┬──────────────┘
     │
     │ 5. Save booking to database
     │
     ▼
┌────────────┐
│ PostgreSQL │
│  Database  │
└────┬───────┘
     │
     │ 6. Return booking entity
     │
     ▼
┌──────────────────┐
│ BookingService   │
│ Maps to DTO      │
└────┬─────────────┘
     │
     │ 7. Return BookingResponse DTO
     │
     ▼
┌────────────────────┐
│ BookingController  │
│ Returns 201        │
└────┬───────────────┘
     │
     │ 8. HTTP Response
     │    Status: 201 Created
     │    Body: BookingResponse JSON
     │
     ▼
┌─────────┐
│  Guest  │
│  Client │
└─────────┘
```

## Component Relationships

```
┌─────────────────────────────────────────────────────────────┐
│                        Domain Model                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────┐         ┌────────┐         ┌────────┐          │
│  │  User  │────────▶│ Hotel  │────────▶│  Room  │          │
│  └────────┘         └────────┘         └────────┘          │
│      │                                       │               │
│      │ manages                               │               │
│      │                                       │               │
│      │                                       ▼               │
│      │                                  ┌─────────┐         │
│      └─────────────────────────────────▶│ Booking │         │
│                  books                  └─────────┘         │
│                                                              │
└─────────────────────────────────────────────────────────────┘

Relationships:
- User (MANAGER) → Hotel (1:N) - A manager can manage multiple hotels
- Hotel → Room (1:N) - A hotel has multiple rooms
- User (GUEST) → Booking (1:N) - A guest can have multiple bookings
- Room → Booking (1:N) - A room can have multiple bookings
```

## Security Flow

```
┌──────────────┐
│ Client makes │
│ API request  │
└──────┬───────┘
       │
       │ Include: Authorization: Bearer {JWT}
       │
       ▼
┌───────────────────────┐
│ JwtAuthenticationFilter│
└───────┬───────────────┘
       │
       │ Extract token from header
       │
       ▼
┌───────────────────────┐
│  JwtTokenProvider     │
└───────┬───────────────┘
       │
       │ Validate token
       │ Extract userId
       │
       ▼
┌───────────────────────┐
│ CustomUserDetails     │
│ Service               │
└───────┬───────────────┘
       │
       │ Load user by ID
       │ Get roles/authorities
       │
       ▼
┌───────────────────────┐
│ SecurityContext       │
│ Set authentication    │
└───────┬───────────────┘
       │
       │ User authenticated
       │
       ▼
┌───────────────────────┐
│ @PreAuthorize         │
│ Check user authority  │
└───────┬───────────────┘
       │
       ├─ ADMIN? ──→ Allow hotel management
       ├─ MANAGER? ─→ Allow room management
       └─ GUEST? ───→ Allow booking creation
       │
       ▼
┌───────────────────────┐
│ Controller method     │
│ executes              │
└───────────────────────┘
```

## Data Flow: Room Availability Check

```
┌──────────────────────────────────────────────────────────────┐
│ GET /api/hotels/{id}/rooms?checkIn=2026-09-01&checkOut=...  │
└────────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
        ┌───────────────────────────┐
        │  1. Get all active rooms  │
        │     for hotel             │
        └───────────┬───────────────┘
                    │
                    ▼
        ┌───────────────────────────┐
        │  2. For each room:        │
        │     Query bookings        │
        └───────────┬───────────────┘
                    │
                    ▼
        ┌───────────────────────────────────────┐
        │  3. Find conflicting bookings:        │
        │     WHERE room_id = ?                 │
        │     AND status NOT IN ('CANCELLED')   │
        │     AND (checkIn <= ? AND checkOut >= ?)│
        └───────────┬───────────────────────────┘
                    │
                    ▼
        ┌───────────────────────────┐
        │  4. Filter rooms:         │
        │     Keep only rooms       │
        │     with no conflicts     │
        └───────────┬───────────────┘
                    │
                    ▼
        ┌───────────────────────────┐
        │  5. Map to RoomResponse   │
        │     DTOs                  │
        └───────────┬───────────────┘
                    │
                    ▼
        ┌───────────────────────────┐
        │  6. Return list of        │
        │     available rooms       │
        └───────────────────────────┘
```

## Exception Handling Flow

```
┌────────────────┐
│ API Request    │
└───────┬────────┘
        │
        ▼
┌─────────────────┐
│ Controller      │
│ Method          │
└───────┬─────────┘
        │
        │ Throws exception
        │
        ▼
┌────────────────────────────┐
│ GlobalExceptionHandler     │
│ @RestControllerAdvice      │
└───────┬────────────────────┘
        │
        ├─ ResourceNotFoundException? → 404
        ├─ IllegalArgumentException? ─→ 400
        ├─ IllegalStateException? ────→ 400
        ├─ ValidationException? ──────→ 400
        └─ Other? ────────────────────→ 500
        │
        ▼
┌─────────────────────────────┐
│ ErrorResponse               │
│ {                           │
│   timestamp,                │
│   path,                     │
│   error,                    │
│   message,                  │
│   details                   │
│ }                           │
└───────┬─────────────────────┘
        │
        ▼
┌─────────────────┐
│ HTTP Response   │
│ with error JSON │
└─────────────────┘
```

## Deployment Architecture (Suggested)

```
┌────────────────────────────────────────────────────┐
│                    Load Balancer                   │
└───────────────────┬────────────────────────────────┘
                    │
        ┌───────────┴───────────┐
        │                       │
┌───────▼──────┐        ┌───────▼──────┐
│  App Server  │        │  App Server  │
│  Instance 1  │        │  Instance 2  │
└───────┬──────┘        └───────┬──────┘
        │                       │
        └───────────┬───────────┘
                    │
            ┌───────▼────────┐
            │   PostgreSQL   │
            │   Database     │
            │   (Primary)    │
            └───────┬────────┘
                    │
            ┌───────▼────────┐
            │   PostgreSQL   │
            │   Database     │
            │   (Replica)    │
            └────────────────┘

Additional Components:
- Redis for caching (future)
- Message Queue for notifications (future)
- File Storage for images (S3/Azure Blob)
```

## Technology Stack

```
┌─────────────────────────────────────────────┐
│            Frontend Layer                   │
│  React/Angular (Future)                     │
└─────────────────┬───────────────────────────┘
                  │
                  │ REST API
                  │
┌─────────────────▼───────────────────────────┐
│         Application Layer                   │
│  ┌───────────────────────────────────┐     │
│  │ Spring Boot 4.1.1                 │     │
│  │ - Spring Web                      │     │
│  │ - Spring Security                 │     │
│  │ - Spring Data JPA                 │     │
│  │ - Spring Validation               │     │
│  └───────────────────────────────────┘     │
│  ┌───────────────────────────────────┐     │
│  │ Libraries                         │     │
│  │ - Lombok                          │     │
│  │ - JJWT 0.12.6                     │     │
│  │ - Springdoc OpenAPI 2.8.6         │     │
│  └───────────────────────────────────┘     │
└─────────────────┬───────────────────────────┘
                  │
                  │ JDBC
                  │
┌─────────────────▼───────────────────────────┐
│         Persistence Layer                   │
│  ┌───────────────────────────────────┐     │
│  │ PostgreSQL 18.6                   │     │
│  │ - Flyway Migrations               │     │
│  │ - Indexes                         │     │
│  │ - Constraints                     │     │
│  └───────────────────────────────────┘     │
└─────────────────────────────────────────────┘
```

---

*This architecture follows industry best practices for scalable, maintainable, and secure REST API development.*

