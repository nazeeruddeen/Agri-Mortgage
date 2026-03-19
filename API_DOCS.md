# API Documentation

Base path: `/api/v1/agri-mortgage-applications`

## Access Control

Roles used by the project:

- `ADMIN`
- `LOAN_OFFICER`
- `REVIEWER`

## Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/` | Create a draft mortgage application |
| `GET` | `/{applicationId}` | Fetch a single application |
| `POST` | `/{applicationId}/evaluate` | Run eligibility evaluation |
| `POST` | `/{applicationId}/status` | Advance the workflow status |
| `GET` | `/` | Search applications with filters and pagination |
| `GET` | `/summary` | Fetch dashboard-level aggregate metrics |

## Create Draft

`POST /api/v1/agri-mortgage-applications`

Creates a draft application with:

- Primary applicant details
- Optional co-borrowers
- Agricultural land parcels
- Requested loan amount and tenure

## Evaluate

`POST /api/v1/agri-mortgage-applications/{applicationId}/evaluate`

Runs rule-based checks such as:

- Land availability
- Ownership validation
- Encumbrance validation
- LTV threshold
- Affordability based on income

## Advance Status

`POST /api/v1/agri-mortgage-applications/{applicationId}/status`

Valid target statuses:

- `LAND_VERIFICATION`
- `ENCUMBRANCE_CHECK`
- `CREDIT_REVIEW`
- `LEGAL_REVIEW`
- `SANCTIONED`
- `DISBURSED`
- `CLOSED`
- `REJECTED`

## Search

`GET /api/v1/agri-mortgage-applications`

Query parameters:

- `district`
- `taluka`
- `status`
- `minAmount`
- standard Spring pagination parameters like `page`, `size`, and `sort`

## Summary

`GET /api/v1/agri-mortgage-applications/summary`

Returns:

- total applications
- eligible applications
- counts by status
- total requested amount
- average requested amount
- total land parcels
- total appraised value

## Response Shape

The main application response includes:

- application metadata
- primary applicant details
- co-borrowers
- land parcels
- state history
- eligibility summary and computed ratios
