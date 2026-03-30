# Agri Mortgage Loan System

Agricultural mortgage lending platform focused on borrower intake, land parcel capture, encumbrance-sensitive eligibility evaluation, mortgage workflow progression, district-level reporting, and secured operator access.

## Project Story

This project is the domain-heavy lending application in the portfolio.

It demonstrates:
- agricultural borrower onboarding with co-borrowers
- land parcel capture with district, taluka, village, and appraisal inputs
- document metadata and review workflow for land and legal readiness
- persisted encumbrance verification routed through retry-aware gateway integration
- encumbrance and ownership-sensitive mortgage evaluation
- operator-facing dashboard KPIs for document backlog, encumbrance readiness, and district concentration
- workflow progression across verification, review, sanction, disbursement, and closure states
- paginated search, dashboard summary, district summary, and Excel export
- secured Spring Boot APIs with JWT-backed Angular access

## Tech Stack

- Java 17
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

## Default Users

- `admin / Admin@123`
- `officer / Officer@123`
- `reviewer / Reviewer@123`
- `borrower / Borrower@123`

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

Frontend:

```bash
cd frontend
npm install
npm start
```

## Main Workflow

1. Sign in with a seeded user.
2. Create a draft agri mortgage application with co-borrowers and land parcels.
3. Search and select the application.
4. Upload and review land/legal document metadata for the selected case.
5. Run encumbrance verification and inspect parcel-level gateway results.
6. Run eligibility evaluation.
7. Advance the application through the configured mortgage workflow. Credit review now requires clear encumbrance verification, and sanction requires required documents to be verified.
8. Review dashboard backlog KPIs, readiness counts, and district summary updates.
9. Export the application register to Excel when needed.
