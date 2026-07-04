# PRD: JobStack.ma
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: PM | **Status**: Draft

## 1. Problem Statement
No single job board focuses on Morocco's engineering sectors (automotive, pharma, aerospace, battery plants). AutoJobs.ma proved the 490 MAD/30-day pay-per-post model works for automotive; the same demand exists — unmet — across the other industrial sectors.

## 2. Goals & Success Metrics
| Goal | Metric | Target |
|---|---|---|
| Validate multi-sector demand | Paid job posts | 50 within 90 days of launch |
| Candidate engagement | Active candidate profiles | 500 within 90 days |
| Revenue | MRR from job posts | 24,500 MAD/month by month 3 |

## 3. User Stories
- As a **candidate**, I want to create a profile with CV upload, so employers can find me.
- As a **candidate**, I want to search/filter jobs by sector and location, so I find relevant openings fast.
- As a **candidate**, I want to apply to a job in one click using my saved profile, so applying is fast.
- As an **employer**, I want to post a job and pay 490 MAD for 30-day visibility, so I can hire.
- As an **employer**, I want a dashboard of my postings and applicants, so I can track hiring.
- As an **admin**, I want to moderate postings and accounts, so the platform stays trustworthy.
- As an **admin**, I want platform-wide metrics (posts, revenue, users), so I can track business health.

## 4. Scope
### In Scope
- Candidate registration, profile, CV upload (PDF)
- Job search/filter by sector, city, contract type
- One-click apply
- Employer registration + company verification
- Job posting + CMI payment (490 MAD / 30 days)
- Employer dashboard (postings + applicants)
- Admin moderation panel + platform metrics
- Transactional email (SMTP), product analytics (PostHog)

### Out of Scope (MVP)
- Stripe / international payments
- Native mobile apps
- In-app messaging/chat
- AI-based candidate matching
- Employer subscription tiers beyond pay-per-post
- Kubernetes / multi-region infra
- Languages beyond FR + EN (AR deferred)

## 5. Requirements
### Functional
- FR-1: Candidates can register, edit profile, upload CV (PDF, max 5MB)
- FR-2: Candidates can search/filter jobs by sector, city, contract type
- FR-3: Candidates can apply to a job in one click (reuses profile + CV)
- FR-4: Employers can register and verify their company
- FR-5: Employers can create a job post, pay via CMI, post live for 30 days
- FR-6: Employers can view applicants per posting in a dashboard
- FR-7: Admins can approve/reject/remove postings and suspend accounts
- FR-8: System sends transactional emails (registration, application received, post expiring)

### Non-Functional
- NFR-1: Performance — job search results p95 < 500ms for catalogs up to 10k listings
- NFR-2: Security — PII (CVs, contact info) encrypted at rest; no raw payment data stored (CMI hosted flow)
- NFR-3: Accessibility — WCAG 2.1 AA on core candidate/employer flows
- NFR-4: Availability — 99% uptime target for MVP (single-region Docker deployment)

## 6. Constraints & Assumptions
- Small team — monolith over microservices (YAGNI); Docker only, no Kubernetes unless a real scaling trigger appears
- Morocco-only launch — CMI is the required payment rail
- AutoJobs.ma's 490 MAD/30-day pricing is assumed to transfer directly to new sectors without re-validation

## 7. Risks
| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Employers don't adopt a new brand outside AutoJobs.ma | M | H | Cross-promote to AutoJobs.ma audience; soft-launch with existing customers |
| CMI integration delays launch | M | M | Build/test against CMI sandbox starting Sprint 1 |
| Candidate supply too thin at launch (chicken-and-egg) | H | M | Seed candidate profiles via manual outreach before employer-facing launch |
| Low differentiation from generic job boards | M | M | Lean into sector-specific filtering/branding |

## 8. Timeline
| Milestone | Target Date |
|---|---|
| PRD Approved | 2026-07-04 |
| Architecture Done | 2026-07-04 |
| Implementation Start | Sprint 1 |
| MVP Ready | End of final sprint (see stories doc) |
