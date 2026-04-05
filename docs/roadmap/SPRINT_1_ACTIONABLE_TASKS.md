# Sprint 1 Actionable Tasks

## Objective

Align Agri Mortgage to the same production baseline standards being established in the Business Loan repo.

## Sprint 1 Priorities

### Config and Environment Alignment

- compare all Agri config keys with Business baseline
- classify variables into config, secret, and local-only buckets
- define staging and prod environment expectations

### Kubernetes Alignment

- add kustomize base and overlays matching the flagship approach
- ensure ingress host and allowed origin settings are environment-driven
- validate secret sourcing strategy

### CI/CD Alignment

- compare Jenkins stages with Business baseline
- add config and deployment guardrail checks
- define promotion path expectations

### Security Alignment

- identify any bootstrap or local-convenience auth assumptions
- document break-glass expectations
- ensure secure cookie and origin policies are explicit

### Documentation Alignment

- add environment model doc
- add configuration matrix
- add ADRs for environment and deployment strategy

## Expected Outcome

Agri Mortgage should end Sprint 1 with the same production-baseline operating model as Business, even if some implementation details are still catching up.
