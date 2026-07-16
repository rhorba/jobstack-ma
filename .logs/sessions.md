# SESSIONS — JobStack.ma



## SESSION_START — 2026-07-04
Resuming JobStack.ma. Prior state: README-only (Next.js/Supabase stack, no code). User pivoted stack to Java (Spring Boot, Java 25 LTS) + Angular (v22, latest stable). Starting fresh per CLAUDE.md new-project rules: full 6-phase flow, foundation docs before code, full sprint backlog upfront.

## SESSION_END — 2026-07-04
Completed: full document-first foundation for JobStack.ma pivot to Java 25 LTS (Spring Boot) + Angular 22, Docker-only deploy. All 10 docs drafted and approved (PRD, system design, architecture, security, database, UX, UI, test strategy, DevOps, 9-sprint stories backlog). Git repo initialized, remote set to github.com/rhorba/jobstack-ma, .env.example written (placeholder values), commit b46c6c9 pushed to origin/master.
Next: Session 2 starts Sprint 1 (Epic 1 — project foundation: backend scaffold, frontend scaffold, Flyway V1 migration, Docker Compose dev stack, CI pipeline skeleton). No code written yet.

## SESSION_END — 2026-07-05
Completed this session:
- Foundation phase (prior session): all 10 docs (PRD, system design, architecture, security, database, UX, UI, test strategy, DevOps, stories) drafted and approved; git initialized with remote github.com/rhorba/jobstack-ma; .env.example written; foundation commit pushed.
- Sprint 1 (Epic 1: Project Foundation) fully executed and shipped:
  - Story 1.1: Spring Boot 3.5.16 (Java 25 LTS) backend scaffold, package-by-feature structure
  - Story 1.2: Angular 22 + Material frontend scaffold, feature-folder structure
  - Story 1.3: Flyway V1 migration (6 tables + indexes), verified against Testcontainers Postgres
  - Story 1.4: Docker Compose dev stack (nginx+api+db), verified end-to-end on host port 8090
  - Story 1.5: GitHub Actions CI — went red 5 times, each diagnosed and fixed (mvnw exec bit, YAML quoting, Trivy rate-limit restructure, Spring Boot 3.5.0->3.5.16 CVE fixes, Alpine base image switch). Final run 28746846936 fully green.
- Local toolchain installed this session: Eclipse Temurin JDK 25, Node.js bumped 22.22.0->22.23.1 (both via winget).
- Local environment note: this machine already runs two other unrelated projects in Docker (darkom-ma, atlas-events) occupying ports 8080/8081/5432/4200/4201/5672/15672 — jobstack-ma's nginx uses 8090 to avoid collision.

Next session: Start Sprint 2 (Epic 2: Authentication & Accounts) — stories 2.1-2.5 (register, login+JWT, refresh rotation, role guards, admin seed). No auth code written yet. Sprint backlog: docs/stories-jobstack.md. Remember Story 2.5 (admin seed) needs ADMIN_SEED_EMAIL/PASSWORD env vars already defined in .env.example.

## SESSION_START — 2026-07-06
Resuming JobStack.ma. Sprint 1 (foundation) complete and pushed (commit 4029e8e). Starting Sprint 2 (Epic 2: Authentication & Accounts) — stories 2.1-2.5.

