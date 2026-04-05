# Agri Mortgage Production Runbook

This runbook matches the current production hardening in the codebase:
- optimistic locking on mortgage entities
- retry-aware encumbrance verification
- explicit `409 Conflict` handling for concurrent operator updates
- document-readiness visibility in the operator console
- mortgage servicing accounts with repayment posting and installment tracking
- scheduled overdue-aging sweep for unpaid installments
- database-backed district reporting
- ExternalSecret-backed runtime configuration and local-profile-gated bootstrap users

## Normal operating posture
- Use `admin` for platform administration.
- Use `officer` for borrower, land, and application operations.
- Use `reviewer` for document review and workflow progression.
- Keep the operator console open during encumbrance and review windows.

## Key surfaces
- Mortgage applications: `/api/v1/agri-mortgage-applications`
- Summary: `/api/v1/agri-mortgage-applications/summary`
- District reports: `/api/v1/agri-mortgage-applications/reports/district-summary`
- Excel export: `/api/v1/agri-mortgage-applications/export`
- Servicing account APIs: `/api/v1/agri-mortgage-applications/{applicationId}/loan-account` and `/api/v1/agri-mortgage-applications/loan-accounts`

## What to watch
- `409 Conflict` from concurrent parcel or application updates
- `GATEWAY_ERROR` states from encumbrance verification retries
- Missing document readiness blockers
- District summary latency
- Reviewer backlog and pending document counts
- Outstanding principal and overdue installment drift on disbursed mortgage accounts

## Encumbrance fallback handling
1. Check whether the encumbrance state is `GATEWAY_ERROR`.
2. Confirm whether the gateway dependency is still failing.
3. Retry after the dependency recovers.
4. Do not advance to credit review until encumbrance is `CLEAR`.

## Document readiness handling
1. Confirm the selected application has all required land/legal documents.
2. Verify the operator console shows the blocker explicitly.
3. Reload the case if another operator has changed document status.
4. Do not sanction while required documents are incomplete.

## Concurrent update handling
1. Treat `409 Conflict` as a sign of a stale UI or a second operator update.
2. Reload the latest application state before retrying.
3. Preserve the latest audit trail so the changed state is explainable.

## Mortgage servicing handling
1. Confirm the selected disbursed case has a servicing account number in the operator console.
2. Review outstanding principal, next due date, and installment status before posting a repayment.
3. Treat prepayment as principal curtailment and verify the recast schedule after posting the transaction.
4. Do not close the mortgage workflow while outstanding principal remains on the servicing account.

## Overdue aging handling
1. The servicing scheduler marks unpaid past-due installments as `OVERDUE` on the configured overnight sweep.
2. If an installment still appears `PENDING` after its due date, check the scheduler logs and the latest application refresh before taking manual action.
3. Do not manually flip installment status in the database to compensate for a missed sweep; fix the scheduler path and rerun the aging process.

## Incident checklist
- Preserve correlation IDs and request IDs before remediation.
- Do not bypass encumbrance or document blockers to satisfy a deadline.
- Keep district reporting database-backed; do not switch back to in-memory aggregation for recovery.
- Escalate if a verifier outage blocks a large volume of applications.

## Deployment posture
- Production expects connection settings and JWT secrets from the cluster secret store, not committed YAML values.
- The backend is deployed as a two-replica rolling update target.
- External access is expected through the ingress manifest at `k8s/06-ingress.yaml`, with `/` routed to the frontend service and `/api` routed to the backend service.
- Replace the placeholder host `agri-mortgage.example.com` and TLS secret `agri-mortgage-tls` with platform-owned DNS and certificate values before live deployment.
- Keep Actuator endpoints internal to the cluster unless the platform team explicitly exposes them through a protected operations ingress.
- The in-repo MySQL manifest is for local or integration use; production should point at a managed HA MySQL service.

## Local verification
- Backend: `mvn clean test`
- Frontend: `npm run build`
- Full stack: `docker compose up -d --build`

## Minikube smoke deployment
- Build unique images such as `agri-mortgage-loan-system:smoke-1` and
  `agri-mortgage-loan-system-frontend:smoke-1`.
- Load those images with `minikube image load`.
- Create the smoke secret before backend rollout and ensure
  `APP_SECURITY_JWT_SECRET` is Base64-encoded.
- Use the hardened in-repo MySQL manifest and allow it time to pass readiness.
- Set deployment images explicitly with `kubectl set image`.
- Scale the backend to one replica for smoke validation if cluster pressure makes
  the two-replica rollout unnecessary.
- Verify ingress from the ingress controller pod and expect:
  - `308 Permanent Redirect` on HTTP
  - `200 OK` plus frontend HTML on HTTPS
