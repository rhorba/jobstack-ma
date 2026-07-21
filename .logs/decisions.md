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

## DECISION — 2026-07-15 — Sprint 4 approach + Story 4.3 search scope
Chosen: same backend-first, one-story-at-a-time approach as Sprints 2-3 (4.1 -> 4.2 -> 4.3 -> 4.4, checkpointing after each).
Story 4.3 (public job search): exact-match filters only (sector, city, contract_type) using the existing idx_job_postings_search index — no free-text search this sprint. Rejected alternatives: ILIKE title search and Postgres full-text search (tsvector/GIN), both premature at MVP scale (YAGNI). Revisit if users ask for free-text search once there's real posting volume.

## DECISION — 2026-07-15 — Sprint 5 approach: mock payment gateway, no real CMI integration yet
User does not have CMI's official merchant integration docs (API reference, hash/signature algorithm, sandbox credentials) available this session. Getting a payment callback signature scheme wrong is exactly the risk docs/security-jobstack.md flags (spoofed callback -> free job posting), so guessing at CMI's real crypto/API shape was rejected as unsafe for a Maximum-rigor payment flow.
Chosen: build the full payment state machine (INITIATED/CONFIRMED/FAILED, job posting DRAFT->PENDING_PAYMENT->LIVE transition, 30-day expiry) behind a PaymentGateway interface with a MockPaymentGateway implementation. The mock reuses the already-collected CMI_STORE_KEY env var as an HMAC-SHA256 shared secret over a placeholder payload shape (transactionId|outcome|amount) — clearly documented as NOT CMI's real algorithm. POST /api/v1/payments/cmi/callback verifies signatures and drives the state machine for real (idempotency, invalid-signature rejection, amount-tampering rejection all built and adversarially tested this sprint, per test-strategy's Maximum-rigor checklist). A dev-only POST /api/v1/payments/{id}/mock-outcome endpoint lets the frontend's mock checkout page simulate a CMI redirect-back by routing through the exact same callback-handling code path (one real implementation, not a parallel test-only code path).
When real CMI credentials/docs become available, MockPaymentGateway gets replaced by a CmiPaymentGateway implementing the same interface; the mock-outcome endpoint gets deleted (not feature-flagged off — YAGNI, per CLAUDE.md's "no feature flags when you can just change the code").

## 2026-07-16 — BRAINSTORM: Sprint 6 approach
Decision: single straightforward approach (no simple/balanced/comprehensive split) — follow the existing REST + Angular pattern from Epics 3-5 (entity/repo/controller/DTOs, IDOR-checked ownership, reactive Angular components). User explicitly agreed to skip a full BRAINSTORM given how clear-cut the acceptance criteria are.

## 2026-07-17 — Sprint 7 kickoff decisions
- CMI real integration still deferred: user has no merchant sandbox credentials yet, MockPaymentGateway stays in place. Revisit each sprint per standing note.
- Story 7.4 (transactional email) will use generic SMTP via Gmail (smtp.gmail.com:587, account rrhorba.mohamed@gmail.com) rather than a provider API (SendGrid/Mailgun) or local MailHog catcher. SMTP_PASSWORD is a placeholder in .env.example — user needs to generate a Gmail App Password (2-Step Verification required) before live email sending can be verified end-to-end this sprint. Until then, email sending will be implemented and unit-tested but live-verification of actual delivery is blocked on that credential.

## 2026-07-17 — Sprint 7 approach (BRAINSTORM)
- Story 7.1 moderation model: user chose the balanced option — add REJECTED/REMOVED to the job posting status enum (with reason) AND a new `moderation_actions` audit log table (who/when/reason) recording every admin moderation action, not just a status flip. More overhead than status-only but gives a full audit trail.
- Story 7.4 email delivery: user chose async via Spring `@Async` — email sending offloaded to a thread pool (executor config + async-context exception handling) so SMTP latency/outages never block register/apply/reject/expiry-soon request threads. Failures still logged per AC, not silently swallowed.

## 2026-07-20 — Sprint 8 approach
- Story 8.1 (PostHog): simple direct-HTTP approach chosen over the posthog-java SDK — small @Async AnalyticsService posting to PostHog's /capture/ REST endpoint, no new Maven dependency, no-ops cleanly when POSTHOG_API_KEY is blank (user has no PostHog account yet; placeholder env vars only this sprint).
- Story 8.2 (adversarial): work the checklist in test-strategy-jobstack.md §4 directly — Auth/Payment (Maximum) first, then CV Upload/Job Search (High).
- Story 8.3 (coverage gate): neither backend (Maven) nor frontend (Angular/Karma) has coverage tooling wired up yet — JaCoCo + Karma coverage both need to be added from scratch this sprint, then tests added to reach 80% combined.

## 2026-07-21 — Sprint 9 (Story 9.1) TLS approach
Chose: self-signed cert generated for the prod nginx image + HSTS + HTTP->HTTPS redirect, fully configured and testable now; README documents swapping in a real CA/Let's Encrypt cert once a real domain/hosting exists. Rejected Caddy+ACME (no real domain yet to issue against) and "assume upstream LB terminates TLS" (doesn't satisfy 9.1's literal acceptance criteria within this repo). No new env vars needed.
