# DECISIONS — JobStack.ma



## 2026-07-04 — Stack decision
- Backend: Java 25 (LTS, Sept 2025) + Spring Boot (latest stable)
- Frontend: Angular 22 (latest stable, June 2026)
- Deploy: Docker / Docker Compose only for now; Kubernetes deferred until a concrete scaling need arises (YAGNI)
- Video recording: deferred to final sprint only (per user instruction, overrides default "every version" cadence)

## 2026-07-04 — Backend service decisions
- Persistence/Auth: PostgreSQL (own container) + Spring Security with JWT. No Supabase dependency.
- Payments: CMI (Moroccan gateway) only for MVP. Stripe deferred (YAGNI) until international employer demand appears.
- Email: SMTP via Spring JavaMailSender (not Resend) — conventional Java-ecosystem choice.
- Analytics: PostHog retained (plain REST API, no JS lock-in).

## 2026-07-04 — Architecture ADRs (see docs/architecture-jobstack.md)
ADR-1 layered/package-by-feature, ADR-2 Spring Boot on Java 25 LTS, ADR-3 Angular 22 static SPA behind shared nginx, ADR-4 Spring Security+JWT, ADR-5 Flyway migrations.

## 2026-07-04 — Env vars collected
Identified during foundation phase: APP_BASE_URL, DB_NAME/USER/PASSWORD, JWT_SECRET, ADMIN_SEED_EMAIL/PASSWORD, CMI_MERCHANT_ID/STORE_KEY/API_URL/CALLBACK_URL, SMTP_HOST/PORT/USERNAME/PASSWORD/FROM_ADDRESS, POSTHOG_API_KEY/HOST. Written to .env.example with placeholders per user choice (real secrets to be filled in a local, gitignored .env whenever CMI/SMTP accounts are ready).

## DECISION — 2026-07-06 — Sprint 3 approach: backend-first, one story at a time
Chosen: implement Story 3.1 (profile CRUD) fully with tests, then 3.2 (CV upload) fully with tests, then 3.3 (frontend profile screen wiring both) — checkpointing after each, matching how Sprint 2 was executed.
CV storage confirmed already decided in docs/architecture-jobstack.md / docs/devops-jobstack.md: dedicated Docker volume ("CV volume") mounted into the backend container, not S3/external object storage. No new decision needed for 3.2.
