# Security Baseline: JobStack.ma
**Architecture Reference**: docs/architecture-jobstack.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: Security Engineer

**HANDOFF: Software Architect → Security Engineer**
Context: Architecture approved — Spring Boot monolith, JWT auth, Postgres, CMI payment integration, CV file uploads.
Need: Threat model, auth/authz hardening spec, data protection plan sized for a solo-MVP stage (not enterprise).

## 1. Threat Model (5-Minute)
- **What are we building?** A job board handling candidate PII (CVs, contact info) and employer payments (via CMI) for Morocco's industrial sectors.
- **Who would attack it?** Mostly opportunistic/script-kiddie (credential stuffing, scraping, IDOR probing); low nation-state/insider risk at MVP stage; a motivated competitor could scrape job/candidate data.
- **Worst outcome?** Candidate PII leak (CVs, contact info), employer account takeover leading to fraudulent job posts, or a spoofed CMI callback that activates a job posting without real payment.

## 2. STRIDE Analysis (top risks only)
| Threat | Component | Mitigation | Status |
|---|---|---|---|
| Spoofing | CMI payment callback | Verify CMI signature/HMAC on every callback; reject unsigned/invalid requests | TODO |
| Tampering | Job posting / application ownership | Server-side ownership checks on every employer/candidate-scoped endpoint (no trust in client-supplied IDs) | TODO |
| Repudiation | Payment confirmation | Log every payment state transition (initiated/confirmed/failed) with CMI transaction ID | TODO |
| Info Disclosure | CV files, candidate PII | Files stored outside webroot, access via authenticated/ownership-checked endpoint only (never a public static URL) | TODO |
| DoS | Public job search/auth endpoints | Rate limiting on `/api/v1/auth/*` and `/api/v1/jobs` (login attempts, scraping) | TODO |
| Elevation of Privilege | Role claims in JWT | Roles set server-side only at issuance; never trust a role claim the client could tamper with (signed JWT, validated on every request) | TODO |

## 3. Authentication Strategy
- **Type**: JWT (access + refresh) via Spring Security — single-server monolith, but stateless JWT avoids sticky-session concerns if scaled later
- **MFA**: Not required for candidates/employers at MVP (YAGNI — low-value target for MFA-worthy attacks); **required consideration flagged** for admin accounts once more than one admin exists
- **Password policy**: bcrypt hashing, min 10 chars, no composition rules (per NIST guidance), no breached-list check at MVP (revisit if credential-stuffing incidents occur)
- **Session management**: Access token ≤ 15min, refresh token ≤ 7 days with rotation; refresh token stored as HttpOnly/Secure/SameSite=Strict cookie, access token in memory (not localStorage, to reduce XSS token theft)

## 4. Authorization Model
- **Pattern**: Simple roles (`CANDIDATE`, `EMPLOYER`, `ADMIN`) + per-resource ownership checks
- **Roles defined**:
  - `CANDIDATE` — own profile, own applications
  - `EMPLOYER` — own company, own job postings, applicants to own postings
  - `ADMIN` — moderate all postings/accounts, view platform metrics
- **Resource-level checks**: yes — every employer/candidate-scoped endpoint verifies the authenticated user owns the resource (e.g., an employer can only list applicants for their own job postings), not just role membership

## 5. Data Protection
- **PII fields**: candidate name, email, phone, CV file content, employer contact details
- **Encryption at rest**: Postgres column-level encryption (or disk-level via encrypted volume, minimum viable) for phone/email; CV files on an encrypted Docker volume
- **Encryption in transit**: HTTPS enforced end-to-end (nginx TLS termination), HSTS enabled
- **Secrets management**: all credentials (DB, JWT signing key, CMI keys, SMTP creds) via environment variables, never committed — see `.env.example`

## 6. Security Requirements for Dev Team
- [ ] All inputs validated server-side (Bean Validation on every DTO)
- [ ] Output encoded for context (Angular's default HTML sanitization relied on; no raw HTML injection from user content)
- [ ] No secrets in code, logs, or error messages (generic error responses to clients; details server-side logs only)
- [ ] HTTPS only, security headers configured (HSTS, X-Content-Type-Options, X-Frame-Options, CSP baseline)
- [ ] Dependencies scanned in CI (SCA) — see docs/devops-jobstack.md
- [ ] File uploads: PDF-only (magic-byte check, not just extension), 5MB max, stored outside webroot
- [ ] CMI callback signature verified before any payment state change
