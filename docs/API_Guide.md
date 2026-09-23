# StayEase - Complete API Guide

> **One-stop documentation for all APIs, authentication, and backend design**

---

## 📚 Table of Contents
1. [Technology Stack](#technology-stack)
2. [Design Patterns](#design-patterns)
3. [Authentication & Security](#authentication--security)
4. [API Endpoints](#api-endpoints)
5. [Request/Response Examples](#requestresponse-examples)
6. [Error Handling](#error-handling)
7. [Business Logic](#business-logic)

---

## Technology Stack

### Backend Framework
- **Java**: 21
- **Spring Boot**: 4.1.1
- **Spring MVC**: REST APIs (blocking)
- **Spring Data JPA**: Database persistence
- **Spring Security**: Authentication & authorization
- **Spring Validation**: Jakarta Bean Validation

### Database
- **PostgreSQL**: Primary database (all environments)
- **Flyway**: Database migration management

### Security & Auth
- **JWT**: Stateless authentication
- **BCrypt**: Password hashing
- **JJWT**: JWT library (0.12.6)

### Documentation & Testing
- **Springdoc OpenAPI**: Swagger UI (2.8.6)
- **JUnit 5 + Mockito**: Unit testing
- **JaCoCo**: Code coverage (80% minimum)

---

## Design Patterns

Our application implements multiple design patterns for clean, maintainable code:

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| **Layered Architecture** | Controller → Service → Repository → Entity | Separation of concerns |
| **Repository Pattern** | Spring Data JPA `@Query` repositories | Abstract data access |
| **DTO Pattern** | Request/Response DTOs separate from entities | API contract management |
| **Service Layer Pattern** | Interface + Impl separation (`AuthService`/`AuthServiceImpl`) | Abstraction, testability |
| **Builder Pattern** | Lombok `@Builder` on all entities & DTOs | Fluent object creation |
| **Factory Pattern** | `BookingReferenceGenerator.generate()` → `BK-YYYYMMDD-XXXXXXXX` | Centralized object creation |
| **Strategy Pattern** | Pricing: `nights × pricePerNight`; extensible for seasonal pricing | Extensible algorithms |
| **Dependency Injection** | Constructor injection via `@RequiredArgsConstructor` | Loose coupling, testability |
| **Filter/Chain of Responsibility** | `JwtAuthenticationFilter` in Spring Security chain | Pluggable authentication |
| **Singleton** | Spring-managed beans (`@Service`, `@Repository`, `@Component`) | Resource management |
| **Global Handler (Advice)** | `@RestControllerAdvice` in `GlobalExceptionHandler` | Consistent error responses |

---

## Authentication & Security

### JWT Authentication Flow

```
┌─────────┐                                              ┌──────────────┐
│ Client  │                                              │   Backend    │
└────┬────┘                                              └──────┬───────┘
     │                                                          │
     │  1. POST /api/auth/register                             │
     │     {email, password, name}                             │
     ├──────────────────────────────────────────────────────►  │
     │                                                          │
     │  2. Validate, Hash password (BCrypt), Save user         │
     │     Generate JWT token                                  │
     │  ◄──────────────────────────────────────────────────────┤
     │     201 Created {token, userId, role}                   │
     │                                                          │
     │  3. POST /api/auth/login                                │
     │     {email, password}                                   │
     ├──────────────────────────────────────────────────────►  │
     │                                                          │
     │  4. Validate credentials, Generate JWT                  │
     │  ◄──────────────────────────────────────────────────────┤
     │     200 OK {token, userId, role}                        │
     │                                                          │
     │  5. Protected API Request                               │
     │     Authorization: Bearer <JWT>                         │
     ├──────────────────────────────────────────────────────►  │
     │                                                          │
     │  6. Validate JWT, Extract user, Check role              │
     │  ◄──────────────────────────────────────────────────────┤
     │     200 OK {data}                                       │
     │                                                          │
```

### JWT Token Structure

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user-uuid",
    "email": "user@example.com",
    "role": "GUEST",
    "iat": 1724573400,
    "exp": 1724659800
  },
  "signature": "..."
}
```

### User Roles

| Role | Permissions |
|------|------------|
| **GUEST** | Create bookings, View own bookings, Cancel own bookings |
| **MANAGER** | Manage rooms for their hotels, View hotel bookings, Toggle room status |
| **ADMIN** | Full system access, Hotel CRUD, All manager permissions |

### Security Configuration

```yaml
# JWT Configuration
app:
  jwt:
    secret: ${JWT_SECRET:base64-encoded-secret}
    expiration-ms: 86400000  # 24 hours
```

### Password Security
- **Algorithm**: BCrypt with default strength (10 rounds)
- **Storage**: Passwords never stored in plain text
- **Transmission**: HTTPS only in production
- **Validation**: Min 8 characters, max 100 characters

---

## API Endpoints

### Auth APIs (Public)

#### 1. Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "john.doe@example.com",
  "password": "SecurePass123",
  "name": "John Doe"
}
```

**Response: 201 Created**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "tokenType": "Bearer",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "john.doe@example.com",
  "name": "John Doe",
  "role": "GUEST"
}
```

#### 2. Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john.doe@example.com",
  "password": "SecurePass123"
}
```

**Response: 200 OK** (Same as register)

#### 3. Logout
```http
POST /api/auth/logout
Authorization: Bearer <token>
```

**Response: 204 No Content**

---

### Hotel APIs

#### 1. List Hotels by City (Public)
```http
GET /api/hotels?city=New York
```

**Response: 200 OK**
```json
[
  {
    "hotelId": "uuid",
    "hotelName": "Grand Plaza Hotel",
    "cityName": "New York",
    "starRating": 5,
    "description": "Luxury 5-star hotel...",
    "coverImageUrl": "https://...",
    "isActive": true,
    "managerId": "uuid",
    "managerName": "Manager Name"
  }
]
```

#### 2. Get Hotel Details (Public)
```http
GET /api/hotels/{id}
```

#### 2a. Get All Hotels (ADMIN)
```http
GET /api/hotels/all
Authorization: ******
```

**Response: 200 OK** (Array of all hotels, regardless of city)

#### 3. Create Hotel (ADMIN)
```http
POST /api/hotels
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "hotelName": "Grand Plaza Hotel",
  "cityName": "New York",
  "countryName": "USA",
  "starRating": 5,
  "managerId": "manager-uuid",
  "description": "Luxury hotel in downtown",
  "contactEmail": "info@hotel.com",
  "contactPhone": "+1-212-555-1234"
}
```

**Response: 201 Created**

#### 4. Update Hotel (ADMIN)
```http
PUT /api/hotels/{id}
Authorization: Bearer <admin-token>
```

#### 5. Delete Hotel (ADMIN)
```http
DELETE /api/hotels/{id}
Authorization: Bearer <admin-token>
```

**Response: 204 No Content**

---

### Room APIs

#### 1. Get Available Rooms (Public)
```http
GET /api/hotels/{id}/rooms?checkIn=2026-09-01&checkOut=2026-09-05
```

#### 1a. Get Room by ID (Public)
```http
GET /api/rooms/{id}
```

**Response: 200 OK**
```json
[
  {
    "roomId": "uuid",
    "roomNumber": "101",
    "roomType": "DELUXE",
    "pricePerNight": 150.00,
    "maxOccupancy": 2,
    "roomDescription": "Spacious deluxe room",
    "isAvailable": true,
    "hotelId": "uuid",
    "hotelName": "Grand Plaza Hotel"
  }
]
```

#### 2. Create Room (MANAGER/ADMIN)
```http
POST /api/hotels/{id}/rooms
Authorization: Bearer <manager-token>
Content-Type: application/json

{
  "roomNumber": "101",
  "roomType": "DELUXE",
  "pricePerNight": 150.00,
  "maxOccupancy": 2,
  "roomDescription": "Spacious room with city view",
  "isAvailable": true
}
```

**Response: 201 Created**

#### 3. Update Room (MANAGER/ADMIN)
```http
PUT /api/rooms/{id}
Authorization: Bearer <manager-token>
```

#### 4. Delete Room (MANAGER/ADMIN)
```http
DELETE /api/rooms/{id}
Authorization: Bearer <manager-token>
```

**Response: 204 No Content**

#### 5. Toggle Room Status (MANAGER/ADMIN)
```http
PATCH /api/rooms/{id}/status?isAvailable=false
Authorization: Bearer <manager-token>
```

**Response: 200 OK**

---

### Manager APIs (MANAGER/ADMIN)

#### 1. Get Hotels Managed by Current User
```http
GET /api/manager/hotels
Authorization: ******
```

**Response: 200 OK** (Array of hotels where `managerId` matches the current user)

#### 2. Get All Rooms for a Managed Hotel (inventory view, incl. inactive)
```http
GET /api/manager/hotels/{hotelId}/rooms
Authorization: ******
```

**Response: 200 OK**

---

### Booking APIs

#### 1. Create Booking (GUEST)
```http
POST /api/bookings
Authorization: Bearer <guest-token>
Content-Type: application/json

{
  "roomId": "uuid",
  "checkInDate": "2026-09-01",
  "checkOutDate": "2026-09-05",
  "numberOfGuests": 2,
  "specialRequests": "Late check-in expected"
}
```

**Response: 201 Created**
```json
{
  "bookingId": "uuid",
  "bookingReference": "BK-20260825-A3F5K7M2",
  "checkInDate": "2026-09-01",
  "checkOutDate": "2026-09-05",
  "numberOfGuests": 2,
  "totalAmount": 600.00,
  "bookingStatus": "CONFIRMED",
  "roomNumber": "101",
  "roomType": "DELUXE",
  "hotelName": "Grand Plaza Hotel",
  "customerName": "John Doe"
}
```

#### 2. Get My Bookings (GUEST)
```http
GET /api/bookings/mine
Authorization: Bearer <guest-token>
```

**Response: 200 OK** (Array of bookings)

#### 3. Cancel Booking (GUEST)
```http
PUT /api/bookings/{id}/cancel
Authorization: Bearer <guest-token>
```

**Response: 200 OK** (Updated booking with CANCELLED status)

#### 4. Get Manager's Hotel Bookings (MANAGER)
```http
GET /api/manager/bookings
Authorization: Bearer <manager-token>
```

**Response: 200 OK** (Array of upcoming bookings)

---

## Error Handling

### Consistent Error Format

All errors return a consistent JSON structure:

```json
{
  "timestamp": "2026-08-25T12:30:00",
  "path": "/api/bookings",
  "error": "INVALID_REQUEST",
  "message": "Check-out date must be after check-in date",
  "details": {
    "field": "checkOutDate",
    "value": "2026-09-01"
  }
}
```

### HTTP Status Codes

| Status | Error Type | Description |
|--------|-----------|-------------|
| **400** | Bad Request | Validation error, invalid input |
| **401** | Unauthorized | Invalid or missing JWT token |
| **403** | Forbidden | Insufficient permissions |
| **404** | Not Found | Resource doesn't exist |
| **409** | Conflict | Duplicate email, room not available |
| **500** | Internal Server Error | Unexpected server error |

### Common Error Scenarios

#### 1. Validation Errors (400)
```json
{
  "timestamp": "2026-08-25T12:30:00",
  "path": "/api/auth/register",
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "details": {
    "email": "Email must be valid",
    "password": "Password must be at least 8 characters",
    "name": "Name must be between 2 and 100 characters"
  }
}
```

#### 2. Authentication Failure (401)
```json
{
  "error": "AUTHENTICATION_FAILED",
  "message": "Invalid email or password"
}
```

#### 3. Authorization Failure (403)
```json
{
  "error": "ACCESS_DENIED",
  "message": "You don't have permission to access this resource"
}
```

#### 4. Resource Not Found (404)
```json
{
  "error": "RESOURCE_NOT_FOUND",
  "message": "Hotel not found with hotelId : '3fa85f64-5717-4562-b3fc-2c963f66afa6'"
}
```

#### 5. Room Not Available (400)
```json
{
  "error": "INVALID_STATE",
  "message": "Room is not available for the selected dates"
}
```

---

## Business Logic

### Room Availability Algorithm

Checks if a room can be booked for specific dates:

```java
// A room is available if:
// 1. Room is active (isAvailable = true)
// 2. No overlapping non-cancelled bookings exist

// Overlap detection:
existing.checkInDate < requested.checkOutDate 
AND 
existing.checkOutDate > requested.checkInDate
```

**Query Logic:**
```sql
SELECT r FROM Room r
WHERE r.hotel.id = :hotelId
  AND r.isAvailable = true
  AND r.id NOT IN (
    SELECT b.room.id FROM Booking b
    WHERE b.status <> 'CANCELLED'
      AND b.checkInDate < :checkOut
      AND b.checkOutDate > :checkIn
  )
```

### Booking Creation Flow

```
1. Validate customer exists (from JWT)
2. Validate room exists and is active
3. Validate dates (future, valid range, max 30 nights)
4. Check guest count ≤ room max occupancy
5. Check for booking conflicts (availability query)
6. Calculate total amount:
   - nights = ChronoUnit.DAYS.between(checkIn, checkOut)
   - total = nights × pricePerNight
7. Generate unique booking reference (BK-YYYYMMDD-XXXXXXXX)
8. Create booking with CONFIRMED status
9. Save to database (@Transactional)
10. Return booking response
```

### Booking Reference Format

Pattern: `BK-YYYYMMDD-XXXXXXXX`

- **Prefix**: BK (Booking)
- **Date**: Current date (YYYYMMDD)
- **Random**: 8 alphanumeric characters
- **Example**: `BK-20260825-A3F5K7M2`

Generated by `BookingReferenceGenerator` utility class using Factory pattern.

### Price Calculation

```
Total Amount = Number of Nights × Price Per Night

Where:
- Number of Nights = checkOutDate - checkInDate (in days)
- Price Per Night = room.pricePerNight
```

### Booking Cancellation

```
1. Validate booking exists
2. Check booking belongs to user
3. Verify not already cancelled or completed
4. Check check-in date not in past
5. Update status to CANCELLED
6. Room automatically becomes available (excluded from availability query)
```

---

## Data Model

### Entity Relationships

```
User (MANAGER) ──────< manages >──── Hotel ──────< has >──── Room
User (GUEST)   ──────< books  >──── Booking ────< for >──── Room
                                     Hotel  ──────< has >──── HotelAmenity
```

### Key Entities

#### User
```java
{
  userId: UUID,
  emailAddress: String (unique),
  passwordHash: String (BCrypt),
  fullName: String,
  userRole: Enum (GUEST/MANAGER/ADMIN),
  isActive: Boolean,
  createdAt: LocalDateTime
}
```

#### Hotel
```java
{
  hotelId: UUID,
  hotelName: String,
  cityName: String,
  countryName: String,
  starRating: Integer (1-5),
  description: String,
  coverImageUrl: String,
  managerId: UUID (FK to User),
  isActive: Boolean,
  createdAt: LocalDateTime
}
```

#### Room
```java
{
  roomId: UUID,
  roomNumber: String (unique per hotel),
  roomType: Enum (SINGLE/DOUBLE/SUITE/DELUXE/FAMILY),
  pricePerNight: BigDecimal,
  maxOccupancy: Integer,
  roomDescription: String,
  roomImageUrl: String,
  isAvailable: Boolean,
  hotelId: UUID (FK to Hotel),
  createdAt: LocalDateTime
}
```

#### Booking
```java
{
  bookingId: UUID,
  bookingReference: String (unique),
  checkInDate: LocalDate,
  checkOutDate: LocalDate,
  numberOfGuests: Integer,
  totalAmount: BigDecimal,
  bookingStatus: Enum (PENDING/CONFIRMED/CANCELLED/COMPLETED),
  specialRequests: String,
  customerId: UUID (FK to User),
  roomId: UUID (FK to Room),
  createdAt: LocalDateTime
}
```

#### HotelAmenity
```java
{
  hotelId: UUID (composite PK, FK to Hotel),
  amenityName: String (composite PK),
  amenityDescription: String
}
```

---

## Validation Rules

### Registration
- **Email**: Valid format, max 255 chars, unique
- **Password**: Min 8 chars, max 100 chars
- **Name**: Min 2 chars, max 100 chars

### Hotel
- **Name**: Required, max 150 chars
- **City**: Required, max 100 chars
- **Country**: Required, max 100 chars
- **Star Rating**: 1-5
- **Manager**: Must have MANAGER or ADMIN role

### Room
- **Room Number**: Required, max 20 chars, unique per hotel
- **Room Type**: Required (enum value)
- **Price**: Required, positive number
- **Max Occupancy**: Required, min 1

### Booking
- **Room ID**: Required, must exist
- **Check-in Date**: Required, today or future
- **Check-out Date**: Required, after check-in
- **Number of Guests**: Required, min 1, ≤ room max occupancy
- **Booking Duration**: Max 30 nights

---

## API Access Matrix

| Endpoint | Public | GUEST | MANAGER | ADMIN |
|----------|--------|-------|---------|-------|
| POST /api/auth/register | ✅ | - | - | - |
| POST /api/auth/login | ✅ | - | - | - |
| POST /api/auth/logout | - | ✅ | ✅ | ✅ |
| GET /api/hotels | ✅ | - | - | - |
| GET /api/hotels/all | - | - | - | ✅ |
| GET /api/hotels/{id} | ✅ | - | - | - |
| POST /api/hotels | - | - | - | ✅ |
| PUT /api/hotels/{id} | - | - | - | ✅ |
| DELETE /api/hotels/{id} | - | - | - | ✅ |
| GET /api/hotels/{id}/rooms | ✅ | - | - | - |
| GET /api/rooms/{id} | ✅ | - | - | - |
| POST /api/hotels/{id}/rooms | - | - | ✅ | ✅ |
| PUT /api/rooms/{id} | - | - | ✅ | ✅ |
| DELETE /api/rooms/{id} | - | - | ✅ | ✅ |
| PATCH /api/rooms/{id}/status | - | - | ✅ | ✅ |
| POST /api/bookings | - | ✅ | - | - |
| GET /api/bookings/mine | - | ✅ | - | - |
| PUT /api/bookings/{id}/cancel | - | ✅ | - | - |
| GET /api/manager/hotels | - | - | ✅ | ✅ |
| GET /api/manager/hotels/{id}/rooms | - | - | ✅ | ✅ |
| GET /api/manager/bookings | - | - | ✅ | ✅ |

---

## API Documentation Access

### Swagger UI
- **URL**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`
- **Features**: Interactive API testing, schema documentation, example requests

### Health & Monitoring
- **Health Check**: `http://localhost:8080/actuator/health`
- **Application Info**: `http://localhost:8080/actuator/info`

---

## Configuration Profiles

### Local (Development)
```yaml
spring:
  profiles:
    active: local
datasource:
  url: jdbc:h2:mem:stayease
  driver-class-name: org.h2.Driver
sql:
  init:
    mode: always  # Auto-load schema and seed data
```

### Production
```yaml
spring:
  profiles:
    active: prod
datasource:
  url: ${DB_URL}
  username: ${DB_USERNAME}
  password: ${DB_PASSWORD}
  driver-class-name: org.postgresql.Driver
sql:
  init:
    mode: never  # No auto-loading
```

---

## Quick Reference

### Authentication Header
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Date Format
```
YYYY-MM-DD (ISO 8601)
Example: 2026-09-01
```

### UUID Format
```
Standard UUID v4
Example: 3fa85f64-5717-4562-b3fc-2c963f66afa6
```

### Booking Reference Format
```
BK-YYYYMMDD-XXXXXXXX
Example: BK-20260825-A3F5K7M2
```

---

**Document Version**: 2.1  
**Last Updated**: August 29, 2026
**Author**: Dinesh Sahu (dinesh.sahu@apexon.com)  
**Project**: StayEase Hotel Booking Service  
**Team**: Team 45

---

For visual architecture diagrams, see [Architecture.md](./Architecture.md)  
For high-level overview, see [High_Level_Design.md](./High_Level_Design.md)

