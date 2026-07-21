# JobStack.ma — Multi-Sector Engineering Jobs Board

AutoJobs.ma proved the pay-per-post model works for Morocco's industrial sector. The TAM is much larger.

## Problem
No single job board focuses on Morocco's engineering sectors: automotive (Kenitra/Tangier), pharma (Casablanca), aerospace (Casablanca), battery plants (Jorf Lasfar).

## Solution
Multi-sector engineering job board — same 490 MAD/30-day pay-per-post model as AutoJobs.ma, extended to all industrial sectors. Candidate profiles, CV upload, employer dashboards.

## Stack
Spring Boot 3.5 (Java 25 LTS) + PostgreSQL 16 (Flyway) backend, Angular 22 + Material frontend, Docker Compose deploy (nginx + api + db), CMI (Moroccan payment gateway), SMTP email, PostHog analytics. See `docs/architecture-jobstack.md` and `docs/devops-jobstack.md` for details.

## Builds On
AutoJobs.ma — same architecture, broader sector coverage

## Key Roles
Candidate | Employer | Admin

## Status
MVP v1.0 shipped (Sprints 1-9 complete — see `.logs/sessions.md`). Payment runs behind a `MockPaymentGateway` pending real CMI merchant credentials; email delivery is wired but unverified pending a real SMTP App Password.

## Running locally
```
cp .env.example .env   # fill in real values
docker compose up -d   # dev stack: http://localhost:8090
```
Production stack (TLS/HSTS, non-root, hardened): `docker-compose.prod.yml` — see `docs/devops-jobstack.md` §7.
