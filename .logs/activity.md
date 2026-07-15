# ACTIVITY — JobStack.ma



## 2026-07-04 — PRD drafted
docs/prd-jobstack.md written by PM. Awaiting user approval.

## 2026-07-04 — MILESTONE: PRD approved
docs/prd-jobstack.md approved by user, no changes requested.

## 2026-07-04 — System design drafted
docs/system-design-jobstack.md written by System Designer: single Docker host, monolith, no queue/cache/CDN at MVP scale. Awaiting user approval.

## 2026-07-04 — MILESTONE: System design approved
docs/system-design-jobstack.md approved by user, no changes requested.

## 2026-07-04 — Architecture drafted
docs/architecture-jobstack.md written by Software Architect/Tech Lead: layered package-by-feature Spring Boot monolith, Angular 22 static SPA behind nginx, JWT auth, Flyway migrations, full API design table. Awaiting user approval.

## 2026-07-04 — MILESTONE: Architecture approved
docs/architecture-jobstack.md approved by user, no changes requested.

## 2026-07-04 — Security baseline drafted
docs/security-jobstack.md written by Security Engineer: STRIDE top risks, JWT auth (15min access/7d refresh rotation), simple-roles+ownership authz, PII/CV encryption plan. Awaiting user approval.

## 2026-07-04 — MILESTONE: Security baseline approved
docs/security-jobstack.md approved by user, no changes requested.

## 2026-07-04 — Database design drafted
docs/database-jobstack.md written by DBA: 6 tables (users, candidate_profiles, companies, job_postings, payments, applications), indexes for search/dashboard/applicant-list patterns, 2-migration plan, daily pg_dump backup. Awaiting user approval.

## 2026-07-04 — MILESTONE: Database design approved
docs/database-jobstack.md approved by user, no changes requested.

## 2026-07-04 — UX foundation drafted
docs/ux-jobstack.md written by UX Designer: 2 personas, IA sitemap, 3 core flows (apply, post+pay, moderate) with error paths, 2 key wireframes, screen states table. Awaiting user approval.

## 2026-07-04 — MILESTONE: UX foundation approved
docs/ux-jobstack.md approved by user, no changes requested.

## 2026-07-04 — UI foundation drafted
docs/ui-jobstack.md written by UI Designer: Angular Material as component library, minimal token overrides (2 brand colors + semantic), component inventory (2 custom: CV upload, status badge), responsive breakpoints, a11y baseline. Awaiting user approval.

## 2026-07-04 — MILESTONE: UI foundation approved
docs/ui-jobstack.md approved by user, no changes requested.

## 2026-07-04 — Test strategy drafted
docs/test-strategy-jobstack.md written by Test Architect: risk scoring (Payment=Maximum 13, Auth=Maximum 10, Job search/Dashboard=High), ATDD scenarios for apply/pay/moderate, adversarial checklist for auth/payment/CV-upload/IDOR, 80% coverage gate. Awaiting user approval.

## 2026-07-04 — MILESTONE: Test strategy approved
docs/test-strategy-jobstack.md approved by user, no changes requested.

## 2026-07-04 — DevOps foundation drafted
docs/devops-jobstack.md written by DevOps/DevSecOps: GitHub Actions CI (backend/frontend/security/build jobs), Docker Compose (nginx+api+db, no k8s), Semgrep/Trivy/Gitleaks gates, health-check monitoring only (Prometheus/Grafana deferred). Awaiting user approval.

## 2026-07-04 — MILESTONE: DevOps foundation approved
docs/devops-jobstack.md approved by user, no changes requested.

## 2026-07-04 — Full sprint backlog drafted
docs/stories-jobstack.md written by Scrum Master + Test Architect: 9 epics, ~28 stories with ATDD acceptance criteria, allocated across 9 sprints (foundation -> auth -> profile/CV -> jobs/search -> CMI payment -> apply/dashboard -> admin/email -> analytics/hardening -> production release+video). Awaiting user approval.

## 2026-07-04 — MILESTONE: Stories/sprint backlog approved
docs/stories-jobstack.md approved by user, no changes requested. All 10 foundation docs now approved.

## 2026-07-04 — PUSH: foundation docs
Pushed commit b46c6c9 to origin/master (github.com/rhorba/jobstack-ma). 76 files: 10 foundation docs, .env.example, .gitignore, framework/skills config. First-session foundation phase complete per CLAUDE.md rule 13.

