# StayEase - End-to-End API Testing Guide

> **Purpose**: Step-by-step manual test walkthrough covering every controller/endpoint with
> ready-to-use sample payloads **and runnable curl commands**. Follow the steps **in order** —
> later steps reuse IDs/tokens captured in earlier ones.

---

## 📚 Table of Contents
1. [Prerequisites](#prerequisites)
2. [How the curl Examples Work](#how-the-curl-examples-work)
3. [Important: Role Bootstrapping](#important-role-bootstrapping)
4. [Step 0 - Start the Application](#step-0---start-the-application)
5. [Step 1 - Bootstrap ADMIN & MANAGER Users (SQL)](#step-1---bootstrap-admin--manager-users-sql)
6. [Step 2 - Auth APIs](#step-2---auth-apis)
7. [Step 3 - Admin: Hotel Management](#step-3---admin-hotel-management)
8. [Step 4 - Manager: Room Management](#step-4---manager-room-management)
9. [Step 5 - Guest: Register & Browse](#step-5---guest-register--browse)
10. [Step 6 - Guest: Booking Lifecycle](#step-6---guest-booking-lifecycle)
11. [Step 7 - Manager: View Bookings](#step-7---manager-view-bookings)
12. [Step 8 - Negative / Security Test Cases](#step-8---negative--security-test-cases)
13. [Full Endpoint Checklist](#full-endpoint-checklist)
14. [Resetting Data for a Fresh Demo](#resetting-data-for-a-fresh-demo)

---

## Prerequisites
- App running locally on `http://localhost:8080` (no context-path).
- Postgres reachable at `localhost:5432/postgres` (see `application-local.yml`).
- A REST client: **Swagger UI** (`http://localhost:8080/swagger-ui.html`), **Postman**, or `curl`.
- DB access (pgAdmin/DBeaver/psql) to run the one-time bootstrap SQL in Step 1.

---

## How the curl Examples Work

Every request below is given as a **plain, standard curl command** — the same syntax works if
you run it in a terminal (bash/macOS/Linux/WSL/Git Bash), **and it can be pasted directly into
Postman**:

**To import a command into Postman:**
1. Open Postman → **Import** (top-left) → **Raw text** tab.
2. Paste the whole `curl ...` command.
3. Click **Continue** → **Import**. Postman builds the request (method, URL, headers, body) for you.
4. Click **Send**.

**About the `{{variable}}` placeholders:** commands reuse values captured from earlier responses
(tokens, IDs) using Postman's double-curly-brace variable syntax, e.g. `{{adminToken}}`,
`{{hotel1Id}}`. To use them:
1. In Postman, create an **Environment** (top-right dropdown → *No Environment* → **+**), name it
   e.g. `StayEase Local`, and select it.
2. Add each variable used below (`adminToken`, `mgr1Token`, `mgr2Token`, `guest1Token`,
   `guest2Token`, `hotel1Id`, `hotel2Id`, `hotel3Id`, `room101Id`, `room201Id`, `room301Id`,
   `booking1Id`, `booking2Id`) with an empty **Current value** — each is called out with a
   👉 note right after the request that produces it.
3. After sending a request, copy the value from the JSON response (e.g. `token`, `hotelId`) into
   the matching environment variable's **Current value**, then **Save**.
4. Every later request using `{{thatVariable}}` will now automatically substitute it.

If you'd rather not set up an environment, just copy the response value and **manually replace**
the `{{placeholder}}` text in the next command before sending it — either approach works.

**Running from a terminal instead of Postman:**
- macOS/Linux/WSL/Git Bash: paste the commands as-is (replace `{{placeholder}}` with the real
  value, or `export TOKEN=...` and use `$TOKEN`).
- **Windows PowerShell users**: PowerShell's native argument parser can mangle embedded JSON
  double-quotes when calling `curl.exe` directly. The simplest fix is to run these commands from
  **Git Bash** (ships with Git for Windows) or **WSL** instead of PowerShell, or paste them into
  Postman as described above.

---

## Important: Role Bootstrapping
`POST /api/auth/register` **always** creates a user with role `GUEST` (hard-coded in
`AuthServiceImpl`). There is **no API** to self-register or promote a user to `MANAGER`/`ADMIN`.
So the very first ADMIN and MANAGER accounts must be inserted directly into the `users` table
(Step 1). After that, ADMIN can create hotels referencing any MANAGER's `user_id`.

---

## Step 0 - Start the Application
```powershell
mvn spring-boot:run
```
- API base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

## Step 1 - Bootstrap ADMIN & MANAGER Users (SQL)

Run this once against your Postgres DB. Password for **all** accounts below is `password`
(BCrypt hash pre-generated for you):

```sql
INSERT INTO users (user_id, full_name, email_address, password_hash, user_role, is_active)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'Admin User',   'admin@stayease.com',
   '$2b$10$nEdhnTGjMTQMX4auILt1Au/Z0dyGsM1DHSwAY36Mq5PTD5fbYn9hK', 'ADMIN',   true),
  ('22222222-2222-2222-2222-222222222222', 'John Manager', 'mgr1@stayease.com',
   '$2b$10$nEdhnTGjMTQMX4auILt1Au/Z0dyGsM1DHSwAY36Mq5PTD5fbYn9hK', 'MANAGER', true),
  ('33333333-3333-3333-3333-333333333333', 'Sarah Manager', 'mgr2@stayease.com',
   '$2b$10$nEdhnTGjMTQMX4auILt1Au/Z0dyGsM1DHSwAY36Mq5PTD5fbYn9hK', 'MANAGER', true);
```

| Role | Email | Password | user_id |
|------|-------|----------|---------|
| ADMIN | `admin@stayease.com` | `password` | `11111111-1111-1111-1111-111111111111` |
| MANAGER | `mgr1@stayease.com` | `password` | `22222222-2222-2222-2222-222222222222` |
| MANAGER | `mgr2@stayease.com` | `password` | `33333333-3333-3333-3333-333333333333` |

---

## Step 2 - Auth APIs

### 2.1 Login as ADMIN
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "admin@stayease.com",
  "password": "password"
}
```
**Expected**: `200 OK`, response body contains `token`.

**curl:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@stayease.com","password":"password"}'
```
👉 Copy the `token` from the response into a Postman variable named `adminToken` (or your own `$ADMIN_TOKEN`).

### 2.2 Login as MANAGER (mgr1)
```
POST http://localhost:8080/api/auth/login
{
  "email": "mgr1@stayease.com",
  "password": "password"
}
```
**curl:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"mgr1@stayease.com","password":"password"}'
```
👉 Copy `token` into Postman variable `mgr1Token`.

### 2.3 Login as MANAGER (mgr2)
```
POST http://localhost:8080/api/auth/login
{
  "email": "mgr2@stayease.com",
  "password": "password"
}
```
**curl:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"mgr2@stayease.com","password":"password"}'
```
👉 Copy `token` into Postman variable `mgr2Token`.

### 2.4 Logout (any token)
```
POST http://localhost:8080/api/auth/logout
Authorization: `Bearer` {adminToken}
```
**Expected**: `204 No Content`.

**curl:**
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer {{adminToken}}"
```
*(Postman will show this as "Bearer Token" auth automatically once imported — you can also use
the Authorization tab → type "Bearer Token" → paste `{{adminToken}}`.)*

---

## Step 3 - Admin: Hotel Management
All requests below authenticate with `{{adminToken}}` from Step 2.1.

### 3.1 Create Hotel #1 (managed by mgr1)
```
POST http://localhost:8080/api/hotels
{
  "hotelName": "Sunset Palms",
  "description": "Beachfront resort with pool",
  "streetAddress": "Calangute Beach Road",
  "cityName": "Goa",
  "stateName": "Goa",
  "countryName": "India",
  "postalCode": "403516",
  "contactPhone": "+91-832-555-0101",
  "contactEmail": "info@sunsetpalms.com",
  "starRating": 4,
  "coverImageUrl": "https://picsum.photos/seed/hotel1/400",
  "managerId": "22222222-2222-2222-2222-222222222222",
  "isActive": true
}
```
**Expected**: `201 Created`.

**curl:**
```bash
curl -X POST http://localhost:8080/api/hotels \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {{adminToken}}" \
  -d '{
    "hotelName": "Sunset Palms",
    "description": "Beachfront resort with pool",
    "streetAddress": "Calangute Beach Road",
    "cityName": "Goa",
    "stateName": "Goa",
    "countryName": "India",
    "postalCode": "403516",
    "contactPhone": "+91-832-555-0101",
    "contactEmail": "info@sunsetpalms.com",
    "starRating": 4,
    "coverImageUrl": "https://picsum.photos/seed/hotel1/400",
    "managerId": "22222222-2222-2222-2222-222222222222",
    "isActive": true
  }'
```
👉 Copy `hotelId` from the response into Postman variable `hotel1Id`.

### 3.2 Create Hotel #2 (managed by mgr1, same city — for search testing)
```
POST http://localhost:8080/api/hotels
{
  "hotelName": "City Inn",
  "description": "Budget-friendly city centre hotel",
  "cityName": "Goa",
  "countryName": "India",
  "starRating": 3,
  "coverImageUrl": "https://picsum.photos/seed/hotel2/400",
  "managerId": "22222222-2222-2222-2222-222222222222",
  "isActive": true
}
```
**curl:**
```bash
curl -X POST http://localhost:8080/api/hotels \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {{adminToken}}" \
  -d '{
    "hotelName": "City Inn",
    "description": "Budget-friendly city centre hotel",
    "cityName": "Goa",
    "countryName": "India",
    "starRating": 3,
    "coverImageUrl": "https://picsum.photos/seed/hotel2/400",
    "managerId": "22222222-2222-2222-2222-222222222222",
    "isActive": true
  }'
```
👉 Copy `hotelId` into Postman variable `hotel2Id`.

### 3.3 Create Hotel #3 (managed by mgr2, different city)
```
POST http://localhost:8080/api/hotels
{
  "hotelName": "Marine Bay Hotel",
  "description": "Modern hotel near the harbour",
  "cityName": "Mumbai",
  "countryName": "India",
  "starRating": 4,
  "coverImageUrl": "https://picsum.photos/seed/hotel3/400",
  "managerId": "33333333-3333-3333-3333-333333333333",
  "isActive": true
}
```
**curl:**
```bash
curl -X POST http://localhost:8080/api/hotels \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {{adminToken}}" \
  -d '{
    "hotelName": "Marine Bay Hotel",
    "description": "Modern hotel near the harbour",
    "cityName": "Mumbai",
    "countryName": "India",
    "starRating": 4,
    "coverImageUrl": "https://picsum.photos/seed/hotel3/400",
    "managerId": "33333333-3333-3333-3333-333333333333",
    "isActive": true
  }'
```
👉 Copy `hotelId` into Postman variable `hotel3Id`.

### 3.4 Get all hotels (public)
```
GET http://localhost:8080/api/hotels?city=Goa
```
**Expected**: `200 OK`, array with `hotel1Id` and `hotel2Id`.

**curl:**
```bash
curl "http://localhost:8080/api/hotels?city=Goa"
```

### 3.5 Get hotel by ID (public)
```
GET http://localhost:8080/api/hotels/{hotel1Id}
```
**curl:**
```bash
curl "http://localhost:8080/api/hotels/{{hotel1Id}}"
```

### 3.6 Update hotel (admin only)
```
PUT http://localhost:8080/api/hotels/{hotel1Id}
{
  "hotelName": "Sunset Palms Resort",
  "cityName": "Goa",
  "countryName": "India",
  "starRating": 5,
  "managerId": "22222222-2222-2222-2222-222222222222",
  "isActive": true
}
```
**Expected**: `200 OK`, `starRating` updated to `5`.

**curl:**
```bash
curl -X PUT "http://localhost:8080/api/hotels/{{hotel1Id}}" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {{adminToken}}" \
  -d '{
    "hotelName": "Sunset Palms Resort",
    "cityName": "Goa",
    "countryName": "India",
    "starRating": 5,
    "managerId": "22222222-2222-2222-2222-222222222222",
    "isActive": true
  }'
```

### 3.7 Delete hotel (admin only) — *skip for now, test at the end*
```
DELETE http://localhost:8080/api/hotels/{hotel3Id}
```
**Expected**: `204 No Content`.

**curl:**
```bash
curl -X DELETE "http://localhost:8080/api/hotels/{{hotel3Id}}" \
  -H "Authorization: Bearer {{adminToken}}"
```

---

## Step 4 - Manager: Room Management
All requests below authenticate with `{{mgr1Token}}` (mgr1 owns hotel1 & hotel2).

### 4.1 Create Room 101 (Double) in Hotel 1
```
POST http://localhost:8080/api/hotels/{hotel1Id}/rooms
{
  "roomNumber": "101",
  "roomType": "DOUBLE",
  "roomDescription": "Double room with sea view",
  "pricePerNight": 3500.00,
  "maxOccupancy": 2,
  "roomImageUrl": "https://picsum.photos/seed/room101/400",
  "isAvailable": true
}
```
**Expected**: `201 Created`.

**curl:**
```bash
curl -X POST "http://localhost:8080/api/hotels/{{hotel1Id}}/rooms" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {{mgr1Token}}" \
  -d '{
    "roomNumber": "101",
    "roomType": "DOUBLE",
    "roomDescription": "Double room with sea view",
    "pricePerNight": 3500.00,
    "maxOccupancy": 2,
    "roomImageUrl": "https://picsum.photos/seed/room101/400",
    "isAvailable": true
  }'
```
👉 Copy `roomId` into Postman variable `room101Id`.

### 4.2 Create Room 201 (Suite) in Hotel 1
```
POST http://localhost:8080/api/hotels/{hotel1Id}/rooms
{
  "roomNumber": "201",
  "roomType": "SUITE",
  "roomDescription": "Luxury suite with balcony",
  "pricePerNight": 7000.00,
  "maxOccupancy": 3,
  "isAvailable": true
}
```
**curl:**
```bash
curl -X POST "http://localhost:8080/api/hotels/{{hotel1Id}}/rooms" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {{mgr1Token}}" \
  -d '{
    "roomNumber": "201",
    "roomType": "SUITE",
    "roomDescription": "Luxury suite with balcony",
    "pricePerNight": 7000.00,
    "maxOccupancy": 3,
    "isAvailable": true
  }'
```
👉 Copy `roomId` into Postman variable `room201Id`.

### 4.3 Create Room 101 (Single) in Hotel 2
```
POST http://localhost:8080/api/hotels/{hotel2Id}/rooms
{
  "roomNumber": "101",
  "roomType": "SINGLE",
  "pricePerNight": 1500.00,
  "maxOccupancy": 1,
  "isAvailable": true
}
```
**curl:**
```bash
curl -X POST "http://localhost:8080/api/hotels/{{hotel2Id}}/rooms" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {{mgr1Token}}" \
  -d '{
    "roomNumber": "101",
    "roomType": "SINGLE",
    "pricePerNight": 1500.00,
    "maxOccupancy": 1,
    "isAvailable": true
  }'
```
👉 Copy `roomId` into Postman variable `room301Id`.

### 4.4 Get available rooms for Hotel 1 (public — no token needed)
```
GET http://localhost:8080/api/hotels/{hotel1Id}/rooms?checkIn=2026-09-01&checkOut=2026-09-05
```
**Expected**: `200 OK`, array with `room101Id` and `room201Id`.

**curl:**
```bash
curl "http://localhost:8080/api/hotels/{{hotel1Id}}/rooms?checkIn=2026-09-01&checkOut=2026-09-05"
```

### 4.5 Update Room 101
```
PUT http://localhost:8080/api/rooms/{room101Id}
{
  "roomNumber": "101",
  "roomType": "DOUBLE",
  "pricePerNight": 3800.00,
  "maxOccupancy": 2,
  "isAvailable": true
}
```
**curl:**
```bash
curl -X PUT "http://localhost:8080/api/rooms/{{room101Id}}" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {{mgr1Token}}" \
  -d '{
    "roomNumber": "101",
    "roomType": "DOUBLE",
    "pricePerNight": 3800.00,
    "maxOccupancy": 2,
    "isAvailable": true
  }'
```

### 4.6 Toggle Room 201 status to inactive
```
PATCH http://localhost:8080/api/rooms/{room201Id}/status?isAvailable=false
```
**Expected**: `200 OK`, `isAvailable: false`. *(Toggle back to `true` before Step 6.)*

**curl:**
```bash
curl -X PATCH "http://localhost:8080/api/rooms/{{room201Id}}/status?isAvailable=false" \
  -H "Authorization: Bearer {{mgr1Token}}"

# Toggle back to true before Step 6:
curl -X PATCH "http://localhost:8080/api/rooms/{{room201Id}}/status?isAvailable=true" \
  -H "Authorization: Bearer {{mgr1Token}}"
```

### 4.7 Delete room — *skip for now, test at the end*
```
DELETE http://localhost:8080/api/rooms/{room301Id}
```
**curl:**
```bash
curl -X DELETE "http://localhost:8080/api/rooms/{{room301Id}}" \
  -H "Authorization: Bearer {{mgr1Token}}"
```

---

## Step 5 - Guest: Register & Browse

### 5.1 Register Guest #1
```
POST http://localhost:8080/api/auth/register
{
  "email": "guest1@example.com",
  "password": "Password@123",
  "name": "Rohit Verma"
}
```
**Expected**: `201 Created`, role defaults to `GUEST`.

**curl:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "guest1@example.com",
    "password": "Password@123",
    "name": "Rohit Verma"
  }'
```

### 5.2 Register Guest #2
```
POST http://localhost:8080/api/auth/register
{
  "email": "guest2@example.com",
  "password": "Password@123",
  "name": "Priya Nair"
}
```
**curl:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "guest2@example.com",
    "password": "Password@123",
    "name": "Priya Nair"
  }'
```

### 5.3 Login as Guest #1
```
POST http://localhost:8080/api/auth/login
{
  "email": "guest1@example.com",
  "password": "Password@123"
}
```
**curl:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"guest1@example.com","password":"Password@123"}'
```
👉 Copy `token` into Postman variable `guest1Token`.

### 5.4 Login as Guest #2
```
POST http://localhost:8080/api/auth/login
{
  "email": "guest2@example.com",
  "password": "Password@123"
}
```
**curl:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"guest2@example.com","password":"Password@123"}'
```
👉 Copy `token` into Postman variable `guest2Token`.

### 5.5 Browse hotels by city (public)
```
GET http://localhost:8080/api/hotels?city=Goa
```
**curl:**
```bash
curl "http://localhost:8080/api/hotels?city=Goa"
```

### 5.6 View hotel detail (public)
```
GET http://localhost:8080/api/hotels/{hotel1Id}
```
**curl:**
```bash
curl "http://localhost:8080/api/hotels/{{hotel1Id}}"
```

### 5.7 Check available rooms for dates (public)
```
GET http://localhost:8080/api/hotels/{hotel1Id}/rooms?checkIn=2026-09-10&checkOut=2026-09-12
```
**curl:**
```bash
curl "http://localhost:8080/api/hotels/{{hotel1Id}}/rooms?checkIn=2026-09-10&checkOut=2026-09-12"
```

---

## Step 6 - Guest: Booking Lifecycle

### 6.1 Guest #1 books Room 101 in Hotel 1
```
POST http://localhost:8080/api/bookings
{
  "roomId": "{room101Id}",
  "checkInDate": "2026-09-10",
  "checkOutDate": "2026-09-12",
  "numberOfGuests": 2,
  "specialRequests": "Late check-in expected"
}
```
**Expected**: `201 Created`. `totalAmount` = 2 nights × 3800.00 = **7600.00**.

**curl:**
```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {{guest1Token}}" \
  -d '{
    "roomId": "{{room101Id}}",
    "checkInDate": "2026-09-10",
    "checkOutDate": "2026-09-12",
    "numberOfGuests": 2,
    "specialRequests": "Late check-in expected"
  }'
```
👉 Copy `bookingId` into Postman variable `booking1Id`.

### 6.2 Verify room no longer available for overlapping dates
```
GET http://localhost:8080/api/hotels/{hotel1Id}/rooms?checkIn=2026-09-11&checkOut=2026-09-13
```
**Expected**: `room101Id` should **NOT** appear in the results (overlaps booking1).

**curl:**
```bash
curl "http://localhost:8080/api/hotels/{{hotel1Id}}/rooms?checkIn=2026-09-11&checkOut=2026-09-13"
```

### 6.3 Guest #2 books Room 201 (Suite) — different dates
```
POST http://localhost:8080/api/bookings
{
  "roomId": "{room201Id}",
  "checkInDate": "2026-09-15",
  "checkOutDate": "2026-09-18",
  "numberOfGuests": 3
}
```
**Expected**: `totalAmount` = 3 × 7000.00 = **21000.00**.

**curl:**
```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {{guest2Token}}" \
  -d '{
    "roomId": "{{room201Id}}",
    "checkInDate": "2026-09-15",
    "checkOutDate": "2026-09-18",
    "numberOfGuests": 3
  }'
