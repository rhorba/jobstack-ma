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
  sector        VARCHAR(50),
  city          VARCHAR(100),
  cv_path       VARCHAR(500),
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
  contract_type VARCHAR(30) NOT NULL,
  status        VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                 CHECK (status IN ('DRAFT','PENDING_PAYMENT','LIVE','EXPIRED','REJECTED','REMOVED')),
  published_at  TIMESTAMPTZ,
  expires_at    TIMESTAMPTZ,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table: payments
CREATE TABLE payments (
  id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_posting_id     UUID NOT NULL UNIQUE REFERENCES job_postings(id) ON DELETE CASCADE,
  cmi_transaction_id VARCHAR(100) UNIQUE,
  amount_mad         NUMERIC(10,2) NOT NULL DEFAULT 490.00,
  status             VARCHAR(20) NOT NULL DEFAULT 'INITIATED'
                      CHECK (status IN ('INITIATED','CONFIRMED','FAILED')),
  confirmed_at       TIMESTAMPTZ,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
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

-- Indexes (docs/database-jobstack.md §4)
CREATE INDEX idx_job_postings_search ON job_postings (status, sector, city);
CREATE INDEX idx_job_postings_company ON job_postings (company_id);
CREATE INDEX idx_applications_job ON applications (job_posting_id);
CREATE INDEX idx_applications_candidate ON applications (candidate_profile_id);
CREATE INDEX idx_companies_owner ON companies (owner_user_id);
