# Stories: JobStack.ma
**PRD**: docs/prd-jobstack.md
**Architecture**: docs/architecture-jobstack.md
**Database**: docs/database-jobstack.md
**Security**: docs/security-jobstack.md
**Test Strategy**: docs/test-strategy-jobstack.md

**HANDOFF: DevOps/DevSecOps → Scrum Master + Test Architect**
Context: All foundation docs approved (PRD, system design, architecture, security, database, UX, UI, test strategy, DevOps).
Need: Full sprint backlog breaking the MVP into epics/stories with ATDD acceptance criteria, ready for Session 2+ execution.

---

## Epic 1: Project Foundation & Infrastructure
Scaffolds both codebases and the deployment pipeline so every later story has somewhere to land.

### Story 1.1: Backend project scaffold
**Priority**: Must | **Size**: S | **Specialist**: Backend Dev
As a developer, I want a Spring Boot 3.x project on Java 25 with the package-by-feature structure from the architecture doc, so subsequent stories have a place to add code.
**Acceptance Criteria**:
```gherkin
Given a fresh clone of the repo
When I run ./mvnw spring-boot:run
Then the app starts and GET /actuator/health returns 200
```
**Technical Notes**: Maven, package structure per ADR-1 (candidate/employer/job/application/payment/admin/shared).

### Story 1.2: Frontend project scaffold
**Priority**: Must | **Size**: S | **Specialist**: Frontend Dev
As a developer, I want an Angular 22 standalone-components project with Angular Material installed, so UI stories have a place to add screens.
**Acceptance Criteria**:
```gherkin
Given a fresh clone of the repo
When I run ng serve
Then the app loads a placeholder home page with the Material theme applied
```

### Story 1.3: Postgres + Flyway migration V1
**Priority**: Must | **Size**: S | **Specialist**: DBA / Backend Dev
As a developer, I want the initial schema applied via Flyway on startup, so all tables exist before feature work begins.
**Acceptance Criteria**:
```gherkin
Given a clean Postgres container
When the Spring Boot app starts
Then all 6 tables from docs/database-jobstack.md exist with their indexes
```
**Dependencies**: 1.1

### Story 1.4: Docker Compose dev stack
**Priority**: Must | **Size**: S | **Specialist**: DevOps
As a developer, I want `docker-compose up` to run nginx+api+db locally, so the full stack is runnable in one command.
**Acceptance Criteria**:
```gherkin
Given .env is populated from .env.example
When I run docker-compose up
Then the frontend is reachable on localhost and proxies /api calls to the backend
```
**Dependencies**: 1.1, 1.2

### Story 1.5: CI pipeline skeleton
**Priority**: Must | **Size**: S | **Specialist**: DevOps
As a team, I want CI to run lint+test+security+build on every push, so quality gates are enforced from day one.
**Acceptance Criteria**:
```gherkin
Given a push to any branch
When CI runs
Then backend tests, frontend tests, Semgrep, Trivy, and Gitleaks all execute and the pipeline is visible as a required check
```
**Dependencies**: 1.1, 1.2

---

## Epic 2: Authentication & Accounts
### Story 2.1: User registration (candidate/employer)
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev
As a candidate or employer, I want to register with email/password, so I can access role-specific features.
**Acceptance Criteria**:
```gherkin
Given a new email address
When I POST /api/v1/auth/register with role=CANDIDATE or EMPLOYER
Then a user is created with bcrypt-hashed password and the requested role
And duplicate emails are rejected with a clear error
```
**Technical Notes**: Uses `users` table; role fixed at registration (no self-promotion to ADMIN).

### Story 2.2: Login + JWT issuance
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev
As a registered user, I want to log in and receive tokens, so I can call protected endpoints.
**Acceptance Criteria**:
```gherkin
Given valid credentials
When I POST /api/v1/auth/login
Then I receive an access token (≤15min expiry) and a refresh token (HttpOnly cookie, ≤7d, rotating)
And invalid credentials return a generic 401 (no user-enumeration hints)
```
**Dependencies**: 2.1. **Security**: per docs/security-jobstack.md §3.

### Story 2.3: Refresh token rotation + logout
**Priority**: Must | **Size**: S | **Specialist**: Backend Dev
Acceptance Criteria:
```gherkin
Given a valid refresh token
When I POST /api/v1/auth/refresh
Then I get a new access token and the old refresh token is invalidated
And reusing an invalidated refresh token is rejected (adversarial check, test-strategy §4)
```

### Story 2.4: Role-based route guards (frontend + backend)
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
Acceptance Criteria:
```gherkin
Given I am logged in as CANDIDATE
When I call an EMPLOYER-only or ADMIN-only endpoint
Then I receive 403
And the Angular app hides/redirects away from role-inappropriate routes
```

