# Track My Case Service

A Spring Boot backend service for the Track-a-Case (TAC) application, built for the Ministry of Justice (MOJ).
This backend service provides the required data for the Track-a-Case (TAC) UI.
The TAC UI invokes APIs exposed by this service, which in turn communicate with the API Marketplace to retrieve hearing and court details from the Common Platform.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Running Tests](#running-tests)
- [Configuration](#configuration)
- [Code Quality](#code-quality)
- [CI/CD](#cicd)
- [License](#license)

---

## Architecture Overview

```
TAC UI
  │
  ▼
Track My Case Service  (this service, port 9999)
  │
  ├── User Management  ──►  PostgreSQL (Flyway-managed schema)
  │
  └── Case / Court Data ──►  API Marketplace  ──►  Common Platform
                              (OAuth2-secured)
```

The service acts as an aggregation layer: it fetches court schedule, prosecution case, and court house data from three separate downstream APIs, merges them into a single case-details response, and returns it to the UI.
Sensitive fields (e.g. email addresses) are encrypted with AES/HMAC before storage and decrypted on retrieval.

---

## Tech Stack

| Layer            | Technology                                                           |
| ---------------- | -------------------------------------------------------------------- |
| Language         | Java 25                                                              |
| Framework        | Spring Boot 4.0.6                                                    |
| Build            | Gradle 8 (wrapper included)                                          |
| Database         | PostgreSQL (H2 for tests)                                            |
| Migrations       | Flyway                                                               |
| ORM              | Hibernate / Spring Data JPA                                          |
| API Docs         | SpringDoc OpenAPI 3 (Swagger UI)                                     |
| Security         | AWS Secrets Manager, AES/HMAC encryption, OAuth2                     |
| Monitoring       | Micrometer / Prometheus, Azure Application Insights, Spring Actuator |
| Logging          | SLF4J + Logback, HMCTS Java Logging                                  |
| Containerisation | Docker (Amazon Corretto 25 builder → Eclipse Temurin 25 JRE runtime) |
| Deployment       | Kubernetes via Helm, AWS ECR                                         |

---

## Prerequisites

- **Java 25** (JDK; Gradle toolchain configured automatically)
- **Gradle 8** – use the included wrapper (`./gradlew`)
- **Docker & Docker Compose** – for local containerised runs
- **PostgreSQL 12+** – if running outside Docker

---

## Getting Started

### 1. Clone the repository

```bash
git clone git@github.com:ministryofjustice/stg-track-my-case-service.git
cd stg-track-my-case-service
```

### 2. Configure environment variables

Copy the example environment file and populate the required values:
Key variables include the PostgreSQL connection details, AWS credentials (for Secrets Manager), and OAuth2 client configuration for the API Marketplace.

To build the project execute the following command:

```bash
  ./gradlew clean build
```

### 3. Run with Docker Compose

The quickest way to start the service and a local PostgreSQL instance together:

```bash
docker-compose up -d
```

The service will be available at `http://localhost:4550` (mapped from container port 9999).

### 4. Build and run locally (without Docker)

```bash
./gradlew bootRun
```

---

## Running Tests

| Command                      | Description                                     |
| ---------------------------- | ----------------------------------------------- |
| `./gradlew test`             | Unit tests                                      |
| `./gradlew integration`      | Integration tests (Spring context, H2 database) |
| `./gradlew functional`       | Functional / end-to-end API tests               |
| `./gradlew test integration` | Unit + integration together                     |

Code coverage is measured by JaCoCo and reported to SonarQube in CI.

---

### Case Details

| Method | Path                                | Description                                                                             |
| ------ | ----------------------------------- | --------------------------------------------------------------------------------------- |
| `GET`  | `/api/cases/{case_urn}/casedetails` | Retrieve full case details (court schedule, court house, case status) for the given URN |

### User Management

| Method   | Path                       | Description                      |
| -------- | -------------------------- | -------------------------------- |
| `GET`    | `/api/users`               | List all users                   |
| `GET`    | `/api/users?email={email}` | Find user by email address       |
| `POST`   | `/api/users/create`        | Create one or more user accounts |
| `PUT`    | `/api/users/edit`          | Update an existing user          |
| `DELETE` | `/api/users/delete`        | Delete a user                    |

### System

| Method | Path                   | Description                                                           |
| ------ | ---------------------- | --------------------------------------------------------------------- |
| `GET`  | `/`                    | Welcome / liveness check                                              |
| `GET`  | `/api/health`          | Application health status                                             |
| `GET`  | `/api/active-user`     | Returns the currently active user context                             |
| `GET`  | `/actuator/health`     | Spring Actuator health (used by Kubernetes liveness/readiness probes) |
| `GET`  | `/actuator/prometheus` | Prometheus metrics scrape endpoint                                    |

---

## Configuration

Main configuration is in `src/main/resources/application.yaml`. The following properties are typically overridden via environment variables or AWS Secrets Manager at runtime:

| Property              | Description                                                      |
| --------------------- | ---------------------------------------------------------------- |
| `server.port`         | Application port (default `9999`)                                |
| `spring.datasource.*` | PostgreSQL connection URL, username, password                    |
| `api.rcc.*`           | Referencedata API endpoint and credentials                       |
| `api.slc.*`           | Schedule Listing Court API endpoint and credentials              |
| `api.pcd.*`           | Prosecution Case Details API endpoint and credentials            |
| `oauth2.*`            | OAuth2 token endpoint and client credentials for downstream APIs |
| `encryption.*`        | AES/HMAC key configuration for PII encryption                    |

Secrets are resolved from AWS Secrets Manager in deployed environments.
For local development, values can be supplied via the `.env` file

---

## Code Quality

The following checks run as part of the standard build:

```bash
# Checkstyle (zero warnings enforced)
./gradlew checkstyleMain checkstyleIntegrationTest

# Spotless code formatting
./gradlew spotlessCheck      # verify
./gradlew spotlessApply      # auto-fix

# OWASP dependency vulnerability scan
./gradlew dependencyCheckAggregate

# SonarQube analysis (requires SONAR_HOST_URL and SONAR_TOKEN)
./gradlew sonarqube
```

---

## CI/CD

GitHub Actions workflows are defined in `.github/workflows/`. Each target environment has its own pipeline:

| Workflow                        | Trigger                                     | Environment    |
| ------------------------------- | ------------------------------------------- | -------------- |
| `build-push-deploy-dev.yml`     | Push to `develop` / `main`, manual dispatch | Development    |
| `build-push-deploy-test.yml`    | Manual dispatch                             | Test           |
| `build-push-deploy-sit.yml`     | Manual dispatch                             | SIT            |
| `build-push-deploy-preprod.yml` | Manual dispatch                             | Pre-production |
| `build-push-deploy-prod.yml`    | Manual dispatch                             | Production     |

Each deployment pipeline:

1. Builds and pushes a Docker image to AWS ECR (tagged with auto-incremented patch version)
2. Deploys the image to Kubernetes via Helm
3. Deploys the monitoring Helm chart

Security scanning runs independently on every push:

| Workflow                              | Tool                       |
| ------------------------------------- | -------------------------- |
| `security_codeql_actions_scan.yml`    | GitHub CodeQL              |
| `security_veracode_pipeline_scan.yml` | Veracode SAST              |
| `security_trivy.yml`                  | Trivy container image scan |

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
