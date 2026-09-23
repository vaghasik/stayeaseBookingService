# StayEase - Frontend (UI) Components & Design

> Web UI for the StayEase hotel booking application. It talks to the **single StayEase backend service**
> over REST. This document is the frontend blueprint the team develops against.
>
> **Backend reference:** [API_Guide.md](./API_Guide.md) · [High_Level_Design.md](./High_Level_Design.md)

- **Framework:** React + Vite + TypeScript (Hooks + Context API) — *Angular alternative noted where relevant*
- **Routing:** React Router
- **HTTP:** Axios (single instance → **backend REST API**, JWT + error interceptors)
- **Styling:** CSS3 / CSS Modules (or your team's choice)
- **State:** Context API for Auth + Search; local component state elsewhere

---

## 1. Frontend Design Patterns

| Pattern | Where / Why |
|---------|-------------|
| **Container / Presentational** | `pages/` (data + logic) vs `components/` (pure UI) |
| **Provider (Context)** | `AuthContext`, `SearchContext` inject shared state |
| **Custom Hooks** | `useAuth`, `useSearch`, `useHotels` encapsulate reusable logic |
| **API client (Singleton)** | One Axios instance → the backend REST API; feature API modules |
| **Adapter** | `api/` maps backend DTOs ↔ UI types |
| **Facade** | Feature API modules (`hotelApi`, `bookingApi`) hide HTTP details |
| **Guard / HOC** | `ProtectedRoute`, `RoleRoute` wrap route access |
| **Compound components** | `Modal`, `Table`, `Toast` for reuse |
| **Observer** | React state/effect hooks reactively update on data changes |

> The UI talks to the **single backend service** (e.g., `VITE_API_BASE_URL=http://localhost:8080`).
> Auth (JWT) and error handling are centralised in the Axios instance's interceptors.

---

## 2. Project Structure (React)

```
src/
├── main.tsx / App.tsx        # Router + providers
├── api/                      # Axios instance + endpoint modules
│   ├── client.ts             # base axios, interceptors
│   ├── authApi.ts            # register, login, logout
│   ├── hotelApi.ts           # search hotels, CRUD (admin)
│   ├── roomApi.ts            # available rooms, CRUD (manager), toggle status
│   └── bookingApi.ts         # create, getMyBookings, cancel, managerBookings
├── context/                  # AuthContext, SearchContext
├── hooks/                    # useAuth, useSearch, custom data hooks
├── components/               # Reusable presentational components
├── pages/                    # Route-level components
├── routes/                   # Route config + ProtectedRoute / RoleRoute
├── types/                    # TS interfaces (Hotel, Room, Booking, User)
└── utils/                    # date helpers, price calc, formatters
```

**Angular equivalent:** `services/` (Auth, Hotel, Room, Booking), `components/`, `guards/` (AuthGuard, RoleGuard), `models/`.

---

## 3. Routing Map

| Path | Page | Access |
|------|------|--------|
| `/` | Home (search panel) | Public |
| `/hotels` | Hotel List (search results) | Public |
| `/hotels/:id` | Hotel Detail + available rooms | Public |
| `/login` | Login | Public |
| `/register` | Register | Public |
| `/my-stays` | My Stays (bookings) | GUEST (auth) |
| `/manager` | Manager Dashboard | MANAGER |
| `/admin` | Admin - Hotel Management | ADMIN |

`ProtectedRoute` guards auth-only pages; `RoleRoute` restricts by role and redirects unauthorized users.

---

## 4. Pages (route-level components)

### 4.1 Home (`/`)
- `SearchPanel` — City input, Check-in Date, Check-out Date, [Search Hotels]
- Validates dates (check-out > check-in, no past dates)
- On submit → store criteria in `SearchContext` → navigate to `/hotels`

### 4.2 Hotel List (`/hotels`)
- Reads search criteria; calls `GET /api/hotels?city=`
- Renders `HotelCard` grid: cover image, name, star rating (1–5), city, "from ₹X/night", [View Rooms]
- Empty + loading + error states

### 4.3 Hotel Detail (`/hotels/:id`)
- Hotel header: cover, name, description, star rating, address, contact info
- Calls `GET /api/hotels/{id}/rooms?checkIn=&checkOut=` with dates from `SearchContext`
- `RoomCard` list: roomNumber, roomType (SINGLE/DOUBLE/SUITE/DELUXE/FAMILY), price/night, max occupancy, [Book]
- Book flow: if not authenticated → redirect to `/login` (remember intent); else show booking review

### 4.4 Login / Register
- `LoginForm` — email, password → `POST /api/auth/login`, store token + user info
- `RegisterForm` — name, email, password → `POST /api/auth/register`
- Field validation + error toasts; redirect after success
- **Validation rules:** email (valid format), password (min 8 chars), name (min 2 chars)

### 4.5 My Stays (`/my-stays`) — GUEST
- Calls `GET /api/bookings/mine`
- Table: Booking Ref / Hotel / Room / Check-in / Check-out / Guests / Total / Status
- Status badges: PENDING (yellow), CONFIRMED (green), CANCELLED (red), COMPLETED (blue)
- [Cancel] on non-past bookings → `PUT /api/bookings/{id}/cancel` → refresh
- Show specialRequests if present

### 4.6 Manager Dashboard (`/manager`) — MANAGER
- **Room Management Tab:**
  - `RoomTable` — CRUD rooms (roomNumber, roomType, pricePerNight, maxOccupancy, isAvailable toggle)
  - `RoomFormModal` — create/edit room (`POST /api/hotels/{id}/rooms`, `PUT /api/rooms/{id}`)
  - Available toggle → `PATCH /api/rooms/{id}/status?isAvailable=true/false`
  - Room types dropdown: SINGLE, DOUBLE, SUITE, DELUXE, FAMILY
- **Upcoming Bookings Tab:**
  - Calls `GET /api/manager/bookings`
  - Shows upcoming bookings (CONFIRMED + PENDING) for manager's hotel, sorted by check-in date
  - Columns: Booking Ref / Guest Name / Guest Email / Room / Check-in / Check-out / Guests / Total / Status

### 4.7 Admin Panel (`/admin`) — ADMIN
- `HotelTable` — CRUD hotels
- `HotelFormModal` — hotelName, cityName, countryName, starRating, description, coverImageUrl, managerId (select from MANAGER users), contactEmail, contactPhone
- `POST/PUT/DELETE /api/hotels`
- Manager assignment: select from users with MANAGER role

---

## 5. Reusable Components
- `NavBar` — logo, links, auth-aware (login/logout, role-based links)
- `HotelCard`, `RoomCard`
- `DatePicker` / date range inputs (shared with SearchPanel)
- `Modal`, `ConfirmDialog`
- `Toast` / notification system (error + success)
- `Loader` / `Spinner`, `EmptyState`
- `Table` (generic) for stays/rooms/hotels
- `ProtectedRoute`, `RoleRoute`
- `BookingReview` — shows nights × rate = total before confirm
- `StatusBadge` — color-coded booking status (PENDING/CONFIRMED/CANCELLED/COMPLETED)
- `StarRating` — display 1–5 star rating

---

## 6. State Management (Context)

### AuthContext
- Holds: `user`, `token`, `role`, `isAuthenticated`
- `user` shape: `{ userId, email, fullName, role }`
- Actions: `login()`, `register()`, `logout()`
- Persists token (e.g., localStorage); hydrates on app load
- Exposes role for `RoleRoute` and conditional NavBar links

### SearchContext
- Holds: `city`, `checkInDate`, `checkOutDate`
- Carries selected dates from Home → Hotel List → Hotel Detail (used in availability call)

---

## 7. API Layer (Axios)

### client.ts
- Base URL from `VITE_API_BASE_URL` (default: `http://localhost:8080`)
- Request interceptor: attach `Authorization: Bearer <token>` from AuthContext
- Response interceptor: map error JSON to toasts, handle 401 → logout/redirect

### API Modules

| Module | Methods | Backend Endpoints |
|--------|---------|-------------------|
| `authApi` | `register(email, password, name)`, `login(email, password)`, `logout()` | POST /api/auth/* |
| `hotelApi` | `searchByCity(city)`, `getById(id)`, `create(data)`, `update(id, data)`, `delete(id)` | GET/POST/PUT/DELETE /api/hotels/* |
| `roomApi` | `getAvailable(hotelId, checkIn, checkOut)`, `create(hotelId, data)`, `update(roomId, data)`, `delete(roomId)`, `toggleStatus(roomId, isAvailable)` | /api/hotels/{id}/rooms, /api/rooms/* |
| `bookingApi` | `create(data)`, `getMyBookings()`, `cancel(bookingId)`, `getManagerBookings()` | /api/bookings/*, /api/manager/bookings |

### TypeScript Types (`types/`)

```typescript
// Enums
type UserRole = 'GUEST' | 'MANAGER' | 'ADMIN';
type RoomType = 'SINGLE' | 'DOUBLE' | 'SUITE' | 'DELUXE' | 'FAMILY';
type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';

// Models
interface User { userId: string; fullName: string; emailAddress: string; userRole: UserRole; }
interface Hotel { hotelId: string; hotelName: string; cityName: string; countryName: string; starRating: number; description: string; coverImageUrl: string; managerId: string; managerName: string; isActive: boolean; }
interface Room { roomId: string; roomNumber: string; roomType: RoomType; pricePerNight: number; maxOccupancy: number; roomDescription: string; isAvailable: boolean; hotelId: string; hotelName: string; }
interface Booking { bookingId: string; bookingReference: string; checkInDate: string; checkOutDate: string; numberOfGuests: number; totalAmount: number; bookingStatus: BookingStatus; specialRequests: string; customerId: string; customerName: string; roomId: string; roomNumber: string; roomType: RoomType; hotelId: string; hotelName: string; }

// Request shapes
interface BookingRequest { roomId: string; checkInDate: string; checkOutDate: string; numberOfGuests: number; specialRequests?: string; }
interface HotelRequest { hotelName: string; cityName: string; countryName: string; starRating: number; managerId: string; description?: string; /* ... */ }
interface RoomRequest { roomNumber: string; roomType: RoomType; pricePerNight: number; maxOccupancy: number; roomDescription?: string; isAvailable?: boolean; }
```

---

## 8. Key UX Rules
- Guest prompted to login/register only at booking time (not while browsing).
- Cancelling a booking frees the room; re-search should show it available.
- Show user-friendly error toasts from the structured API error JSON.
- Booking max duration: 30 nights (enforced by backend).
- Number of guests cannot exceed room's maxOccupancy.
- **Stretch:** star-rating filter on results; live nights + total price as dates change; printable booking confirmation.

---

## 9. Backend API Quick Reference

### Public Endpoints (no auth needed)
| Endpoint | Description |
|----------|-------------|
| `POST /api/auth/register` | Register (returns token) |
| `POST /api/auth/login` | Login (returns token) |
| `GET /api/hotels?city=` | Search hotels |
| `GET /api/hotels/{id}` | Hotel details |
| `GET /api/hotels/{id}/rooms?checkIn=&checkOut=` | Available rooms |
| `GET /api/rooms/{id}` | Room details |

### Authenticated Endpoints
| Endpoint | Role | Description |
|----------|------|-------------|
| `POST /api/bookings` | GUEST | Create booking |
| `GET /api/bookings/mine` | GUEST | My bookings |
| `PUT /api/bookings/{id}/cancel` | GUEST | Cancel booking |
| `GET /api/hotels/all` | ADMIN | List all hotels |
| `POST /api/hotels` | ADMIN | Create hotel |
| `PUT /api/hotels/{id}` | ADMIN | Update hotel |
| `DELETE /api/hotels/{id}` | ADMIN | Delete hotel |
| `POST /api/hotels/{id}/rooms` | MANAGER/ADMIN | Create room |
| `PUT /api/rooms/{id}` | MANAGER/ADMIN | Update room |
| `DELETE /api/rooms/{id}` | MANAGER/ADMIN | Delete room |
| `PATCH /api/rooms/{id}/status?isAvailable=` | MANAGER/ADMIN | Toggle room |
| `GET /api/manager/hotels` | MANAGER/ADMIN | Hotels I manage |
| `GET /api/manager/hotels/{id}/rooms` | MANAGER/ADMIN | All rooms (inventory view) |
| `GET /api/manager/bookings` | MANAGER/ADMIN | Upcoming bookings |

---

## 10. Getting Started
```bash
# React
npm create vite@latest stayease-ui -- --template react-ts
cd stayease-ui
npm install
npm install react-router-dom axios
npm run dev

# Angular alternative
ng new stayease-ui --routing --style=scss
```

Configure the API base URL (e.g., `VITE_API_BASE_URL=http://localhost:8080`) and ensure CORS is enabled on the backend (already configured for `localhost:3000` and `localhost:5173`).

---

## 11. Testing (light)
- Optional unit tests for 1–2 key components (e.g., Booking form / date validation).
- No automated e2e required at Foundation level.

---

**Author**: Dinesh Sahu · **Email**: dinesh.sahu@apexon.com · **ID**: 206755  
**Team**: Team 45 · **Date**: August 25, 2026
