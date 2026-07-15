# METRICS — JobStack.ma



## 2026-07-05 — Sprint 1 SPRINT_SNAPSHOT
Sprint 1 (Epic 1: Project Foundation & Infrastructure) complete.
Stories: 1.1 backend scaffold, 1.2 frontend scaffold, 1.3 Flyway migration, 1.4 Docker Compose dev stack, 1.5 CI pipeline — all done.
Test coverage: minimal at this stage (1 backend context-load test via Testcontainers, 2 frontend smoke tests) — expected per docs/test-strategy-jobstack.md, the 80% gate is enforced starting Story 8.3, not from Sprint 1. Not yet gating CI.
CI: green (run 28746846936) after 5 fix iterations, all logged.

## 2026-07-06 — Sprint 2 SPRINT_SNAPSHOT
Sprint 2 (Epic 2: Authentication & Accounts) complete.
Stories: 2.1 register, 2.2 login+JWT, 2.3 refresh rotation+logout, 2.4 role guards (backend+frontend), 2.5 admin seed — all done.
Tests: backend 12/12 green (11 AuthFlowTests + 1 context load), frontend 5/5 green (2 test files). Role guards additionally verified end-to-end against the live docker-compose stack (see activity.md milestone entry).
Coverage tooling: still not wired up — per docs/test-strategy-jobstack.md Story 8.3 (Sprint 8) is when the CI coverage gate is introduced, consistent with Sprint 1. Eyeballed only.
CI: green (run 28770777990, commit f60ba70).
Known minor issue carried to backlog: unauthenticated requests return 403 instead of 401 (see issues.md).

## 2026-07-06 — Sprint 3 SPRINT_SNAPSHOT
Sprint 3 (Epic 3: Candidate Profile & CV) complete.
Stories: 3.1 profile view/edit, 3.2 CV upload, 3.3 candidate profile screen (UI) — all done.
Tests: backend 23/23 green (11 AuthFlowTests + 1 context load + 5 CandidateProfileTests + 6 CvUploadTests), frontend 11/11 green (3 test files: app smoke, auth guard, candidate profile screen).
Verified end-to-end against the live docker-compose stack: candidate profile GET/PUT, CV upload/download, incomplete-profile banner, cross-role denial — all confirmed working (browser flow driven via Playwright since the Chrome extension was unavailable this session).
Coverage tooling: still not wired up — per docs/test-strategy-jobstack.md Story 8.3 (Sprint 8) is when the CI coverage gate is introduced, consistent with Sprints 1-2. Eyeballed only.
Fixed this sprint: systemic bug where any `ResponseStatusException` surfaced as an empty 403 in the live stack (servlet error-dispatch re-entering the security filter chain on `/error`) — see issues.md.
Known minor issue carried to backlog (unchanged): unauthenticated requests return 403 instead of 401.
CI: green (run 29391919227, commit b842750) — all 4 jobs (backend, security, frontend, build) passed.

## 2026-07-15 — Sprint 4 SPRINT_SNAPSHOT
Sprint 4 (Epic 4: Job Posting & Search) complete.
Stories: 4.1 employer/company registration, 4.2 create job posting (draft), 4.3 public job search/filter, 4.4 job search + detail screens (UI) — all done.
Tests: backend 39/39 green (11 AuthFlowTests + 1 context load + 5 CandidateProfileTests + 6 CvUploadTests + 5 EmployerCompanyTests + 5 JobPostingTests + 6 JobSearchTests), frontend 19/19 green (5 test files: app smoke, auth guard, candidate profile screen, job search, job detail).
Verified end-to-end against the live docker-compose stack: employer company creation, draft job posting creation, public search/filter (exact-match sector/city/contractType), job detail page, and all 4 Apply-CTA states (guest/wrong-role/missing-cv/ready) — all confirmed working live via Chrome. DRAFT postings confirmed not publicly visible.
Coverage tooling: still not wired up — per docs/stories-jobstack.md Story 8.3 (Sprint 8) is when the CI coverage gate is introduced. Eyeballed only, consistent with Sprints 1-3.
CI: green (run 29406704110, commit d20afda) — all 4 jobs (frontend, security, backend, build) passed.