```
👉 Copy `bookingId` into Postman variable `booking2Id`.

### 6.4 Guest #1 views own bookings
```
GET http://localhost:8080/api/bookings/mine
```
**Expected**: only `booking1Id` shown (not `booking2Id`, which belongs to guest2).

**curl:**
```bash
curl http://localhost:8080/api/bookings/mine \
  -H "Authorization: Bearer {{guest1Token}}"
```

### 6.5 Guest #1 cancels booking1
```
PUT http://localhost:8080/api/bookings/{booking1Id}/cancel
```
**Expected**: `200 OK`, `status: CANCELLED`.

**curl:**
```bash
curl -X PUT "http://localhost:8080/api/bookings/{{booking1Id}}/cancel" \
  -H "Authorization: Bearer {{guest1Token}}"
```

### 6.6 Verify room reappears as available
```
GET http://localhost:8080/api/hotels/{hotel1Id}/rooms?checkIn=2026-09-10&checkOut=2026-09-12
```
**Expected**: `room101Id` now appears again (booking cancelled).

**curl:**
```bash
curl "http://localhost:8080/api/hotels/{{hotel1Id}}/rooms?checkIn=2026-09-10&checkOut=2026-09-12"
```

---

## Step 7 - Manager: View Bookings

### 7.1 mgr1 views upcoming bookings for their hotels
```
GET http://localhost:8080/api/manager/bookings
```
**Expected**: `200 OK`, includes `booking2Id` (Hotel 1, still CONFIRMED); booking1 shows as CANCELLED.

**curl:**
```bash
curl http://localhost:8080/api/manager/bookings \
  -H "Authorization: Bearer {{mgr1Token}}"
