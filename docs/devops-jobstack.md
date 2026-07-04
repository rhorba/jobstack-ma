# DevOps Foundation: JobStack.ma
**Architecture**: docs/architecture-jobstack.md
**Security**: docs/security-jobstack.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: DevOps/DevSecOps

**HANDOFF: Security Engineer → DevOps/DevSecOps**
Context: Security baseline approved — secrets via env vars, HTTPS/HSTS required, SCA/SAST scanning needed in CI, CV volume must be encrypted.
Need: CI pipeline, Docker Compose stack (nginx + Spring Boot + Postgres, per system design SDR-1), security scanning gates, monitoring baseline — Docker only, no Kubernetes (per user instruction, revisit only if a real scaling trigger appears).

## 1. Environment Strategy
| Environment | Purpose | Deploy Trigger |
|---|---|---|
| local | Development (docker-compose.yml, hot-reload) | Manual |
| staging | QA / preview, same Docker Compose stack on a second host or port set | PR merge to main |
| production | Live users | Manual tag / approved release |

## 2. CI Pipeline (GitHub Actions)
```yaml
name: ci
on: [push, pull_request]
permissions:
  contents: read
jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '25', distribution: 'temurin' }
      - name: Test (unit + integration via Testcontainers)
        run: ./mvnw verify -Pcoverage
      - name: Coverage gate (≥80%)
        run: ./mvnw jacoco:check
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '22' }
      - run: npm ci
      - run: npm run lint
      - run: npm test -- --code-coverage
      - run: npm run build
  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: returntocorp/semgrep-action@v1
        with: { config: p/owasp-top-ten }
      - uses: aquasecurity/trivy-action@master
        with: { scan-type: fs, severity: CRITICAL,HIGH, exit-code: 1 }
      - uses: gitleaks/gitleaks-action@v2
  build:
    needs: [backend, frontend, security]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: docker build -t jobstack-api ./backend
      - run: docker build -t jobstack-web ./frontend
      - uses: aquasecurity/trivy-action@master
        with: { scan-type: image, image-ref: jobstack-api, severity: CRITICAL,HIGH, exit-code: 1 }
```

## 3. Infrastructure
- **Hosting**: single VPS/Docker host (per system design)
- **Compute**: Docker Compose — 3 services: `nginx` (reverse proxy + static Angular build), `api` (Spring Boot), `db` (Postgres)
- **Database**: self-hosted Postgres container, encrypted volume, nightly `pg_dump`
- **Secrets**: `.env` file (gitignored) on the host, injected via `env_file:` in Compose; never baked into images
- **Monitoring**: container stdout logs → shipped to a log volume; revisit dedicated tooling only if incident response needs it

## 4. Security Scanning Gates
| Scanner | Scan Type | Fail Threshold |
|---|---|---|
| Semgrep | SAST — code vulnerabilities | Critical findings |
| Trivy | SCA — dependency CVEs + container image scan | Critical/High CVEs |
| Gitleaks | Secrets detection | Any secrets found |

## 5. Docker Setup

### backend/Dockerfile
```dockerfile
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:25-jre
RUN groupadd -r app && useradd -r -g app app
WORKDIR /app
COPY --from=builder --chown=app:app /app/target/*.jar app.jar
USER app
HEALTHCHECK --interval=30s CMD curl -f http://localhost:8080/actuator/health || exit 1
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

### frontend/Dockerfile
```dockerfile
FROM node:22-slim AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=builder /app/dist/jobstack-web /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

### docker-compose.yml (production shape)
```yaml
services:
  nginx:
    build: ./frontend
    ports: ["443:443", "80:80"]
    depends_on: [api]
  api:
    build: ./backend
    env_file: .env
    depends_on:
      db: { condition: service_healthy }
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: jobstack
      POSTGRES_USER: jobstack
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes: ["pgdata:/var/lib/postgresql/data"]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U jobstack"]
      interval: 5s
volumes:
  pgdata:
```

## 6. Monitoring Baseline
| Signal | Tool | Alert Threshold |
|---|---|---|
| Logs | Container stdout → log volume | Error rate spike (manual review at MVP stage) |
| Health | Spring Boot Actuator `/actuator/health` | Health check failures → container restart |
| Uptime | Manual/external uptime ping (e.g., a free uptime monitor) | Downtime detected |

Full metrics/alerting stack (Prometheus/Grafana) deferred — YAGNI at single-host MVP scale; revisit if incidents go undetected too long or traffic grows enough to need real dashboards.

## CI Monitoring Protocol
Per CLAUDE.md rule 11: after every push, watch `gh run watch` to completion. Green → log to `.logs/activity.md`. Red → diagnose, fix, push again, log each iteration to `.logs/issues.md`. No SHIP phase begins on red CI.