## 2026-07-04 — EXECUTE: Sprint 1 started
Beginning Sprint 1 (Epic 1: Project Foundation & Infrastructure) — stories 1.1-1.5. Toolchain check: Java 21 LTS was the only JDK installed locally (target is Java 25 LTS per ADR-2); installed Eclipse Temurin JDK 25 via winget (session-scoped JAVA_HOME/PATH override used, no global PATH edit). Global Angular CLI was v17.3.17 (outdated); using npx @angular/cli@22 per-project instead of upgrading globally. Node 22.22.0, npm 10.2.3, Docker 27.0.3 already present and compatible.

## 2026-07-04 — Story 1.1 done: Backend scaffold
Spring Boot 3.5.0 project generated (Java 25 LTS, package-by-feature: candidate/employer/job/application/payment/admin/shared). Verified end-to-end against a temp Postgres 16 container: app boots, GET /actuator/health returns 200 (validated on an alternate port after discovering 8080 was occupied by leftover local processes, since cleaned up). application.properties wired for DB_HOST/PORT/NAME/USER/PASSWORD and SERVER_PORT env overrides.

## 2026-07-04 — Story 1.2 done: Frontend scaffold
Angular 22 standalone-components project generated (frontend/), Angular Material 22 added, feature-folder skeleton created (features/candidate,employer,job,admin + core + shared). Verified: ng build succeeds, ng serve returns HTTP 200 with Material fonts/theme wired into the shell. Toolbar/title component added as the placeholder home page.
Toolchain note: had to upgrade Node 22.22.0 -> 22.23.1 via winget (Angular CLI 22 requires >=22.22.3).

## 2026-07-04 — Story 1.3 done: Flyway V1 migration
db/migration/V1__initial_schema.sql added (6 tables + all planned indexes from docs/database-jobstack.md). Verified against a temp Postgres 16 container: Flyway applies cleanly to v1, all 6 tables + 18 indexes/constraints confirmed via psql, health endpoint stayed green.

## 2026-07-04 — Story 1.4 done: Docker Compose dev stack
backend/Dockerfile (multi-stage, eclipse-temurin 25 jdk->jre, non-root, curl-based healthcheck), frontend/Dockerfile (node:22-slim build -> nginx:1.27-alpine), frontend/nginx.conf (SPA fallback + /api/ proxy to api:8080), root docker-compose.yml (nginx+api+db). Verified: docker compose build succeeded, `docker compose up -d` brought all 3 containers healthy, frontend reachable on localhost:8090 (HTTP 200), api reachable+healthy from nginx container over the internal network. Stack torn down after verification (docker compose down).
Note: host ports 8080/8081/5432/4200/4201/5672/15672 are occupied by other local projects (darkom-ma, atlas-events) already running in Docker — chose 8090 for this project's nginx to avoid collision; no host ports published for api/db per architecture (internal network only).

## 2026-07-05 — Story 1.5 done: CI pipeline green (after fixes)
.github/workflows/ci.yml added: backend/frontend/security/build jobs. CI monitoring protocol (rule 11) followed through 5 red iterations, each diagnosed and fixed:
1. mvnw missing +x bit (Windows checkout lost the exec bit) -> git update-index --chmod=+x
2. Trivy severity YAML flow-mapping comma parsed as extra key -> quoted 'CRITICAL,HIGH'
3. Trivy fs scan of pom.xml hit live Maven Central 429 (no local .m2 cache in a cold job) -> moved SCA scan into backend/frontend jobs (after deps already resolved)
4. Real CVEs found in Spring Boot 3.5.0-managed Tomcat/Spring Security/Spring Core/Jackson -> bumped to Spring Boot 3.5.16
5. Real CVE in eclipse-temurin's Ubuntu-based image (pebble binary, golang.org/x/net) + Alpine's p11-kit -> switched backend Dockerfile to -alpine base + apk upgrade
Final run 28746846936: all 4 jobs (security, backend, frontend, build) green.

## 2026-07-05 — MILESTONE: Sprint 1 shipped
Foundation infrastructure complete and pushed to origin/master (commits ea3ef4d..7bc66a4). CI green. Video recording skipped per user instruction (final sprint only). Coverage gate not yet enforced per approved test-strategy doc (Story 8.3 turns it on).