## SESSION_END — 2026-07-06
Completed this session: Sprint 2 (Epic 2: Authentication) stories 2.1 (register), 2.2 (login+JWT), 2.3 (refresh rotation+logout), 2.5 (admin seed) fully done and tested (12 backend tests green). Story 2.4 (role guards) backend done+tested; frontend guards/auth service/login-register forms/dashboard stubs built and unit-tested (5 frontend tests green) but NOT yet verified end-to-end against the live stack (no docker-compose run this session for auth). Pushed commit f60ba70.
NOT done: Sprint 2 VERIFY+SHIP (task #22) — no coverage report run, no CI watch-to-green (pushed but not monitored per user's explicit "end session now" request), no sprint metrics snapshot logged.
Next session, in order:
1. Check CI status on commit f60ba70 (gh run list/view) — diagnose and fix if red, this was never confirmed green.
2. Optionally do a quick end-to-end manual check of Story 2.4 (docker-compose up, register+login as candidate, confirm dashboard loads, confirm /employer redirects away).
3. Run coverage report, decide if it matters yet (test-strategy doc says the 80% gate is enforced starting Story 8.3, not now — just eyeball it).
4. Log Sprint 2 SPRINT_SNAPSHOT to .logs/metrics.md and MILESTONE to activity.md, mark task #22 complete.
5. Start Sprint 3 (Epic 3: Candidate Profile & CV) — stories 3.1-3.3.

## SESSION_START — 2026-07-06 (cont.)
Resuming JobStack.ma. Last session ended after Sprint 2 EXECUTE but before VERIFY+SHIP (task #22 open). Picking up: check CI on f60ba70, optionally verify Story 2.4 e2e, run coverage, log Sprint 2 snapshot, then start Sprint 3 (Epic 3: Candidate Profile & CV).

## SESSION_END — 2026-07-06 (cont.)
Completed this session:
- Sprint 2 VERIFY+SHIP fully closed out: CI confirmed green on f60ba70, Story 2.4 role guards verified end-to-end against the live stack (backend authorization layer — register/login/role-scoped access all correct), backend 12/12 + frontend 5/5 green, Sprint 2 snapshot logged to metrics.md, minor 401-vs-403 convention issue logged as backlog (not blocking).
- Sprint 3 (Epic 3: Candidate Profile & CV) started:
  - Story 3.1 (profile view/edit): Flyway V3 (full_name nullable), CandidateProfile entity+repo, auto-created on candidate registration, GET/PUT /api/v1/candidates/me. Backend tests 5/5 green.
  - Story 3.2 (CV upload): Dockerfile+compose CV volume (/data/cvs), POST/GET /api/v1/candidates/me/cv with magic-byte PDF validation, 5MB limit, server-derived filename (no path traversal), ownership-scoped via "me". Backend tests 6/6 green.
  - Full backend suite: 23/23 green (verified via MockMvc + Testcontainers).
  - Fixed a self-introduced bug along the way: a test-scoped application.properties shadowed the main config and broke all tests (logged in issues.md), root-caused and fixed by scoping the override via @TestPropertySource instead.
- NOT done: Story 3.3 (frontend profile screen) not started. Sprint 3 not yet verified/shipped.
- Local .env created for dev docker-compose runs (gitignored, not committed) with placeholder dev values.
OPEN ISSUE (must resolve next session before Sprint 3 ships): live docker-stack smoke test of GET /api/v1/candidates/me returns 403 for a pre-existing test user, not reproduced by the automated test suite. Full detail and investigation plan in issues.md (2026-07-06 entry). Leading theory: that user (candidate1@test.ma) was registered before Story 3.1's auto-profile-creation existed, so has no candidate_profiles row, and the resulting 404 may be getting intercepted/translated to 403 somewhere in the security filter chain — needs `docker compose logs api` to confirm the real exception.
All work is uncommitted (git status: several modified + new files, see below) — nothing pushed this session since Sprint 3 isn't complete. Docker stack stopped cleanly (`docker compose down`, volumes preserved).
Next session, in order:
1. Investigate and fix the 403 issue above (check api container logs, retest with a fresh candidate registered under current code).
2. Finish Story 3.2 verification once the above is resolved.
3. Story 3.3 (frontend candidate profile screen + CV upload UI + incomplete-profile prompt).
4. Sprint 3 VERIFY+SHIP: run both test suites, log snapshot, commit, push.

## SESSION_START — 2026-07-06 (cont. 2)
Resuming JobStack.ma. Picking up open issue: 403 on live-stack candidate profile endpoint.

## Progress this session
Root-caused and fixed the open 403 issue (see issues.md and activity.md FIX entry) — it was a systemic Spring Security + servlet error-dispatch bug (`/error` re-entering the security filter chain on `DispatcherType.ERROR`), not specific to the candidate profile endpoint. Fixed via one-line `SecurityConfig` change (`permitAll` on `/error`). Verified end-to-end against the live docker stack through nginx: register/login error paths return correct codes, candidate profile GET/PUT/CV-upload/CV-download all confirmed working, cross-role denial still correct. Backend suite 23/23 green after the fix. Stack torn down cleanly.
Story 3.2 (CV upload) is now fully verified — this was the last open blocker from last session.
Next, in order: Story 3.3 (frontend candidate profile screen + CV upload UI + incomplete-profile prompt), then Sprint 3 VERIFY+SHIP (run both test suites, log snapshot, commit, push).

## SESSION_END — 2026-07-06 (cont. 2)
Completed this session:
- Root-caused and fixed the open 403 bug from last session (see issues.md/activity.md FIX entry): any `ResponseStatusException` was surfacing as an empty 403 in the live stack due to `/error` re-entering the Spring Security filter chain on `DispatcherType.ERROR`. One-line fix in `SecurityConfig` (`permitAll` on `/error`). Verified end-to-end against the rebuilt live stack.
- Story 3.2 (CV upload) now fully verified end-to-end (was the last blocker carried in from last session).
- Story 3.3 (candidate profile screen UI) built and shipped-in-code: backend `hasCv` field added to `CandidateProfileResponse`; frontend `candidate-home.component.ts/html` rebuilt as the real profile screen (form + CV upload + incomplete-profile banner per UX Flow 1); `candidate-profile.model.ts` added.
- Tests: backend 23/23 green, frontend 11/11 green (3 test files).
- Live UI verification done via Playwright (Chrome extension was unavailable this session) — installed `playwright-core` standalone in the session scratchpad only, NOT added to frontend package.json/project dependencies. Drove the full flow against the live docker-compose stack: incomplete banner on fresh registration, form save, CV upload, banner clearing, reload persistence — all confirmed with screenshots.
- Sprint 3 (Epic 3: Candidate Profile & CV) is feature-complete. Sprint 3 SPRINT_SNAPSHOT logged to metrics.md.
- Docker stack brought up/down cleanly multiple times during investigation and verification; always torn down after use.
NOT done / explicitly deferred: commit and push. Per CLAUDE.md rule 7, a sprint-end push is expected, but per the standing "never commit without explicit user ask" rule I asked the user for go-ahead; the user ended the session before answering, so **all of this session's work (Sprint 3 code + the SecurityConfig fix) is uncommitted** — see git status below. Do not lose this.
Uncommitted changes at session end:
- Modified: .logs/*, backend/Dockerfile, backend/src/main/java/ma/jobstack/auth/AuthService.java, backend/src/main/java/ma/jobstack/auth/SecurityConfig.java (the /error fix), backend/src/main/java/ma/jobstack/candidate/CandidateController.java, backend/src/main/resources/application.properties, docker-compose.yml, frontend/src/app/features/candidate/candidate-home.component.ts
- New/untracked: backend candidate profile+CV classes and dto/ (CandidateProfile, CandidateProfileRepository, CvStorageService, CvUploadExceptionHandler), backend/src/main/resources/db/migration/V3__candidate_profile_full_name_nullable.sql, backend/src/test/java/ma/jobstack/candidate/ (CandidateProfileTests, CvUploadTests), frontend candidate-home.component.html/.spec.ts, candidate-profile.model.ts
Next session, in order:
1. Ask user whether to commit + push Sprint 3 now (this was the exact point the session ended on).
2. If yes: stage, commit (Story 3.1-3.3 + the /error security fix), push to origin/master, then watch CI to green per rule 11 (fix-and-repush if red, do not leave this session without confirming green — last time this was skipped it required an explicit next-session check).
3. Log the push to activity.md and update this file's snapshot with the CI run result.
4. Then start Sprint 4 (Epic 4: Job Posting & Search) — stories 4.1+.

## SESSION_END — 2026-07-15
Completed this session:
- Sprint 3 (Epic 3: Candidate Profile & CV), carried over uncommitted from last session: committed (b842750), pushed, CI confirmed green (run 29391919227). Fully shipped.
- Sprint 4 (Epic 4: Job Posting & Search) fully executed and shipped end-to-end in this session:
  - Story 4.1: employer company registration (POST/GET /api/v1/employers/me/company, one company per employer, 409 on duplicate).
  - Story 4.2: create job posting draft (POST /api/v1/jobs, requires an owned company, creates DRAFT). SecurityConfig split so GET /api/v1/jobs/** stays public while POST requires EMPLOYER.
  - Story 4.3: public job search/filter (GET /api/v1/jobs, GET /api/v1/jobs/:id), exact-match sector/city/contractType filters (user-confirmed scope choice, no free-text search this sprint), LIVE-only visibility, parameterized queries (adversarial SQL-injection test included).
  - Story 4.4: Angular job-search + job-detail screens, 4-state Apply CTA (guest/wrong-role/missing-cv/ready). App root redirect changed from /login to /jobs per the approved UX IA (Home = job search).
  - Backend 39/39 tests green, frontend 19/19 tests green.
  - Live-verified end-to-end via Chrome against the rebuilt docker stack: employer company + draft job creation through the real API, flipped a posting to LIVE directly in Postgres (payment flow that would normally do this is Sprint 5, not built yet), then confirmed in-browser: search screen lists it correctly, detail page renders all 4 Apply-CTA states correctly (including logging in as a candidate, completing the profile, and uploading a CV live), DRAFT postings confirmed NOT publicly visible. Chrome's file_upload tool couldn't reach the local scratchpad file in this sandboxed session, so the CV-upload step of live verification used the real API instead of the UI file input; the UI file-input path itself was already verified live in Sprint 3 and is unchanged.
  - Committed (d20afda), pushed, CI confirmed green (run 29406704110). Sprint 4 snapshot logged to metrics.md.
- Coverage tooling still not wired up — by design, Story 8.3 (Sprint 8) introduces the gate, consistent with all prior sprints.
Next session: Start Sprint 5 (Epic 5: Payment/CMI) — stories 5.1-5.3. This is flagged Maximum rigor per test-strategy (payment + security). Will need CMI merchant credentials (CMI_MERCHANT_ID/STORE_KEY/API_URL/CALLBACK_URL) from the user before EXECUTE — these are placeholder-only in .env.example currently; real sandbox/test credentials should be collected at the start of that session per CLAUDE.md rule 10.

## SESSION_END — 2026-07-15 (cont.)
This was a continuation of the same 2026-07-15 session, past the SESSION_END entry above — the user asked to keep going into Sprint 5 instead of stopping.
Completed this continuation:
- Sprint 5 (Epic 5: Payment/CMI) fully executed and shipped end-to-end.
- Key decision: user had no CMI merchant integration docs/sandbox credentials available. Rather than guess at CMI's real signature/API scheme for a Maximum-rigor payment flow, built the full payment state machine behind a `PaymentGateway` interface with a `MockPaymentGateway` implementation (HMAC-SHA256 over the already-collected `CMI_STORE_KEY`, clearly documented as a placeholder — see decisions.md, 2026-07-15 "Sprint 5 approach"). **This is the most important thing for next session to know**: real CMI integration is NOT done. `MockPaymentGateway` and the dev-only `POST /api/v1/payments/{id}/mock-outcome` endpoint must be replaced/removed once real CMI docs and sandbox credentials are available.
- Story 5.1: Payment entity/repo, `POST /api/v1/jobs/:id/checkout` (employer owns posting, DRAFT-only, 409 if a payment already exists per the DB's `UNIQUE(job_posting_id)`).
- Story 5.2 (Maximum rigor): `POST /api/v1/payments/cmi/callback` — signature verified before any state change, idempotent duplicate-callback handling, amount-tampering rejection, SUCCESS activates the job posting to LIVE (+30d expiry).
- Story 5.3: employer "Post a Job" flow rebuilt in `employer-home.component` (company registration if missing -> job draft -> checkout -> mock CMI page -> success/failed+retry).
- Backend 51/51 tests green (12 new), frontend 26/26 tests green (7 new).
- Live-verified end-to-end via Chrome: full employer flow including a deliberate FAILED outcome + Retry, then SUCCESS, then confirmed the posting appears in public search. Had to log out of a leftover CANDIDATE session cookie from Sprint 4 testing first.
- Committed (2e473a3), pushed, CI confirmed green (run 29440497158). Sprint 5 snapshot logged to metrics.md.
- Known accepted gap (not a blocker, logged in activity.md): a true concurrent double-checkout race isn't unit-tested; the DB's UNIQUE(job_posting_id) constraint backs the safety property structurally regardless (a race would surface as a 500, not a clean 409).
- Coverage tooling still not wired up — by design, Story 8.3 (Sprint 8), consistent with all prior sprints.
Next session, in order:
1. If real CMI merchant docs/sandbox credentials have become available, that's the natural point to replace MockPaymentGateway with a real CmiPaymentGateway (and delete the mock-outcome endpoint) before going further — ask the user.
2. Otherwise, start Sprint 6 (Epic 6: Application Flow) — stories 6.1 (one-click apply, depends on 3.2 CV + 4.3 search, both done) and 6.2 (employer applicant dashboard). No new env vars expected.
3. Local dev docker volumes still hold accumulated test data from Sprints 4-5 live verification (multiple LIVE "Automotive QA Engineer" postings etc.) — harmless for dev, but be aware when eyeballing search results live.

## SESSION_START — 2026-07-16
Resuming JobStack.ma. Last session ended with Sprint 5 (Epic 5: Payment/CMI, mock gateway) shipped and CI-green (commit 2e473a3, run 29440497158). Asked user: swap in real CMI now, or start Sprint 6? User chose Sprint 6 (Epic 6: Application Flow).
