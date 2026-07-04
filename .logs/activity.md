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