```

### 7.2 mgr2 views their bookings (should be empty — no bookings on Hotel 3)
```
GET http://localhost:8080/api/manager/bookings
```
**Expected**: `200 OK`, empty array `[]`.

**curl:**
```bash
curl http://localhost:8080/api/manager/bookings \
  -H "Authorization: Bearer {{mgr2Token}}"
```

### 7.3 ADMIN also has manager-level visibility
```
GET http://localhost:8080/api/manager/bookings
```
**Expected**: `200 OK` (endpoint allows `MANAGER` or `ADMIN`).

**curl:**
```bash
curl http://localhost:8080/api/manager/bookings \
  -H "Authorization: Bearer {{adminToken}}"
```

---

## Step 8 - Negative / Security Test Cases

| # | Test | Request | Expected |
|---|------|---------|----------|
| 8.1 | Guest tries to create a hotel | `POST /api/hotels` with `guest1Token` | `403 Forbidden` |
| 8.2 | Guest tries to create a room | `POST /api/hotels/{hotel1Id}/rooms` with `guest1Token` | `403 Forbidden` |
| 8.3 | No token on protected endpoint | `POST /api/bookings` with no token | `401 Unauthorized` |
| 8.4 | Manager tries admin-only hotel delete | `DELETE /api/hotels/{hotel1Id}` with `mgr1Token` | `403 Forbidden` |
| 8.5 | Guest cancels someone else's booking | `PUT /api/bookings/{booking2Id}/cancel` with `guest1Token` | `403`/`404` (ownership check) |
| 8.6 | Register with duplicate email | `POST /api/auth/register` with `guest1@example.com` again | `409 Conflict` |
| 8.7 | Login with wrong password | `POST /api/auth/login` with wrong password | `401 Unauthorized` |
| 8.8 | Invalid booking dates (checkout before checkin) | `POST /api/bookings` bad dates | `400 Bad Request` |
| 8.9 | Book an already-booked room for overlapping dates | Repeat booking2's dates on `room201Id` | `400 Bad Request` |
| 8.10 | Missing required field | `POST /api/hotels` without `hotelName` | `400 Bad Request` |

**curl for each** (run after Steps 1-7; paste each into Postman individually and check the response
status code against the **Expected** column above):
```bash
# 8.1 Guest creates hotel -> expect 403
curl -i -X POST http://localhost:8080/api/hotels \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {{guest1Token}}" \
  -d '{"hotelName":"Hack Hotel","cityName":"Goa","countryName":"India","managerId":"22222222-2222-2222-2222-222222222222"}'

