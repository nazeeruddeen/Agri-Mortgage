# Agri Mortgage Loan System

Agricultural mortgage lending platform focused on borrower intake, land parcel capture, encumbrance-sensitive eligibility evaluation, mortgage workflow progression, district-level reporting, and secured operator access.

## Project Story

This repo is the domain-heavy mortgage application in the startup lending platform.

It currently includes:
- agricultural borrower onboarding with co-borrowers
- land parcel capture with district, taluka, village, and appraisal inputs
- document metadata and review workflow for land and legal readiness
- persisted encumbrance verification routed through retry-aware gateway integration
- encumbrance and ownership-sensitive mortgage evaluation
- optimistic locking on core mortgage entities so concurrent document, parcel, and status updates return a safe `409 Conflict` instead of silently overwriting
- operator-facing dashboard KPIs for document backlog, encumbrance readiness, and district concentration
- explicit operator-console feedback for retryable encumbrance fallback and document-readiness blockers
- database-backed district reporting instead of in-memory grouping for large operator datasets
- workflow progression across verification, review, sanction, disbursement, and closure states
- mortgage servicing after disbursement, including loan-account materialization, EMI schedule generation, repayment posting, and closure blocking while outstanding principal remains
- scheduled overdue aging so unpaid installments move to `OVERDUE` even when no repayment event occurs that day
- paginated search, dashboard summary, district summary, and Excel export
- secured Spring Boot APIs with JWT-backed Angular access
- operational runbook and recovery notes in `RUNBOOK.md`

## Tech Stack

- Java 21
- Spring Boot 3.2
- Spring Data JPA / Hibernate
- MySQL
- Spring Security + JWT
- Flyway
- Apache POI
- Angular
- Docker
- Jenkins
- Kubernetes

## Local bootstrap access

The backend only seeds bootstrap users when all of the following are true:

- the `local` Spring profile is active
- `APP_SECURITY_BOOTSTRAP_USERS_ENABLED=true`
- bootstrap passwords are supplied through environment variables

Usernames default to `admin`, `officer`, `reviewer`, and `borrower`, but passwords are no longer committed in the repository.

## Ports

- Backend API: `http://localhost:8011`
- Swagger UI: `http://localhost:8011/swagger-ui.html`
- Frontend dev server: `http://localhost:4400`

## Run Locally

Backend:

```bash
cd backend
mvn clean test
mvn spring-boot:run
```

Provide datasource credentials, JWT secret, and any local bootstrap passwords through environment variables before using direct backend startup.

Frontend:

```bash
cd frontend
npm install
npm start
```

To run the full local stack with bootstrap users, create a local `.env` from `.env.example` and then start:

```bash
docker compose up -d --build
```

## Main Workflow

1. Sign in with a provisioned account or a local bootstrap user.
2. Create a draft agri mortgage application with co-borrowers and land parcels.
3. Search and select the application.
4. Upload and review land/legal document metadata for the selected case.
5. Run encumbrance verification and inspect parcel-level gateway results. If the gateway exhausts retries, the application stays in a retryable fallback state instead of silently failing.
6. Run eligibility evaluation.
7. Advance the application through the configured mortgage workflow. Credit review now requires clear encumbrance verification, and sanction requires required documents to be verified.
8. Disburse the sanctioned case to materialize the mortgage servicing account and repayment schedule.
9. Review dashboard backlog KPIs, readiness counts, district summary updates, and servicing-account state. The district summary is computed in SQL so it stays responsive as the application register grows.
10. Post repayments against the servicing account and monitor installment status, outstanding principal, and prepayment effects.
11. Export the application register to Excel when needed.
12. If two operators touch the same application concurrently, expect a `409 Conflict` and reload the latest application before retrying. The operator console now surfaces this as an explicit conflict state instead of a generic failure.

## Production deployment posture

- backend pods run as a rolling two-replica deployment in Kubernetes
- application secrets and connection settings are expected to come from an External Secrets store, not committed manifest values
- the in-repo MySQL manifest is for integration environments only; production should use a managed HA database endpoint
