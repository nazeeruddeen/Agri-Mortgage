# Agri Mortgage Production Alignment

Product role
- domain-heavy agricultural mortgage workflow system in the workspace

Production hardening now owned by this repo
- frontend is live against the backend, not local-only state
- actuator health endpoints are probe-safe
- nginx runtime configuration is production-safe
- land/legal document readiness is part of workflow control
- encumbrance verification is persisted and retry-aware
- optimistic locking protects concurrent operator updates
- MySQL probe timing and backend startup timing are hardened for Kubernetes
- ingress manifest is in place for `/` and `/api`
- backend, frontend, MySQL, and ingress smoke validation were completed
  successfully in Minikube

Current repo-owned priorities
1. Keep the mortgage workflow, document readiness, and encumbrance story stable.
2. Preserve the conflict-safe update path and database-backed reporting path.
3. Add only focused reviewer backlog or reporting improvements if they deepen the
   operator story without weakening runtime clarity.

Smoke validation status
- backend rollout: passed
- frontend rollout: passed
- mysql rollout: passed
- ingress verification: passed

Best demo flow
1. Create or inspect an application with parcel and document context.
2. Show document readiness and encumbrance state as workflow gates.
3. Advance through review-ready stages only when domain conditions are satisfied.
4. Show district summary or export as the reporting proof point.
5. If needed, discuss optimistic locking and explicit `409 Conflict` handling for
   concurrent operator updates.