# 8.2 Guest creates room -> expect 403
curl -i -X POST "http://localhost:8080/api/hotels/{{hotel1Id}}/rooms" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {{guest1Token}}" \
  -d '{"roomNumber":"999","roomType":"SINGLE","pricePerNight":100.00,"maxOccupancy":1}'

# 8.3 No token -> expect 401
curl -i -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{"roomId":"00000000-0000-0000-0000-000000000000","checkInDate":"2026-10-01","checkOutDate":"2026-10-02","numberOfGuests":1}'

# 8.4 Manager deletes hotel -> expect 403
curl -i -X DELETE "http://localhost:8080/api/hotels/{{hotel1Id}}" \
  -H "Authorization: Bearer {{mgr1Token}}"

# 8.5 Guest1 cancels Guest2's booking -> expect 403/404
curl -i -X PUT "http://localhost:8080/api/bookings/{{booking2Id}}/cancel" \
  -H "Authorization: Bearer {{guest1Token}}"

# 8.6 Duplicate email registration -> expect 409
curl -i -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"guest1@example.com","password":"Password@123","name":"Duplicate"}'

# 8.7 Wrong password -> expect 401
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"guest1@example.com","password":"WrongPass1"}'

# 8.8 Checkout before checkin -> expect 400
curl -i -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {{guest1Token}}" \
  -d '{"roomId":"{{room101Id}}","checkInDate":"2026-09-12","checkOutDate":"2026-09-10","numberOfGuests":2}'

