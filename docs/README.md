# 📚 StayEase Documentation

**Welcome to the StayEase Hotel Booking Service documentation!**

---

## 🗂️ Documents

| # | File | Purpose | When to Read |
|---|------|---------|-------------|
| 1 | **[High_Level_Design.md](./High_Level_Design.md)** | High-level overview: tech stack, data model, design patterns, API outline, auth strategy, request flows | Start here for big picture |
| 2 | **[Backend_Components.md](./Backend_Components.md)** | Backend blueprint: package structure, entities, component breakdown, availability logic, booking flow, security config, testing | When building backend features |
| 3 | **[API_Guide.md](./API_Guide.md)** | Complete API documentation: all 21 endpoints, request/response examples, error handling, validation rules | When calling or testing APIs |
| 4 | **[Architecture.md](./Architecture.md)** | Visual architecture diagrams: system flow, component relationships, security flow, data flow | When you need visual understanding |
| 5 | **[Frontend_Components.md](./Frontend_Components.md)** | Frontend UI blueprint: React project structure, routing, pages, components, TypeScript types | When building the frontend |
| 6 | **[Deployment.md](./Deployment.md)** | Free hosting guide (Render): backend + frontend deployment, env vars, CORS, troubleshooting | When deploying for the team to use |

---

## 🎯 Quick Navigation

| I want to... | Go to |
|--------------|-------|
| Understand the system overview | [High_Level_Design.md](./High_Level_Design.md) |
| See the backend blueprint & components | [Backend_Components.md](./Backend_Components.md) |
| See visual architecture diagrams | [Architecture.md](./Architecture.md) |
| Know what APIs exist and how to call them | [API_Guide.md](./API_Guide.md) |
| Understand authentication & security | [Backend_Components.md](./Backend_Components.md) § Security |
| See design patterns used | [High_Level_Design.md](./High_Level_Design.md) § Design Patterns |
| Know the data model & entities | [Backend_Components.md](./Backend_Components.md) § Domain Entities |
| Build the frontend UI | [Frontend_Components.md](./Frontend_Components.md) |
| See error response format | [API_Guide.md](./API_Guide.md) § Error Handling |

---

## 📖 Reading Order (New Team Members)

1. **[High_Level_Design.md](./High_Level_Design.md)** — 15 min overview
2. **[Architecture.md](./Architecture.md)** — 10 min diagrams
3. **[API_Guide.md](./API_Guide.md)** — Reference as needed
4. **Swagger UI** — `http://localhost:8080/swagger-ui.html` (interactive)

---

## 🔍 Quick Reference

### URLs
| Purpose | URL |
|---------|-----|
| Local API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health Check | http://localhost:8080/actuator/health |

### Key Endpoints
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| /api/auth/register | POST | Public | Register user |
| /api/auth/login | POST | Public | Login |
| /api/hotels?city= | GET | Public | Search hotels |
| /api/hotels/all | GET | ADMIN | List all hotels |
| /api/hotels/{id} | GET | Public | Hotel details |
| /api/hotels/{id}/rooms | GET | Public | Available rooms |
| /api/rooms/{id} | GET | Public | Room details |
| /api/bookings | POST | GUEST | Create booking |
| /api/hotels | POST | ADMIN | Create hotel |
| /api/hotels/{id}/rooms | POST | MANAGER/ADMIN | Create room |
| /api/manager/hotels | GET | MANAGER/ADMIN | Hotels I manage |
| /api/manager/hotels/{id}/rooms | GET | MANAGER/ADMIN | All rooms (inventory view) |
| /api/manager/bookings | GET | MANAGER/ADMIN | Hotel bookings |

### User Roles
| Role | Permissions |
|------|------------|
| GUEST | Create, view, cancel own bookings |
| MANAGER | Manage rooms, view hotel bookings |
| ADMIN | Manage hotels, all manager permissions |

---

**Author**: Dinesh Sahu · **Email**: dinesh.sahu@apexon.com · **ID**: 206755  
**Team**: Team 45 · **Date**: August 25, 2026