### Story 2.5: Admin seed account
**Priority**: Must | **Size**: XS | **Specialist**: DBA / DevOps
Acceptance Criteria:
```gherkin
Given the V2 migration runs with ADMIN_SEED_EMAIL / ADMIN_SEED_PASSWORD env vars set
When the app starts for the first time
Then exactly one ADMIN user exists, created from those env vars (never hardcoded)
```

---

## Epic 3: Candidate Profile & CV
### Story 3.1: View/edit candidate profile
**Priority**: Must | **Size**: S | **Specialist**: Backend Dev + Frontend Dev
Acceptance Criteria:
```gherkin
Given I am a logged-in candidate
When I GET or PUT /api/v1/candidates/me
Then I can view and update my name, phone, sector, city
```

### Story 3.2: CV upload
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
Acceptance Criteria:
```gherkin
Given I am a logged-in candidate
When I POST /api/v1/candidates/me/cv with a valid PDF ≤5MB
Then the file is stored outside the webroot and cv_path is updated
And a non-PDF (checked by magic bytes) or oversized file is rejected with a clear error
```
**Technical Notes**: adversarial checks from test-strategy §4 (CV Upload).

### Story 3.3: Candidate profile screen (UI)
**Priority**: Must | **Size**: S | **Specialist**: Frontend Dev
Acceptance Criteria:
```gherkin
Given I am on my profile screen
When my profile is incomplete
Then I see a prompt to complete it before I can apply to jobs (per UX flow 1)
```
**Dependencies**: 3.1, 3.2

---

## Epic 4: Job Posting & Search
### Story 4.1: Employer/company registration
**Priority**: Must | **Size**: S | **Specialist**: Backend Dev
Acceptance Criteria:
```gherkin
Given a logged-in EMPLOYER
When they create a company profile
Then a companies row is created with owner_user_id = their user id
```

### Story 4.2: Create job posting (draft)
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev
Acceptance Criteria:
```gherkin
Given a logged-in employer who owns a company
When they POST /api/v1/jobs with title/sector/city/contract/description
Then a job_postings row is created with status=DRAFT
```
**Dependencies**: 4.1

### Story 4.3: Public job search/filter
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
Acceptance Criteria:
```gherkin
Given LIVE job postings exist
When I GET /api/v1/jobs?sector=&city=&contract_type=
Then I get matching results in <500ms p95 (NFR-1) using idx_job_postings_search
And SQL-injection attempts in filter params are safely parameterized (test-strategy §4)
```

### Story 4.4: Job search + detail screens (UI)
**Priority**: Must | **Size**: M | **Specialist**: Frontend Dev
Acceptance Criteria:
```gherkin
Given the job search screen (per UX wireframe)
When I filter and open a job card
Then I see the job detail page with an Apply CTA (disabled/prompted if I have no CV yet)
```
**Dependencies**: 4.3

---

## Epic 5: Payment (CMI)
### Story 5.1: CMI checkout initiation
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev
Acceptance Criteria:
```gherkin
Given an employer's DRAFT job posting
When they POST /api/v1/jobs/:id/checkout
Then a payments row is created with status=INITIATED and I am redirected to CMI's hosted payment page
And posting status becomes PENDING_PAYMENT
```
**Dependencies**: 4.2

### Story 5.2: CMI callback handling
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev
Acceptance Criteria:
```gherkin
Given a CMI callback with a valid signature confirming payment
When POST /api/v1/payments/cmi/callback is received
Then the payment is marked CONFIRMED and the job posting becomes LIVE with expires_at = now()+30d
Given a callback with an invalid/missing signature
Then the payment is not updated and the attempt is logged
Given a duplicate callback for an already-CONFIRMED payment
Then it is a no-op (idempotent), not a double-activation
```
**Dependencies**: 5.1. **Security**: Maximum rigor per test-strategy §1 and §4 (Payment).

### Story 5.3: Checkout + status UI
**Priority**: Must | **Size**: S | **Specialist**: Frontend Dev
Acceptance Criteria:
```gherkin
Given I am mid-checkout
When payment succeeds or fails/cancels
Then I see the corresponding confirmation or retry screen (per UX flow 2)
```
**Dependencies**: 5.1, 5.2

---

## Epic 6: Application Flow
### Story 6.1: One-click apply
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
Acceptance Criteria:
```gherkin
Given a logged-in candidate with a complete profile and CV
When they click Apply on a LIVE posting
Then an applications row is created (unique per job+candidate — no duplicate applies)
Given a candidate with no CV
Then Apply is blocked with a clear message (per UX flow 1 error path)
```
**Dependencies**: 3.2, 4.3

### Story 6.2: Employer applicant dashboard
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
Acceptance Criteria:
```gherkin
Given an employer who owns a job posting with applicants
When they GET /api/v1/employers/me/jobs/:id/applicants
Then they see the applicant list (name, contact, CV download link)
And an employer cannot fetch applicants for a posting they don't own (IDOR check, test-strategy §4)
```
**Dependencies**: 6.1

