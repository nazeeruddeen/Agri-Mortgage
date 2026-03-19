# Agri Mortgage Loan System

A domain-heavy rule engine for agricultural mortgage lending. This system focuses on complex business logic, third-party integrations, and state machine transitions, moving beyond simple CRUD operations to solve real-world enterprise domain problems.

## 🎯 Core Interview Story: Domain-Heavy Rule Engine
This project serves as a "Domain Depth" resume story. It demonstrates the ability to model complex real-world workflows, integrate with unreliable external systems, and construct defensible, deterministic approval rules.

### Key Architectural Decisions & Features
*   **External Gateway Integration & Resilience:** Designed an `EncumbranceGatewayClient` interface using the Dependency Inversion Principle to decouple domain logic from third-party revenue APIs.
    *   Implemented an explicit exponential backoff retry wrapper (`EncumbranceGatewayRetryWrapper`) that purposefully retries *only* on network errors (not on definitive business failures), demonstrating an understanding of system resilience over blind `@Retryable` annotations.
*   **Rule Versioning via Snapshots:** Captured `eligibility_rules_snapshot` as a JSON column at the exact time of application submission. If LTV caps or income thresholds change globally during the review process, the application is still evaluated against the snapshot—guaranteeing deterministic outcomes for borrowers.
*   **Java 17 Records:** Extensively used Java 17 Records for Gateway DTOs and Reporting Summaries to ensure immutability and reduce boilerplate.
*   **Advanced Reporting:** Implemented custom Apache POI Excel export services (with streaming `byte[]` controller endpoints) and DB-level `GROUP BY` aggregations for district-level summaries, demonstrating knowing *when* to use Java Streams vs DB aggregations.

## 🛠 Tech Stack
*   **Java 17** & **Spring Boot 3.2**
*   **Spring Data JPA** (MySQL with JSON column support)
*   **Flyway** (Database migrations)
*   **Apache POI** (Excel Report Generation)
*   **Swagger/OpenAPI** & **Actuator**

## 🚀 Run Locally

**Backend:**
```bash
cd backend
mvn clean test
mvn spring-boot:run
```

**Ports:**
*   API / Swagger UI: `http://localhost:8011/swagger-ui.html`
*   Actuator: `http://localhost:8011/actuator`
