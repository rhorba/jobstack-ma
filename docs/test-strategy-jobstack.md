# Test Strategy: JobStack.ma
**Stories Reference**: docs/stories-jobstack.md (drafted after this doc; risk assessment below drives its acceptance criteria)
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: Test Architect

**HANDOFF: Security Engineer → Test Architect**
Context: Security baseline approved — CMI callback spoofing, IDOR, CV/PII exposure, JWT tampering are the top risks.
Need: Risk-based test strategy and adversarial checklist covering those risks plus the core candidate/employer/admin flows.

## 1. Risk Assessment
| Component | Impact | Frequency | Complexity | Risk | Test Level |
|---|---|---|---|---|---|
| Auth (JWT issue/refresh, roles) | Critical (5) | Low (2) | Medium (3) | 10 | Maximum |
| Payment (CMI checkout + callback) | Critical (5) | Medium (3) | High (5) | 13 | Maximum |
| CV upload | High (4) | Low (2) | Medium (3) | 9 | High |
| Job search/filter | Medium (3) | High (5) | Medium (3) | 11 | High |
| Employer dashboard / applicants | High (4) | Medium (3) | Medium (3) | 10 | High |
| Admin moderation | High (4) | Low (2) | Low (2) | 8 | Standard |
| Candidate profile CRUD | Medium (3) | Medium (3) | Low (2) | 8 | Standard |

## 2. Test Pyramid Targets
| Layer | Coverage Target | Tooling |
|---|---|---|
| Unit | ≥ 60% of business logic | JUnit 5 (backend), Jasmine/Karma or Jest (Angular) |
| Integration | ≥ 40% of API + DB layer | Spring Boot Test + Testcontainers (real Postgres) |
| E2E | Critical happy paths only | Playwright |
| **Combined gate** | **≥ 80%** — non-negotiable | CI blocks merge if below (per CLAUDE.md rule 6) |

## 3. ATDD Acceptance Scenarios (critical paths)
```gherkin
Feature: Candidate applies to a job

  Scenario: Successful one-click apply
    Given I am a logged-in candidate with a complete profile and uploaded CV
    When I click Apply on a LIVE job posting
    Then my application is recorded
    And I cannot apply to the same posting twice

  Scenario: Apply blocked without a CV
    Given I am a logged-in candidate with no CV uploaded
    When I click Apply on a LIVE job posting
    Then I see an error telling me to upload a CV first

Feature: Employer posts and pays for a job

  Scenario: Successful payment activates the posting
    Given I am a logged-in employer with a draft job posting
    When I complete CMI checkout successfully
    Then the posting status becomes LIVE
    And it is visible in public job search within 1 minute

  Scenario: Failed/cancelled payment leaves posting inactive
    Given I am a logged-in employer with a draft job posting
    When my CMI checkout fails or is cancelled
    Then the posting status remains PENDING_PAYMENT
    And it is not visible in public job search

  Scenario: Spoofed payment callback is rejected
    Given a CMI callback arrives without a valid signature
    When the system processes it
    Then the payment is not marked CONFIRMED
    And the attempt is logged for review

Feature: Admin moderates a job posting

  Scenario: Reject a posting
    Given a LIVE job posting violates guidelines
    When an admin rejects it with a reason
    Then the posting status becomes REJECTED
    And the employer receives an email with the reason
```

## 4. Adversarial Checklist (high-risk components only)

### Auth (Maximum)
- [ ] Expired/tampered JWT rejected on every protected endpoint
- [ ] Role claim tampering attempt rejected (signature validation catches it)
- [ ] Refresh token reuse after rotation is rejected
- [ ] Horizontal access: candidate A cannot fetch candidate B's profile/applications by guessing IDs

### Payment (Maximum)
- [ ] CMI callback without valid signature is rejected and logged
- [ ] Duplicate callback for the same transaction does not double-activate or double-charge state
- [ ] Race: two checkout attempts for the same posting don't create two CONFIRMED payments
- [ ] Amount tampering: callback claiming a different amount than 490 MAD is rejected/flagged

### CV Upload (High)
- [ ] Non-PDF file with `.pdf` extension rejected (magic-byte check, not extension-only)
- [ ] Oversized file (> 5MB) rejected with clear error
- [ ] Path traversal in filename handled (stored filename never derived directly from user input)
- [ ] Uploaded file never served from a public/guessable URL (ownership-checked download only)

### Job Search / Employer Dashboard (High)
- [ ] Employer cannot list another employer's applicants (IDOR check on `job_posting_id` ownership)
- [ ] Pagination abuse (negative page, huge page size) handled gracefully
- [ ] SQL injection attempted in every search/filter field (sector, city, contract type, text)

## 5. Release Gate Criteria
- [ ] All acceptance scenarios above pass
- [ ] Combined unit + integration coverage ≥ 80% (CI-enforced)
- [ ] No critical/high security findings open (per DevOps SCA/SAST scan)
- [ ] E2E happy paths pass for: candidate apply, employer post+pay, admin moderate
- [ ] Video recording only required at the final sprint's release gate (per user instruction — deviates from CLAUDE.md's default "every version" cadence, logged in decisions.md)
