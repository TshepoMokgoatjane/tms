# 🚀 Tenant Management System (TMS)
```azure

A **modern full-stack, cloud-ready** tenant management application designed to streamline property operations, tenant communication, payments, and maintenance workflows - all built with **Spring Boot 3, React, MySQL/PostgreSQL, AWS**, and industry best practis.

## Overview
The **Tenant Management System (TMS)** eliminates manual spreadsheets and fragmented tracking processes. It provides landlords with a central digital workspace to manage tenants, leases, payments, communications, maintenance logs, and daily operational tasks.

This system is engineered with **clean architecture principles**, enterprise-grade patterns, and production-ready configurations - reflecting real-world backend engineering and solution architecture skills.

## Core Objectives
- Digitize tenant & lease management
- Automate rent reminders, tenant communications, and payment tracking
- Replace spreadsheets with a secure, scalable platform
- Provide landlords with operational visibility & reporting
- Enable effortless onboarding, record keeping, and workflow automation

## System Architecture
Below is a clean, professional architecture diagram representing my backend and its integrations:
```azure
## 🏛️ System Architecture

The high-level system architecture for the Tenant Management System is illustrated below:

```
                           ┌────────────────────────────┐
                           │        React Frontend      │
                           │  Tenant / Admin Dashboards │
                           └──────────────┬─────────────┘
                                          │ HTTPS (REST)
                                          ▼
                        ┌──────────────────────────────────────┐
                        │        Spring Boot Backend API       │
                        │ (Controllers • Services • Repos)     │
                        └───────────────┬───────────────┬──────┘
                                        │               │
                           ┌────────────┘               └───────────────┐
                           ▼                                            ▼
                ┌──────────────────────┐                   ┌────────────────────────┐
                │  Authentication      │                   │     Notification       │
                │  JWT + Google OAuth2 │                   │ Email (SMTP) + SMS     │
                └──────────────────────┘                   │ (Gmail + Twilio APIs)  │
                                                           └─────────────┬──────────┘
                                                                         │
                                                                         ▼
                                                           ┌────────────────────────┐
                                                           │  Scheduler (Quartz-like│
                                                           │  Rent Reminders + Auto │
                                                           │  Payment Creation      │
                                                           └─────────────┬──────────┘
                                                                         │
                                                                         ▼
                                        ┌────────────────────────────────────────────────┐
                                        │                 Data Layer                     │
                                        │ Spring Data JPA • Hibernate ORM                │
                                        ├────────────────────────────────────────────────┤
                                        │ MySQL / PostgreSQL (Production)                │
                                        │ H2 (Testing)                                   │
                                        └────────────────────────────────────────────────┘

                     ┌─────────────────────────────────────────────────────────────────────────┐
                     │                          AWS Deployment Stack                           │
                     │ Elastic Beanstalk • RDS • Parameter Store • Secrets Manager • S3 (opt.) │
                     └─────────────────────────────────────────────────────────────────────────┘
```
```

The Architecture shows mastery of:
- Modular REST design
- Clean layered separation
- Event-driven scheduling
- External integrations (Twilio, OAuth2, Email)
- Cloud-native deployment
- Mature error handling & testing strategy



## 🔄 Payment Processing — Sequence Diagram (Mermaid)

```mermaid
sequenceDiagram
    autonumber

    participant U as User / Frontend (React)
    participant C as PaymentController
    participant S as PaymentService
    participant TR as TenantRepository
    participant PR as PaymentRepository
    participant N as NotificationService (Email/SMS)
    participant SCH as Scheduler

    rect rgb(240, 252, 255)
    Note over U,C: Manual Payment Flow (User initiates a payment)
    U->>C: POST /api/payments (amount, method, tenantId)
    C->>S: validateRequest(paymentDTO)
    S->>TR: findTenantById(tenantId)
    TR-->>S: Tenant
    S->>PR: save(Payment[PENDING or PAID/FAILED])
    PR-->>S: Payment Saved
    S->>N: sendPaymentConfirmation(tenant, payment)
    N-->>S: Notification Sent
    S-->>C: PaymentResponse (status, id, timestamps)
    C-->>U: 200 OK (Payment Recorded)
    end

    rect rgb(245, 245, 245)
    Note over SCH,S: Automated Flow (Daily reminders & auto PENDING)
    SCH->>S: createPendingPaymentForDueTenants()
    S->>TR: getTenantsWithRentDueToday()
    TR-->>S: Tenants[]
    loop For each tenant due today
        S->>PR: findByTenantIdAndPaymentDateBetween(tId, startOfDay, endOfDay)
        PR-->>S: List<Payment> (possibly empty)
        alt no existing payment today
            S->>PR: save(Payment[PENDING])
            PR-->>S: Payment Saved
            S->>N: sendRentReminder(tenant)
            N-->>S: Reminder Sent
        else existing payment found
            S->>S: skip creation (idempotent)
        end
    end
    end
 ```




## Architecture & Technology Stack:
The system is built with a modern, cloud-ready architecture featuring:

### Backend
- **Java 17+**
- **Spring Framework 6** 
- **Spring Boot 3** (Web, Security, Validation, Actuator)
- **Spring Data JPA + Hibernate**
- **JWT Authentication**
- **OAuth2 with Google Login**
- **Twilio SMS Integration**
- **Email Notification - Jakarta Mail**
- **Schedular for rent reminders & recurring tasks**
- **RESTful layered architecture** (Controller → Service → Repository)

### Database
- **MySQL** (Production)
- **PostgreSQL** (Alternative configuration)
- **H2** (Unit Tests)

### Frontend
- **React** (SPA for tenant & admin dashboards)
- Responsive UI built with **Bootstrap**
- Axios (To connect frontend and backend RESTful calls)


### DevOps / Cloud
- **AWS Elastic Beanstalk** (Deployment)
- **AWS RDS (MySQL)**
- **Docker containerization**
- **CI-friendly Maven configuration

### Testing
- **JUnit5**
- **Mockito**
- **Spring Boot Test**
- **MockMvc**
- Repository integration tests
- **100%** coverage for key modules (auth, payments, and tenants)

### Tools
- **Maven**
- **Lombok**
- **Swagger / Springdoc OpenAPI UI**
- **SLF4J / Logback**

## Key Features
### Tenant Management
- Full CRUD for tenant profiles
- Lease agreement with validation & auto rules
- Automatic computation of lease periods
- Tenant behaviour tracking (GOOD / FAIR / BAD) and scoring

### Payment Management
- Capture EFT/CASH rent and deposit payments
- Automated rent-due SMS & email reminders
- Payment status updates (PENDING, PAID, FAILED)
- Date-range filtering for reports
- Dashboard view for payment history

### Maintenance & Operations
- Create & track maintenance tickets
- Log job cards
- Track issue resolution and timeline

### Automation
- Daily rent reminder scheduler
- Auto-creation of PENDING payments for due tenants
- Background email + SMS notifications

### Security
- JWT Authentication
- Google OAuth2 login
- Role-based access (ADMIN / USER)
- Enforced secure headers & session policies

## REST API Documentation
The project includes auto-generated Swagger documentation:
```azure
http://localhost:5000/swagger-ui.html
```
API modules include:
- **Authentication APIs**
- **Tenant APIs**
- **Payment APIs**
- **Maintenance APIs**
- **Admin configuration APIs**

The **Swagger UI** is powered by **springdoc-openapi-starter-webmvc-ui**.



## Testing & Quality
The project demonstrates engineering maturity through:
- JUnit 5 test suites
- Mockito mocks for service/business logic
- MockMvc controller tests
- Repository tests with H2 
- 100% coverage for key modules
- GlobalExceptionHandler with clean 400/404/500 mapping
- UTF-8 & parallel test execution via Maven Surefire

## Deployment
The backend is optimized for cloud deployment:

### AWS Elastic Beanstalk
- Packaged as a Docker or JAR deployment
- Environment variables for DB, Twilio, email, JWT secrets
- Externalized configuration for portability.

### AWS RDS
- Production-grade MySQL with failover support

## Installation & Setup
### Prerequisites
- Java 17+
- Maven 3.9+
- Docker (optional)
- MySQL/PostgreSQL
- Node.js (for React UI)

## Deployment (AWS)
- Build with Maven
- Deploy via Docker container or JAR
- Elastic Beanstalk hosting
- RDS for MySQL
- Secrets via AWS Parameter Store

## Local Setup
Clone:
```azure
git clone https://github.com/<your-user>/tms.git
cd tms
```
Run backend:
```azure
mvn spring-boot:run
```

Run frontend:
```azure
cd frontend
npm install
npm start
```

## Environment Variable
```azure
# Database
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=

# Security
JWT_SECRET=

# Authentication
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

# SMS / Email
TWILIO_ACCOUNT_SID=
TWILIO_AUTH_TOKEN=
EMAIL_PASSWORD=
```

## Project Structure
```azure
src/
├── main/java/za/co/tms
│    ├── controller
│    ├── service
│    ├── repository
│    ├── scheduler
│    ├── exception
│    ├── model
│    └── config
├── test/java/za/co/tms
│    ├── controller
│    ├── service
│    └── repository
└── resources
├── application.properties
├── static/
└── templates/
```

## Code Highlights
- Clean separation of concerns
- Robust global exception handling with 400-level guards
- DTO validation with meaningful messages
- Reusable service components
- Data access abstraction via JPA
- Transaction-safe payment updates

### Inspiration & Learning Source

Built on world-class engineering principles from:

- Robert C. Martin (Uncle Bob)
- Martin Fowler
- Rod Johnson
- Ranga Karanam (in28Minutes)
- John Thompson
- Chad Darby

These educators shaped the architecture and coding style used in this system.

## Author
**Tshepo Mokgoatjane**
Java Developer | Aspiring Solution Architect
Passionate about resilient backend systems, cloud-native design, and enterprise architecture.

## License
- Open license - free for personal and demonstration use.