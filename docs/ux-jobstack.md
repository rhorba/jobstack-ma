# UX Foundation: JobStack.ma
**PRD Reference**: docs/prd-jobstack.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: UX Designer

**HANDOFF: PM → UX Designer**
Context: PRD approved — candidate/employer/admin roles, core flows are apply, post+pay, moderate.
Need: Lightweight personas, IA, and the top 3 flows with wireframes and error states (YAGNI — no multi-week research for an MVP).

## 1. User Personas
| Persona | Role | Goal | Pain Point |
|---|---|---|---|
| Yassine | Candidate, mechanical engineer, Kenitra | Find a sector-specific job fast, apply with minimal friction | Generic job boards bury industrial roles under unrelated listings |
| Fatima-Zahra | HR/recruiter at a Tier-1 automotive supplier | Post a role and reach qualified sector candidates quickly | Existing boards give low-quality, off-sector applicants for the price |

## 2. Information Architecture / Site Map
```
[JobStack.ma]
├── Home (job search)
├── Jobs
│   ├── Job list (filter: sector, city, contract type)
│   └── Job detail
├── Candidate
│   ├── Register / Login
│   ├── Profile (edit + CV upload)
│   └── My Applications
├── Employer
│   ├── Register / Login (+ company verification)
│   ├── Dashboard (my postings)
│   ├── Post a Job (create → pay via CMI → live)
│   └── Applicants (per posting)
└── Admin
    ├── Moderation queue (postings)
    ├── Accounts (suspend/reinstate)
    └── Metrics
```

## 3. Core User Flows

### Flow 1: Candidate applies to a job
```
(Land on Home) → [Search/filter jobs] → [Open job detail] → <Logged in?>
    ↓ No                                                        ↓ Yes
[Register/Login] → [Complete profile + upload CV] → [Click Apply] → (Application submitted)
    ↓ Validation fails (bad CV type/size)
[Inline error: "PDF only, max 5MB"] → [Retry upload]
```

### Flow 2: Employer posts and pays for a job
```
(Employer dashboard) → [Post a Job form] → [Save draft] → [Checkout via CMI]
    ↓ Payment fails/cancelled                    ↓ Payment confirmed (callback)
[Draft stays PENDING_PAYMENT, retry checkout]   [Posting goes LIVE for 30 days] → (Success — visible in dashboard)
```

### Flow 3: Admin moderates a posting
```
(Admin moderation queue) → [Open posting] → <Meets guidelines?>
                                              ↓ Yes            ↓ No
                                         [Approve/leave LIVE]  [Reject + reason] → (Employer notified by email)
```

## 4. Key Screen Wireframes (text-based)

### Screen: Job Search (Home)
```
┌─────────────────────────────────────────┐
│ JobStack.ma      [Sector ▾][City ▾][🔍] │
├─────────────────────────────────────────┤
│ Job Card: Title | Company | City | Sector│
│ Job Card: Title | Company | City | Sector│
│ Job Card: Title | Company | City | Sector│
├─────────────────────────────────────────┤
│              [Load more]                 │
└─────────────────────────────────────────┘
```

### Screen: Post a Job (Employer)
```
┌─────────────────────────────────────────┐
│ Post a Job                               │
├─────────────────────────────────────────┤
│ Title:       [____________________]     │
│ Sector:      [Automotive ▾]              │
│ City:        [____________________]     │
│ Contract:    [CDI ▾]                     │
│ Description: [____________________]     │
│                                           │
│         [Continue to Payment — 490 MAD]  │
└─────────────────────────────────────────┘
```

## 5. Screen States
| Screen | Empty State | Loading | Error | Success |
|---|---|---|---|---|
| Job Search | "No jobs match your filters — try widening sector/city" | Skeleton job cards | "Search failed, retry" banner | Job cards rendered |
| Candidate Profile | "Add your CV to start applying" prompt | Spinner on save | Field-level validation messages | "Profile saved" toast |
| Employer Dashboard | "No postings yet — Post your first job" CTA | Skeleton rows | "Couldn't load postings, retry" | Postings table with status badges |
| Post a Job / Checkout | — | "Redirecting to CMI..." spinner | "Payment failed — try again" with retry button | "Your job is live for 30 days" confirmation |
| Admin Moderation Queue | "Nothing to review" | Skeleton rows | "Couldn't load queue, retry" | Queue list with Approve/Reject actions |