# 8.9 Overlapping booking on already-booked room -> expect 400
curl -i -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {{guest1Token}}" \
  -d '{"roomId":"{{room201Id}}","checkInDate":"2026-09-16","checkOutDate":"2026-09-17","numberOfGuests":1}'

# 8.10 Missing required field -> expect 400
curl -i -X POST http://localhost:8080/api/hotels \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {{adminToken}}" \
  -d '{"cityName":"Goa","countryName":"India","managerId":"22222222-2222-2222-2222-222222222222"}'
```
`-i` includes the response headers/status line in the output so you can see the HTTP status code
directly in the terminal (Postman shows the status code in its UI automatically, no flag needed).

Expected error format (from `GlobalExceptionHandler`):
```json
{
  "timestamp": "2026-08-27T12:00:00",
  "path": "/api/hotels",
  "error": "Bad Request",
  "message": "Validation failed",
  "details": { "hotelName": "Hotel name is required" }
}
```

---

## Full Endpoint Checklist

| Method | Endpoint | Auth | Tested in |
|--------|----------|------|-----------|
| POST | `/api/auth/register` | Public | 5.1, 5.2, 8.6 |
| POST | `/api/auth/login` | Public | 2.1-2.3, 5.3-5.4, 8.7 |
| POST | `/api/auth/logout` | Public | 2.4 |
| GET | `/api/hotels?city=` | Public | 3.4, 5.5 |
| GET | `/api/hotels/all` | ADMIN | — (see API_Guide.md) |
| GET | `/api/hotels/{id}` | Public | 3.5, 5.6 |
| POST | `/api/hotels` | ADMIN | 3.1-3.3, 8.1, 8.10 |
| PUT | `/api/hotels/{id}` | ADMIN | 3.6 |
| DELETE | `/api/hotels/{id}` | ADMIN | 3.7, 8.4 |
| GET | `/api/hotels/{id}/rooms?checkIn&checkOut` | Public | 4.4, 5.7, 6.2, 6.6 |
| GET | `/api/rooms/{id}` | Public | — (see API_Guide.md) |
| POST | `/api/hotels/{id}/rooms` | MANAGER/ADMIN | 4.1-4.3, 8.2 |
| PUT | `/api/rooms/{id}` | MANAGER/ADMIN | 4.5 |
| PATCH | `/api/rooms/{id}/status` | MANAGER/ADMIN | 4.6 |
| DELETE | `/api/rooms/{id}` | MANAGER/ADMIN | 4.7 |
| POST | `/api/bookings` | GUEST | 6.1, 6.3, 8.8, 8.9 |
| GET | `/api/bookings/mine` | GUEST | 6.4 |
| PUT | `/api/bookings/{id}/cancel` | GUEST (owner) | 6.5, 8.5 |
| GET | `/api/manager/hotels` | MANAGER/ADMIN | — (see API_Guide.md) |
| GET | `/api/manager/hotels/{id}/rooms` | MANAGER/ADMIN | — (see API_Guide.md) |
| GET | `/api/manager/bookings` | MANAGER/ADMIN | 7.1-7.3 |

---

## Resetting Data for a Fresh Demo

```sql
TRUNCATE TABLE bookings, hotel_amenities, rooms, hotels, users RESTART IDENTITY CASCADE;
```

Or run the helper script:
```powershell
cd scripts
npm install pg
node reset-db.js
```

Then re-run **Step 1** (bootstrap SQL) to recreate ADMIN/MANAGER accounts before your demo.
