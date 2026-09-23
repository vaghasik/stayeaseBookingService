# StayEase - Deployment Guide (Free Hosting via Render)

> How to deploy both the backend (this repo) and the companion frontend
> (`...-ui`) for free, so the whole team can access them via a public hostname.

---

## Why Render

[Render](https://render.com) is used because, on its free tier, it can host:

- A free **Web Service** for the Spring Boot backend (builds from the repo's `Dockerfile`)
- A free **PostgreSQL** database
- A free **Static Site** for the React/Vite frontend build output

All three live under one dashboard, each gets its own public HTTPS hostname
(e.g. `stayease-backend.onrender.com`), and Render auto-redeploys on every
`git push` to the connected branch.

---

## Prerequisites

- Both repos (`...-service` and `...-ui`) pushed to GitHub
- A free [Render](https://render.com) account (sign up with GitHub)

---

## 1. Create the database

1. Render Dashboard → **New +** → **PostgreSQL**
2. Name: `stayease-db`, Plan: **Free**
3. Once created, copy the **Internal Database URL** (for the backend service
   running on Render) or the **External Database URL** (if you need to connect
   from your local machine, e.g. with a DB client)

---

## 2. Deploy the backend (this repo)

1. Render Dashboard → **New +** → **Web Service** → connect this repo
2. Render auto-detects the `Dockerfile` at the repo root — no build/start
   command needed, it's a Docker-based service
3. Set the following **Environment Variables**:

   | Variable | Value |
   |----------|-------|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DB_URL` | JDBC URL built from the Postgres connection details, e.g. `jdbc:postgresql://<host>:5432/<database>` |
   | `DB_USERNAME` | from the Postgres instance |
   | `DB_PASSWORD` | from the Postgres instance |
   | `JWT_SECRET` | any long random string (e.g. generate with `openssl rand -base64 32`) |
   | `CORS_ALLOWED_ORIGINS` | the deployed frontend URL, e.g. `https://stayease-team45.onrender.com` |

4. Deploy. Render builds the Docker image (multi-stage Maven build → JRE
   runtime) and starts the container. You'll get a public hostname like:
   ```
   https://stayease-backend.onrender.com
   ```
5. Verify it's up:
   ```bash
   curl https://stayease-backend.onrender.com/actuator/health
   ```

> **Note:** Flyway migrations (`src/main/resources/db/migration`) run
> automatically on startup against the Postgres database — no manual schema
> setup needed for `prod`.

---

## 3. Deploy the frontend (companion `...-ui` repo)

1. Render Dashboard → **New +** → **Static Site** → connect the frontend repo
2. Build Command: `npm install && npm run build`
3. Publish Directory: `dist`
4. Environment Variable:

   | Variable | Value |
   |----------|-------|
   | `VITE_API_URL` | `https://stayease-backend.onrender.com` (the backend URL from step 2) |

5. Deploy → share the resulting URL (e.g. `https://stayease-team45.onrender.com`)
   with your team.

> The frontend's `src/services/api.ts` already reads `VITE_API_URL` at build
> time via `import.meta.env.VITE_API_URL` — no code changes are needed, just
> set the environment variable in Render.

---

## 4. Update CORS after the frontend URL is known

`CorsConfig.java` reads extra allowed origins from the `app.cors.allowed-origins`
property, which is bound to the `CORS_ALLOWED_ORIGINS` environment variable in
`application-prod.yml`. This is **in addition to** the built-in `localhost`
dev ports, so local development is unaffected.

To allow a new frontend origin (or multiple), set a comma-separated list and
redeploy the backend — no code changes required:

```
CORS_ALLOWED_ORIGINS=https://stayease-team45.onrender.com,https://another-preview-url.onrender.com
```

---

## 5. Free tier notes / limitations

- Render's free **Web Service** and **PostgreSQL** instances spin down after
  ~15 minutes of inactivity; the first request after idling will be slow
  (cold start, usually 30-60s) — this is expected on the free tier.
- Render's free PostgreSQL databases expire after 90 days unless upgraded. For
  a longer-lived free database, consider [Neon](https://neon.tech) or
  [Supabase](https://supabase.com) (both have a free-forever tier) and point
  `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` at them instead.
- Each redeploy re-runs Flyway migrations automatically; no manual DB steps
  are needed after the first deploy.

---

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| Frontend shows network/CORS errors after deploy | `CORS_ALLOWED_ORIGINS` not set (or set to the wrong URL) on the backend | Set `CORS_ALLOWED_ORIGINS` to the exact frontend URL (including `https://`, no trailing slash) and redeploy the backend |
| Backend fails to start with a datasource error | `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` missing or malformed | Double-check the values against the Postgres instance's connection details in Render |
| First request after idling is very slow | Render free tier spins down inactive services | Expected behavior on the free tier; the service wakes up on the next request |
| Frontend calls `localhost:8080` in production | `VITE_API_URL` not set at build time | Set the env var in Render's Static Site settings and trigger a rebuild |

---

For local development setup, see the main [README.md](../README.md).
