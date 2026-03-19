# Agri Mortgage Loan System

Agri Mortgage Loan System is a Spring Boot backend for agricultural mortgage lending workflows. It focuses on borrower onboarding, land verification, eligibility checks, approval state tracking, and reporting.

## Tech Stack

- Java 17
- Spring Boot 3.2
- Spring Security
- Spring Data JPA
- MySQL
- Flyway
- JWT
- Docker
- Jenkins
- Kubernetes

## Core Features

- Agricultural borrower onboarding
- Co-borrower capture
- Land parcel intake
- Ownership and encumbrance validation
- Eligibility evaluation
- Manual approval state transitions
- Application search and filtering
- Summary dashboard

## API Overview

See [API_DOCS.md](./API_DOCS.md) for the endpoint reference.

## Run Locally

Backend:

```bash
cd backend
mvn clean test
mvn spring-boot:run
```

Default database name:

- `agri_mortgage_loan_system`

Ports:

- Backend: `8011`
- Frontend dev-server: `4400`

## Notes

- Authentication and access control are role-based.
- The project is designed to be interview-ready and easy to explain as a backend system.
