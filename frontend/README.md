# Agri Mortgage Frontend

Angular operator console for the Agri Mortgage Loan System backend.

## Dev Server

- Port: `4400`
- API base URL: `http://localhost:8011/api/v1/agri-mortgage-applications`

## Run

```bash
npm install
npm start
```

## What the UI Covers

- JWT-backed sign-in against the Spring Boot backend
- agri mortgage draft creation
- co-borrower and land parcel capture
- operator dashboard KPIs for document backlog and encumbrance readiness
- paginated application search
- land/legal document upload metadata and reviewer status changes
- persisted encumbrance verification with parcel-level outcome visibility
- eligibility evaluation
- workflow status advancement
- dashboard summary and district-level summary visibility
- Excel export for the agri mortgage register

## Seeded Users

- `admin / Admin@123`
- `officer / Officer@123`
- `reviewer / Reviewer@123`