---

## Epic 7: Admin Moderation & Notifications
### Story 7.1: Admin moderation queue
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
Acceptance Criteria:
```gherkin
Given postings in PENDING_PAYMENT or LIVE status
When an admin views the moderation queue
Then they can approve (leave LIVE), reject (with reason), or remove a posting
```

### Story 7.2: Admin account suspension
**Priority**: Should | **Size**: S | **Specialist**: Backend Dev
Acceptance Criteria:
```gherkin
Given an admin viewing a user account
When they PUT /api/v1/admin/users/:id/status = SUSPENDED
Then that user can no longer log in, and existing tokens are rejected on next validation
```

### Story 7.3: Admin platform metrics
**Priority**: Should | **Size**: S | **Specialist**: Backend Dev + Frontend Dev
Acceptance Criteria:
```gherkin
Given an admin on the metrics screen
When the page loads
Then they see counts of postings, applications, and confirmed payments (revenue proxy)
```

### Story 7.4: Transactional emails
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev
Acceptance Criteria:
```gherkin
Given a registration, application submission, posting rejection, or posting-expiry-soon event
When it occurs
Then an SMTP email is sent via JavaMailSender with the relevant template
And email failures are logged, not silently swallowed
```

---

## Epic 8: Analytics & Hardening
### Story 8.1: PostHog event tracking
**Priority**: Should | **Size**: S | **Specialist**: Backend Dev
Acceptance Criteria:
```gherkin
Given a key funnel event (registration, job post paid, application submitted)
When it occurs
Then a PostHog event is sent asynchronously (never blocking the response)
```

### Story 8.2: Adversarial fixes pass
**Priority**: Must | **Size**: L | **Specialist**: Test Architect + Backend Dev
Acceptance Criteria:
```gherkin
Given the full adversarial checklist in docs/test-strategy-jobstack.md §4
When each check is executed
Then all Auth and Payment (Maximum-risk) checks pass, and any findings are fixed and re-tested
```

### Story 8.3: Coverage gate to ≥80%
**Priority**: Must | **Size**: M | **Specialist**: Tester
Acceptance Criteria:
```gherkin
Given the combined unit+integration coverage report
When it is below 80%
Then missing tests are added until the CI coverage gate passes
```

---

## Epic 9: Production Readiness & Release
### Story 9.1: Production Docker Compose hardening
**Priority**: Must | **Size**: M | **Specialist**: DevOps
Acceptance Criteria:
```gherkin
Given the production docker-compose.yml
When deployed
Then TLS is enforced (HSTS), containers run as non-root, and secrets come only from the host .env
```

### Story 9.2: Final release E2E + video recording
**Priority**: Must | **Size**: M | **Specialist**: Test Architect + Tester
Acceptance Criteria:
```gherkin
Given the MVP is feature-complete and CI is green
When the final sprint's release gate runs
Then Playwright E2E covers candidate apply, employer post+pay, and admin moderate — recorded to .recordings/v1.0-[date].webm
```
**Technical Notes**: Per user instruction, this is the ONLY video recording checkpoint (not every sprint) — see docs/test-strategy-jobstack.md §5.

---

## Sprint Allocation
| Sprint | Epic(s) | Stories | Sprint Goal |
|---|---|---|---|
| Sprint 1 | Epic 1 | 1.1, 1.2, 1.3, 1.4, 1.5 | Runnable skeleton: both apps, DB schema, Docker Compose, CI green |
| Sprint 2 | Epic 2 | 2.1, 2.2, 2.3, 2.4, 2.5 | Full auth: register/login/refresh/roles/admin seed |
| Sprint 3 | Epic 3 | 3.1, 3.2, 3.3 | Candidate profile + CV upload end-to-end |
| Sprint 4 | Epic 4 | 4.1, 4.2, 4.3, 4.4 | Employer can draft a job; public can search/filter |
| Sprint 5 | Epic 5 | 5.1, 5.2, 5.3 | CMI payment activates postings, incl. spoofed-callback defense |
| Sprint 6 | Epic 6 | 6.1, 6.2 | Candidates apply; employers see applicants |
| Sprint 7 | Epic 7 | 7.1, 7.2, 7.3, 7.4 | Admin moderation, suspension, metrics, transactional email |
| Sprint 8 | Epic 8 | 8.1, 8.2, 8.3 | Analytics wired up; adversarial findings fixed; ≥80% coverage |
| Sprint 9 | Epic 9 | 9.1, 9.2 | Production-hardened Docker deploy; final E2E + video; MVP ships |

**Definition of Done (every story)**: code written and reviewed, tests pass, no critical/high security findings, combined coverage doesn't regress below 80%, story's acceptance criteria demoed, sprint ends with `git push origin <branch>` (per CLAUDE.md rule 7).
