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
