# Bookly - Multi-tenant Library Management System

![Status](https://img.shields.io/badge/Status-Work%20In%20Progress-yellow)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-green)
![License](https://img.shields.io/badge/License-MIT-blue)

## 🚧 Work In Progress 🚧

Bookly is a multi-tenant library management system built with Spring Boot, designed to support multiple libraries on a single platform. Each library operates in its own isolated tenant environment with dedicated data storage through schema-based isolation.

## 🏗️ Architecture Overview

The system implements a schema-based multi-tenancy pattern:

- Each tenant (library) has a dedicated PostgreSQL schema
- `TenantContext` maintains the current tenant ID in a ThreadLocal variable
- `HttpHeaderTenantResolver` extracts tenant information from HTTP headers
- `ConnectionProvider` manages database connections with the correct schema
- Tenant isolation ensures data security between different libraries

## 🔑 Key Features

- **Schema-based Multi-tenancy**: Complete tenant isolation for data security
- **JWT Authentication**: Secure authentication with ECDSA-signed JWTs
- **Email Verification**: OTP-based email verification during registration
- **Role-based Access Control**: Library administrators and employees have different permissions
- **Employee Invitation System**: Admins can invite staff to join their library
- **RESTful API**: Well-structured API with proper error handling
- **Swagger UI**: Interactive API documentation

## 🛠️ Technologies Used

- **Java 21**
- **Spring Boot 3.4.5**
- **Spring Security** for authentication and authorization
- **Spring Data JPA** for database access
- **PostgreSQL** for data storage
- **Flyway** for database migrations
- **JWT (JJWT 0.12.6)** for authentication tokens
- **Testcontainers** for integration testing
- **Docker** for containerization
- **SpringDoc OpenAPI** for API documentation

## 🚀 Getting Started

### Prerequisites

- Java 21 or higher
- Docker and Docker Compose
- Maven
- PostgreSQL (or use the provided Docker Compose setup)

### Setup

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/bookly.git
   cd bookly
   ```

2. Configure environment variables:
   ```
   # Create and edit .env file with required variables
   touch .env
   chmod 600 .env
   
   # Add required variables
   MAIL_USERNAME=your_email@gmail.com
   MAIL_PASSWORD=your_app_password
   ```

3. Start the PostgreSQL database:
   ```
   docker-compose up -d
   ```

4. Run the application:
   ```
   ./mvnw spring-boot:run
   ```

5. Access the API documentation:
   ```
   http://localhost:8080/swagger-ui
   ```

## 📡 API Endpoints

### Authentication

- `POST /api/auth/login` - Authenticate and get JWT token
- `POST /api/auth/register/library` - Register a new library
- `POST /api/auth/register/employee` - Register as a library employee (requires invitation)
- `POST /api/auth/verify` - Verify OTP for registration
- `POST /api/auth/resend-otp` - Resend OTP if expired

### Tenant Management

- `GET /api/tenants` - Get all tenants (admin only)
- `POST /api/tenants` - Create a new tenant (admin only)

### User Management

- `GET /users` - Get all users in current tenant
- `POST /users` - Create a new user in current tenant

## 🔐 Authentication Flow

1. **Library Registration**:
   - Admin submits registration with library details
   - System sends OTP to verify email
   - Upon verification, creates new tenant schema and admin account

2. **Employee Registration**:
   - Admin invites employee via email
   - Employee registers with invitation
   - System sends OTP to verify email
   - Upon verification, creates employee account in the correct tenant

3. **Authentication**:
   - Users login with username/password
   - System issues JWT token with tenant ID embedded
   - Token is used for subsequent API calls

4. **Request Flow**:
   - `TenantInterceptor` extracts tenant from request header
   - `TenantContext` stores tenant ID for the request duration
   - `ConnectionProvider` switches to correct database schema
   - Operations are isolated to the tenant's schema

## 📂 Project Structure

```
src/main/java/dev/sushaanth/bookly/
├── BooklyApplication.java                  # Application entry point
├── exception/                              # Global exception handling
├── multitenancy/                           # Multi-tenancy implementation
│   ├── context/                            # Tenant context
│   │   └── TenantContext.java              # ThreadLocal tenant storage
│   ├── data/                               # Data isolation
│   │   └── hibernate/                      # Hibernate integration
│   ├── resolver/                           # Tenant resolution
│   │   └── HttpHeaderTenantResolver.java   # Extract tenant from HTTP headers
│   └── web/                                # Web request handling
│       └── TenantInterceptor.java          # Process tenant in HTTP requests
├── security/                               # Security implementation
│   ├── controller/                         # Auth controllers
│   ├── dto/                                # Auth DTOs
│   ├── model/                              # Security entities
│   │   ├── LibraryUser.java                # User entity
│   │   ├── Role.java                       # User roles
│   │   └── VerificationToken.java          # Email verification
│   └── service/                            # Security services
├── tenant/                                 # Tenant management
│   ├── Tenant.java                         # Tenant entity
│   └── TenantService.java                  # Tenant operations
└── user/                                   # User management
    ├── User.java                           # Library user entity
    └── UserController.java                 # User API endpoints
```

## 🧪 Testing

The project includes integration tests using Testcontainers:

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=MultiTenantConcurrencyTest
```

The `MultiTenantConcurrencyTest` verifies that tenant isolation works correctly under concurrent access.

## 🔮 Future Plans

- Book management and cataloging
- Borrowing system with due date tracking
- Fine calculation
- Reporting and analytics
- Mobile app integration
- SSO and OAuth 2.0 support
- Event-driven architecture with webhooks

## 👨‍💻 Contributing

As this project is still in development, contributions are welcome. Please feel free to submit a pull request or open an issue.

## 📝 License

This project is licensed under the MIT License.

---

Project maintained by [Sushaanth Suresh Kumar](https://github.com/Sushaanth-Suresh-Kumar)
