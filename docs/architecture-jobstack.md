# Architecture: JobStack.ma
**PRD Reference**: docs/prd-jobstack.md
**System Design Reference**: docs/system-design-jobstack.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: Software Architect / Tech Lead

**HANDOFF: System Designer → Software Architect**
Context: Topology approved — single Docker host, Spring Boot monolith, Postgres, no queue/cache.
Need: Internal module structure, API design, and tech-stack ADRs for the Java/Angular implementation.

## 1. Overview
A layered, package-by-feature Spring Boot monolith (Java 25 LTS) serving a REST API to an Angular 22 SPA. CRUD-dominant domain with no complex business rules — Simple Layered architecture, not DDD/CQRS (YAGNI).

## 2. Architecture Decision Records

### ADR-1: Layered architecture, package-by-feature
- **Status**: Accepted
- **Context**: Domain is CRUD-dominant (profiles, postings, applications) with no diverging read/write models.
- **Decision**: Standard Spring layering (Controller → Service → Repository) inside feature packages (`candidate/`, `employer/`, `job/`, `application/`, `payment/`, `admin/`, `shared/`).
- **Consequences**: + simple, fast to build, easy onboarding. − will need refactoring if a domain later needs true isolation (e.g., payment as a separate service).
- **Re-evaluate when**: a feature module needs an independent deploy cadence or its own scaling profile.

### ADR-2: Spring Boot 3.x on Java 25 LTS
- **Status**: Accepted
- **Context**: Need long support window and modern language features (virtual threads, records, pattern matching).
- **Decision**: Spring Boot (latest stable 3.x compatible with Java 25), Maven build.
- **Alternatives**: Java 21 LTS — rejected, no reason to target an older LTS on a new project.
- **Re-evaluate when**: a dependency lags Java 25 support (fallback to 21 LTS if blocking).

### ADR-3: Angular 22 SPA, served as static assets
- **Status**: Accepted
- **Context**: Need a modern, typed SPA framework matching the user's stack request.
- **Decision**: Angular 22 (standalone components, signals), built and served as static files behind the same nginx reverse proxy as the API (single Docker Compose stack — no separate frontend host).
- **Consequences**: + one deployment unit, simple CORS-free setup (same origin behind nginx). − Angular and API share a release train for MVP.
- **Re-evaluate when**: frontend and backend need independent release cadences.

### ADR-4: Spring Security + JWT for auth
- **Status**: Accepted
- **Context**: Confirmed in UNDERSTAND phase — no Supabase/Auth.js dependency.
- **Decision**: Spring Security with stateless JWT (access token short-lived, refresh token rotation). Full detail owned by Security Engineer doc.
- **Alternatives**: Session-based auth — rejected, stateless JWT fits a single-instance monolith equally well and avoids sticky-session concerns if scaled later.
- **Re-evaluate when**: multi-instance deployment needs shared session state (JWT already avoids this problem).

### ADR-5: Flyway for schema migrations
- **Status**: Accepted
- **Context**: Need versioned, reviewable schema changes from Sprint 1 onward.
- **Decision**: Flyway, SQL migrations under `src/main/resources/db/migration`.
- **Alternatives**: Liquibase (XML/YAML) — rejected, plain SQL is more direct for a small team.

## 3. System Design
```
[Angular 22 SPA (static build)] ─┐
                                  ├─→ [nginx reverse proxy, TLS] ─→ [Spring Boot API]
[Browser fetch/XHR to /api/*] ────┘                                      │
                                                             ┌────────────┼────────────┐
                                                        [PostgreSQL] [CV volume]  [CMI / SMTP / PostHog]
```

## 4. Data Model (high-level — full schema owned by DBA)
```
Candidate ──1:1──> CandidateProfile ──1:N──> Application
Employer  ──1:N──> Company
Company   ──1:N──> JobPosting ──1:N──> Application
JobPosting ──1:1──> Payment
Admin (role on User, not a separate table)
User ──1:1──> {Candidate | Employer | Admin} (role-based subtype via a `role` column)
```

## 5. API Design
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | /api/v1/auth/register | Register candidate or employer | Public |
| POST | /api/v1/auth/login | Login, issue JWT | Public |
| POST | /api/v1/auth/refresh | Rotate refresh token | Refresh token |
| GET/PUT | /api/v1/candidates/me | View/edit own profile | Candidate |
| POST | /api/v1/candidates/me/cv | Upload CV | Candidate |
| GET | /api/v1/jobs | Search/filter job postings | Public |
| GET | /api/v1/jobs/:id | Get one posting | Public |
| POST | /api/v1/jobs | Create posting (draft, unpaid) | Employer |
| POST | /api/v1/jobs/:id/checkout | Start CMI payment for posting | Employer (owner) |
| POST | /api/v1/payments/cmi/callback | CMI payment confirmation webhook | CMI signature |
| POST | /api/v1/jobs/:id/apply | Candidate applies | Candidate |
| GET | /api/v1/employers/me/jobs | List own postings | Employer |
| GET | /api/v1/employers/me/jobs/:id/applicants | List applicants for a posting | Employer (owner) |
| GET | /api/v1/admin/jobs | List postings for moderation | Admin |
| PUT | /api/v1/admin/jobs/:id/status | Approve/reject/remove posting | Admin |
| PUT | /api/v1/admin/users/:id/status | Suspend/reinstate account | Admin |
| GET | /api/v1/admin/metrics | Platform metrics | Admin |

## 6. Security Considerations
(Full detail: docs/security-jobstack.md)
- Authentication: Spring Security, stateless JWT (access + refresh)
- Authorization: Role-based (`CANDIDATE`, `EMPLOYER`, `ADMIN`) + ownership checks on employer/candidate resources
- Data protection: CV files and PII fields encrypted at rest; CMI payment flow never touches raw card data (hosted/redirect)
- Key risks: payment callback spoofing, unauthorized cross-employer data access — mitigated via CMI signature verification and ownership checks on every employer/candidate-scoped endpoint

## 7. Infrastructure
- Hosting: single VPS/Docker host (per system design)
- Database: PostgreSQL, own container, volume-backed, nightly `pg_dump` backup
- CI/CD: GitHub Actions (lint → test → security scan → build Docker images → deploy)
- Monitoring: container stdout logs to a volume; revisit if a dedicated tool becomes necessary

## 8. Technical Risks
| Risk | Mitigation | Owner |
|---|---|---|
| Java 25 + Spring Boot library gaps at this LTS | Verify key deps (Spring Data, Security, Flyway) support Java 25 before Sprint 1; fallback to Java 21 LTS if blocked | Tech Lead |
| CMI callback security (spoofed payment confirmations) | Verify CMI signature/HMAC on every callback; reconcile against CMI merchant dashboard | Security Engineer |
| Shared Docker Compose release train (frontend+backend coupled) | Acceptable at MVP size; revisit if release cadences diverge | Tech Lead |
