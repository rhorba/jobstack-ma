# Database Design: JobStack.ma
**Architecture Reference**: docs/architecture-jobstack.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: DBA

**HANDOFF: Software Architect → DBA**
Context: Architecture approved — Spring Boot monolith, single Postgres instance, Flyway migrations (ADR-5).
Need: Full schema, indexes, migration plan for candidate/employer/job/application/payment domains.

## 1. Database Selection
- **Engine**: PostgreSQL 16
- **Rationale**: Structured, relational data with real transactional needs (job posting ↔ payment ↔ application). No case for NoSQL/document flexibility. YAGNI default confirmed.
- **Hosting**: Self-hosted container (per system design SDR-1), own Docker volume

## 2. Entity-Relationship Model
```
users ──1:1──> candidate_profiles
users ──1:N──> companies (owner_user_id, employer role)
companies ──1:N──> job_postings
job_postings ──1:1──> payments
job_postings ──1:N──> applications
candidate_profiles ──1:N──> applications
```

## 3. Schema Design
```sql
-- Table: users (all roles — candidate/employer/admin — share auth identity)
CREATE TABLE users (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email         VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role          VARCHAR(20) NOT NULL CHECK (role IN ('CANDIDATE','EMPLOYER','ADMIN')),
  status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED')),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table: candidate_profiles
CREATE TABLE candidate_profiles (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  full_name     VARCHAR(200) NOT NULL,
  phone         VARCHAR(30),
  sector        VARCHAR(50),        -- automotive / pharma / aerospace / battery / other
  city          VARCHAR(100),
  cv_path       VARCHAR(500),        -- path on the CV volume, not a public URL
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table: companies
CREATE TABLE companies (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name          VARCHAR(200) NOT NULL,
  sector        VARCHAR(50) NOT NULL,
  city          VARCHAR(100),
  verified      BOOLEAN NOT NULL DEFAULT FALSE,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table: job_postings
CREATE TABLE job_postings (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id    UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
  title         VARCHAR(200) NOT NULL,
  description   TEXT NOT NULL,
  sector        VARCHAR(50) NOT NULL,
  city          VARCHAR(100) NOT NULL,
  contract_type VARCHAR(30) NOT NULL,   -- CDI / CDD / INTERNSHIP / etc.
  status        VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                 CHECK (status IN ('DRAFT','PENDING_PAYMENT','LIVE','EXPIRED','REJECTED','REMOVED')),
  published_at  TIMESTAMPTZ,
  expires_at    TIMESTAMPTZ,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table: payments
CREATE TABLE payments (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_posting_id UUID NOT NULL UNIQUE REFERENCES job_postings(id) ON DELETE CASCADE,
  cmi_transaction_id VARCHAR(100) UNIQUE,
  amount_mad     NUMERIC(10,2) NOT NULL DEFAULT 490.00,
  status         VARCHAR(20) NOT NULL DEFAULT 'INITIATED'
                  CHECK (status IN ('INITIATED','CONFIRMED','FAILED')),
  confirmed_at   TIMESTAMPTZ,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table: applications
CREATE TABLE applications (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_posting_id        UUID NOT NULL REFERENCES job_postings(id) ON DELETE CASCADE,
  candidate_profile_id  UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
  status                VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED'
                         CHECK (status IN ('SUBMITTED','VIEWED')),
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (job_posting_id, candidate_profile_id)
);
```

## 4. Index Strategy
| Table | Index Name | Columns | Query Pattern |
|---|---|---|---|
| job_postings | idx_job_postings_search | (status, sector, city) | Public search/filter — WHERE status='LIVE' AND sector=? AND city=? |
| job_postings | idx_job_postings_company | (company_id) | Employer dashboard — WHERE company_id=? |
| applications | idx_applications_job | (job_posting_id) | Employer applicant list — WHERE job_posting_id=? |
| applications | idx_applications_candidate | (candidate_profile_id) | Candidate's application history |
| companies | idx_companies_owner | (owner_user_id) | Employer's own companies |
| users | idx_users_email (implicit via UNIQUE) | (email) | Login lookup |

## 5. Migration Plan
| Migration File | Description | Reversible |
|---|---|---|
| V1__initial_schema.sql | users, candidate_profiles, companies, job_postings, payments, applications + all indexes | Yes (drop tables) |
| V2__seed_admin_user.sql | Insert first admin account (email/password from env at deploy time, not hardcoded) | Yes |

## 6. Access Patterns
| Use Case | Query Pattern | Index Coverage |
|---|---|---|
| Candidate searches jobs by sector+city | SELECT ... WHERE status='LIVE' AND sector=? AND city=? | idx_job_postings_search |
| Employer views own postings | SELECT ... WHERE company_id=? | idx_job_postings_company |
| Employer views applicants | SELECT ... WHERE job_posting_id=? | idx_applications_job |
| Admin moderation queue | SELECT ... WHERE status='PENDING_PAYMENT' OR status='LIVE' | idx_job_postings_search (status leading column) |

## 7. Sensitive Data
- Columns requiring encryption/careful handling: `users.password_hash` (bcrypt, not reversible), `candidate_profiles.phone`, `candidate_profiles.cv_path` (file itself on encrypted volume per security baseline)
- Row-level security: not needed at MVP — ownership enforced in the application layer (single DB role for the Spring Boot service); revisit only if direct multi-tenant DB access is ever exposed

## Backup Strategy
| Method | Frequency | Retention |
|---|---|---|
| pg_dump (logical) | Daily | 30 days |
| Manual restore test | Monthly | — |
