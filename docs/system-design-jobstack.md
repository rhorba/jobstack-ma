# System Design: JobStack.ma
**PRD Reference**: docs/prd-jobstack.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: System Designer

**HANDOFF: PM → System Designer**
Context: PRD approved — multi-sector engineering job board, candidate/employer/admin roles, MVP scope (profiles, search, one-click apply, CMI-paid job posts, dashboards, moderation).
Need: Topology + NFRs sized for a small-team MVP on Java/Angular/Postgres/Docker — no premature scale.
Constraints already decided: monolith, Docker only (no Kubernetes unless a real trigger appears), CMI-only payments, SMTP email, PostHog analytics.

## 1. Non-Functional Requirements
| Attribute | Target | Notes |
|---|---|---|
| Availability | 99% SLA | Single Docker host, active-restart on failure. Matches PRD NFR-4. |
| Latency (p99) | < 800ms | p95 search < 500ms per PRD NFR-1; p99 relaxed for non-search endpoints |
| Throughput | ~20 RPS peak | Estimated from 500 candidates + 50 employers/90 days — far below scaling thresholds |
| Data Volume | < 1 GB/day | CV uploads (avg 1MB) dominate; text/metadata negligible |
| Retention | Indefinite (candidate/employer data); CVs retained while account active | No regulatory retention limit identified for MA at MVP stage |
| Recovery (RTO) | 4 hours | Acceptable for MVP — restore from latest DB backup + redeploy container |
| Recovery (RPO) | 24 hours | Nightly Postgres backup is sufficient at this data volume |

## 2. Component Topology
```
[Clients: Web browser (Angular SPA)]
        ↓ HTTPS
[Reverse proxy (nginx, TLS termination)]
        ↓
[Spring Boot API (monolith, single container)]
  ├── PostgreSQL (own container, volume-backed)
  ├── Local/volume file storage → CV uploads (PDF)
  └── External integrations (sync HTTP calls, no queue):
        ├── CMI (payment)
        ├── SMTP relay (transactional email)
        └── PostHog (analytics events)
        ↓
[Observability: container logs → stdout, shipped to a log file/volume]
```

No CDN, load balancer, API gateway, cache, or message queue at MVP scale — traffic and data volume don't justify them (YAGNI). Angular build is served as static files by the same nginx reverse proxy.

## 3. Integration Patterns
| Integration | Pattern | Reason |
|---|---|---|
| CMI (payment) | REST (synchronous, redirect/callback) | Employer pays inline during job-post checkout; CMI callback confirms and activates the post |
| SMTP | Synchronous send at request time (fire-and-forget with retry-on-failure logging) | Email volume is low (registration, application, expiry); no queue needed yet |
| PostHog | REST (async fire-and-forget from backend) | Analytics must never block the request path |

## 4. Scalability Strategy
- Scaling approach: vertical (bigger container/host) — revisit only if sustained CPU/memory saturation is observed
- Cache strategy: none at MVP — introduce Redis only if search latency measurements exceed NFR-1
- Queue strategy: none — introduce (e.g., for email/analytics) only if synchronous calls start measurably slowing requests

## 5. System Design Decision Records

### SDR-1: Single Docker host, no Kubernetes
- **NFR Driver**: Availability 99%, throughput ~20 RPS peak
- **Decision**: Docker Compose running nginx + Spring Boot + Postgres on one host
- **Alternatives**: Kubernetes — rejected, operational overhead has no payoff at this scale
- **Re-evaluate when**: sustained traffic requires horizontal scaling, or multi-host HA becomes a real business requirement

### SDR-2: Monolith over microservices
- **NFR Driver**: Team size (small), throughput (low)
- **Decision**: One Spring Boot deployable covering candidate, employer, job, application, payment, and admin domains
- **Alternatives**: Service-per-domain — rejected, adds deployment/ops complexity with no scaling or team-boundary need yet
- **Re-evaluate when**: team grows enough to need independent deploy cadences, or one domain's load clearly outstrips the rest

### SDR-3: No message queue for email/analytics
- **NFR Driver**: Data volume (< 1GB/day), latency budget
- **Decision**: Synchronous/fire-and-forget calls to SMTP and PostHog from the request thread (analytics on a separate async thread so it never blocks the response)
- **Alternatives**: RabbitMQ/SQS-backed worker — rejected, adds infra for a volume that doesn't need decoupling
- **Re-evaluate when**: email or analytics calls measurably degrade p99 latency, or volume grows an order of magnitude

### SDR-4: Local volume storage for CVs, not object storage
- **NFR Driver**: Data volume (< 1GB/day), RTO/RPO targets
- **Decision**: CVs stored on a Docker volume, included in the nightly backup
- **Alternatives**: S3-compatible object storage — rejected as an added integration/cost with no current need
- **Re-evaluate when**: storage exceeds single-host disk comfortably, or multi-host deployment removes the shared-volume assumption
