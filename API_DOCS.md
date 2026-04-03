# Agri Mortgage API Documentation

Base path: `/api/v1/agri-mortgage-applications`

Authentication base path: `/auth`

## Authentication

- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /auth/me`

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
| `GET` | `/{applicationId}/documents` | List land/legal document metadata for an application |
| `POST` | `/{applicationId}/documents` | Upload document metadata for a case |
| `PATCH` | `/{applicationId}/documents/{documentId}/status` | Review or reject a document |
| `POST` | `/{applicationId}/encumbrance-check` | Run persisted encumbrance verification using the retry wrapper |
| `POST` | `/{applicationId}/evaluate` | Run eligibility evaluation |
| `POST` | `/{applicationId}/status` | Advance the workflow status |
| `GET` | `/` | Search applications with filters and pagination |
| `GET` | `/summary` | Fetch dashboard-level aggregate metrics |
| `GET` | `/reports/district-summary` | Fetch database-aggregated district reporting data |
| `GET` | `/export` | Download the Excel application register |

## Concurrency

The mortgage workflow uses optimistic locking on application, land parcel, document, and applicant rows. If two operators update the same record concurrently, the backend returns `409 Conflict`. Reload the latest application state before retrying the action. The operator console surfaces this as an explicit conflict notice instead of a generic failure.

## Create Draft

`POST /api/v1/agri-mortgage-applications`

Creates a draft application with:

- Primary applicant details
- Optional co-borrowers
- Agricultural land parcels
- Requested loan amount and tenure

## Documents

`GET /api/v1/agri-mortgage-applications/{applicationId}/documents`

Returns the document list for the application, including:

- document type
- document status
- uploaded by / reviewed by metadata
- file name / file reference
- remarks and timestamps

`POST /api/v1/agri-mortgage-applications/{applicationId}/documents`

Adds document metadata for required land/legal proofs such as:

- `PATTADAR_PASSBOOK`
- `OWNERSHIP_PROOF`
- `ENCUMBRANCE_CERTIFICATE`
- `LAND_VALUATION_REPORT`

`PATCH /api/v1/agri-mortgage-applications/{applicationId}/documents/{documentId}/status`

Updates document review status to values such as:

- `VERIFIED`
- `REJECTED`
- `UPLOADED`

## Encumbrance Check

`POST /api/v1/agri-mortgage-applications/{applicationId}/encumbrance-check`

Runs encumbrance verification through the retry-aware gateway wrapper and persists:

- aggregate verification status
- verification summary
- parcel-level gateway availability
- parcel-level encumbrance check details
- verification timestamps

If the external registry keeps failing after retries, the response is returned with `encumbranceVerificationStatus = GATEWAY_ERROR`. That is a retryable fallback state, not a final decision. The operator console highlights it so the reviewer knows to retry once the dependency recovers.

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

Important workflow guards:

- `CREDIT_REVIEW` requires `encumbranceVerificationStatus = CLEAR`
- `SANCTIONED` requires required land/legal documents to be verified
- `SANCTIONED` also requires the application to be eligible

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

## District Summary

`GET /api/v1/agri-mortgage-applications/reports/district-summary`

Returns district-level reporting rows computed in the database using `GROUP BY district` rather than by loading every application into memory. The query is backed by the district index created in the schema migration, so the reporting path stays practical for large case volumes.

## Response Shape

The main application response includes:

- application metadata
- primary applicant details
- co-borrowers
- land parcels
- encumbrance verification status, summary, and timestamp
- parcel-level encumbrance check details
- document list
- document completeness summary
- state history
- eligibility summary and computed ratios

Document readiness is part of the workflow contract: required document completeness is visible in the dashboard summary and on the selected-application panel, so sanction blockers are obvious before an operator attempts the transition.

Operational note:

- correlation IDs and structured logs should be used together when investigating retryable encumbrance fallback or `409 Conflict` updates
- the production runbook in `RUNBOOK.md` captures the recovery path for stale encumbrance states, blocked document readiness, and concurrent operator updates
- production Kubernetes deployments expect datasource credentials, JWT secrets, and connection settings to arrive through External Secrets rather than committed manifest values