## 2026-07-06 — Stories 2.1/2.2/2.3/2.5 backend done
Auth backend complete: User entity/repo (Flyway V2 adds refresh_token_hash/expires_at to users), BCrypt password hashing, JwtService (jjwt, 15min HS256 access tokens, opaque SHA-256-hashed rotating refresh tokens in HttpOnly/Secure/SameSite=Strict cookie), JwtAuthenticationFilter, SecurityConfig (stateless, role-based path rules), AuthController (register/login/refresh/logout), AdminSeeder (ApplicationRunner, not a SQL migration — keeps bcrypt hashing consistent with the rest of the app, deviates from the DBA doc's illustrative migration name). Stub role-gated endpoints added for candidate/employer/admin to prove Story 2.4's authorization end-to-end.
11 new integration tests (AuthFlowTests, Testcontainers Postgres) all pass: register/duplicate/admin-rejection, login success/wrong-password/unknown-email (same 401), refresh rotation + reuse-rejection (adversarial), logout+refresh-rejected, role cross-access 403, no-token 403, tampered-token 403.

## 2026-07-06 — Story 2.4 frontend done, checkpoint pushed
Frontend: AuthService (in-memory access token, silent session restore via APP_INITIALIZER + /auth/refresh), auth interceptor (Bearer header), roleGuard/authGuard, login/register Material forms, role-gated dashboard stubs (candidate/employer/admin) calling their backend stub endpoints. 3 new guard unit tests pass (5 total frontend tests green). Fixed a TS field-initializer-ordering bug (useDefineForClassFields + constructor-parameter properties) by switching to inject().
Pushed commit f60ba70 to origin/master per user request to stop now.
DEVIATION FROM RULE 11: CI triggered by this push has NOT been watched to green this session (user asked to end immediately). Next session's first action must be `gh run list` / `gh run view` to check status and fix if red before any other work.

## MILESTONE — 2026-07-06 — Sprint 2 VERIFY+SHIP closed out
- CI confirmed green on commit f60ba70 (run 28770777990, 3m34s) and on f60ba70's follow-up checkpoint commit 6dfe615 (run 28770868624).
- Story 2.4 role guards verified end-to-end against the live docker-compose stack (backend authorization layer): register candidate -> login -> JWT role claim correct -> GET /api/v1/candidates/me 200 -> GET /api/v1/employers/me 403 -> GET /api/v1/admin/ping 403 for candidate token, 200 for admin-seed token -> unauthenticated request 403 (see issues.md for 401-vs-403 note). Frontend UI flow not driven interactively (browser extension unavailable this session); frontend route guards remain covered by unit tests only.
- Local dev pgdata volume was stale (old password from a prior session); reset via `docker compose down -v` + `up -d --build`, re-seeded clean.
- Backend test suite: 12/12 green (AuthFlowTests 11, BackendApplicationTests 1).
- Frontend test suite: 5/5 green (2 test files).
- No coverage tooling (jacoco/istanbul) wired up yet — by design, per docs/stories-jobstack.md Story 8.3 (Sprint 8) is when the CI coverage gate is introduced. Eyeballed: all existing tests green, no regressions.
- Sprint 2 (Epic 2: Authentication & Accounts) considered fully shipped as of this entry.

## PLAN — 2026-07-06 — Story 3.1 (candidate profile view/edit)
Batch:
  1. Flyway V3: relax candidate_profiles.full_name to nullable (profile is completed after registration per docs/ux-jobstack.md flow, not at registration).
  2. AuthService.register: auto-create empty candidate_profiles row when role=CANDIDATE.
  3. CandidateProfile JPA entity + repository.
  4. GET /api/v1/candidates/me returns full_name, phone, sector, city.
  5. PUT /api/v1/candidates/me updates those fields with validation.
  6. Tests: profile auto-created on candidate registration, GET/PUT happy paths, validation errors.

## TASK — 2026-07-06 — Story 3.1 (candidate profile view/edit) backend done
- Flyway V3: candidate_profiles.full_name made nullable (profile completed after registration, not at registration).
- AuthService.register now auto-creates an empty CandidateProfile row for role=CANDIDATE.
- Added CandidateProfile entity, CandidateProfileRepository.
- CandidateController: GET /api/v1/candidates/me and PUT /api/v1/candidates/me (fullName/phone/sector/city, validated against DB column sizes).
- Tests: 5 new (CandidateProfileTests) covering profile-created-on-register, GET/PUT happy path, validation rejection (fullName > 200 chars -> 400), unauthenticated rejection, cross-role rejection (employer token blocked from /candidates/me).
- Full backend suite: 17/17 green (11 AuthFlowTests + 1 context load + 5 CandidateProfileTests).
- Frontend for 3.1 not yet touched (deferred to Story 3.3 per plan).
Next: Story 3.2 (CV upload).

## PLAN — 2026-07-06 — Story 3.2 (CV upload)
Batch:
  1. Dockerfile: create /data/cvs owned by app user; docker-compose.yml: new cvdata named volume mounted there.
  2. application.properties: cv.storage.path (default /data/cvs), multipart max file size config.
  3. POST /api/v1/candidates/me/cv (multipart): validate magic bytes (%PDF), size <=5MB, store as {userId}.pdf, update candidate_profiles.cv_path.
  4. GET /api/v1/candidates/me/cv: ownership-checked download (candidate's own JWT only).
  5. Tests: valid PDF accepted, non-PDF with .pdf extension rejected (magic-byte check), oversized file rejected, download requires auth, cross-role/cross-user access blocked.

## FIX — 2026-07-06 — Root cause found and fixed: ResponseStatusException surfacing as empty 403 in live stack
Investigated the open issue from last session (403 on GET /api/v1/candidates/me). Brought up the docker-compose stack, reproduced directly against the api container (bypassing nginx to rule it out): duplicate-email register, wrong-password login, and self-register-as-ADMIN all incorrectly returned 403 instead of their real status (409/401/400) — proving this was never specific to the candidate profile endpoint. Root cause: `ResponseStatusException` triggers a servlet error-dispatch to `/error`; Spring Boot's security auto-config re-runs the `SecurityFilterChain` on that `DispatcherType.ERROR` dispatch; `/error` wasn't in the `permitAll` list so it fell to `anyRequest().authenticated()`, and with no custom `AuthenticationEntryPoint` the default `Http403ForbiddenEntryPoint` overwrote the real status with an empty 403. Fix: added `.requestMatchers("/error").permitAll()` in `SecurityConfig`. Full detail in issues.md.
Verified end-to-end against the rebuilt live stack (through nginx on 8090): register/duplicate/admin-blocked/login/wrong-password all return correct codes now; GET/PUT `/api/v1/candidates/me` 200; cross-role access still correctly denied (403); CV upload (200) and download (200, correct PDF bytes returned) both verified. Backend suite re-run: 23/23 green, no regressions. Docker stack torn down cleanly after verification (`docker compose down`).
Story 3.2 (CV upload) now considered fully verified end-to-end — this was the last blocker noted at the end of last session.

## TASK — 2026-07-06 — Story 3.3 done: Candidate profile screen (UI)
- Backend: added `hasCv` boolean to `CandidateProfileResponse` (derived from `cvPath != null`) so the frontend can detect a missing CV, not just missing text fields. 1 new assertion in CandidateProfileTests, 1 in CvUploadTests. Backend suite: 23/23 green.
- Frontend: `candidate-home.component.ts/html` rebuilt as the real profile screen — reactive form (fullName/phone/sector/city) wired to GET/PUT `/api/v1/candidates/me`, file input wired to POST `/api/v1/candidates/me/cv`, incomplete-profile banner ("Complete your profile and upload a CV before you can apply to jobs") shown whenever any field or the CV is missing, per docs/ux-jobstack.md Flow 1. New `candidate-profile.model.ts` for the response shape.
- Frontend tests: 7 new (candidate-home.component.spec.ts) — load/populate, incomplete flagged, complete not flagged, save via PUT, CV upload clears incomplete flag, upload error surfaced. Full frontend suite: 11/11 green (3 test files).
- Live verification: Chrome extension wasn't connected this session, so used Playwright (playwright-core installed standalone in the session scratchpad, not added as a project dependency) driving Chromium against the rebuilt live docker stack — registered a fresh candidate, confirmed incomplete banner on load, filled+saved the form, uploaded a real PDF, confirmed the banner cleared and CV status updated, reloaded the page and confirmed both the saved fields and hasCv persisted. Screenshots captured confirming correct rendering at each step.
- Docker stack torn down cleanly after verification.
Story 3.3 (Epic 3, last story) done. Sprint 3 (Epic 3: Candidate Profile & CV) now feature-complete; next is Sprint 3 VERIFY+SHIP (full test suites already green from this session — re-run at SHIP time, log snapshot, commit, push).
