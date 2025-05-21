# 📚 Bookly - Multi-tenant Library Management System

![Status](https://img.shields.io/badge/Status-Work%20In%20Progress-yellow)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-green)
![License](https://img.shields.io/badge/License-MIT-blue)

Bookly is a modern, multi-tenant library management system built with Spring Boot. It enables multiple libraries to operate on a single platform, with each library isolated in its own tenant environment for complete data separation and security.

## 🏛️ Architecture

Bookly implements a schema-based multi-tenancy pattern:

- Each tenant (library) has its own dedicated PostgreSQL schema
- Complete data isolation between tenants through Hibernate's multi-tenancy support
- JWT-based authentication with tenant context embedded in tokens
- Role-based access control (library admins and employees)

```
┌─────────────────┐     ┌──────────────────┐     ┌───────────────────┐
│  HTTP Request   │────▶│  JwtRequestFilter │────▶│   TenantContext   │
│ with JWT Token  │     │ Extracts Tenant ID│     │ Stores Tenant ID  │
└─────────────────┘     └──────────────────┘     └─────────┬─────────┘
                                                           │
                                                           ▼
┌─────────────────┐     ┌──────────────────┐     ┌───────────────────┐
│  Database       │◀────│  ConnectionProvider    │   Hibernate ORM    │
│  (PostgreSQL)   │     │  Schema Switching │◀────│   Repository      │
└─────────────────┘     └──────────────────┘     └───────────────────┘
```

## 🔑 Key Features

- **Multi-tenancy**: Complete tenant isolation through PostgreSQL schemas
- **Authentication**: Secure JWT-based authentication with ECDSA signatures
- **User Management**: Registration, login, and role-based access control
- **Email Verification**: OTP-based email verification during registration
- **Invitation System**: Library admins can invite employees
- **RESTful API**: Well-structured API with proper error handling
- **Swagger UI**: Interactive API documentation

## 🛠️ Technologies

- **Java 21**
- **Spring Boot 3.4.5**
- **Spring Security** for authentication
- **Spring Data JPA** for database access
- **PostgreSQL** for data storage
- **Flyway** for database migrations
- **JWT (JJWT 0.12.6)** for authentication tokens
- **Testcontainers** for integration testing
- **Docker** for containerization

## 🚀 Getting Started

### Prerequisites

- Java 21
- Maven
- Docker and Docker Compose
- PostgreSQL (or use the Docker Compose setup)

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/bookly.git
   cd bookly
   ```

2. Configure local development settings:

   Create a file named `application-local.properties` in `src/main/resources/`:
   ```properties
   # Email Configuration (Replace with your own credentials)
   spring.mail.username=your_email@gmail.com
   spring.mail.password=your_app_password

   # JWT Configuration (Use a strong secret key in production)
   jwt.secret=YOUR_SECRET_KEY_HERE_NEEDS_TO_BE_AT_LEAST_32_BYTES_LONG_FOR_SECURITY

   # Database Configuration (If different from compose.yaml)
   # spring.datasource.url=jdbc:postgresql://localhost:8000/multitenant
   # spring.datasource.username=postgres
   # spring.datasource.password=postgres
   ```

3. Start PostgreSQL using Docker Compose:
   ```bash
   docker-compose up -d
   ```

4. Run the application with local profile:
   ```bash
   ./mvnw spring-boot:run -Dspring.profiles.active=local
   ```

5. Access Swagger UI for API documentation:
   ```
   http://localhost:8080/swagger-ui
   ```

## 📝 Development Guide

### Project Structure

```
src/main/java/dev/sushaanth/bookly/
├── BooklyApplication.java             # Application entry point
├── exception/                          # Global exception handling
├── multitenancy/                       # Multi-tenancy implementation
│   ├── context/                        # Tenant context management
│   ├── data/                           # Data isolation (Hibernate)
├── security/                           # Authentication and authorization
│   ├── controller/                     # Auth endpoints
│   ├── dto/                            # Request/response objects
│   ├── jwt/                            # JWT implementation
│   ├── model/                          # Security entities
│   └── service/                        # Security services
├── tenant/                             # Tenant management
├── user/                               # User management
```

### Multi-tenancy Flow

1. Request comes in with JWT token in the Authorization header
2. `JwtRequestFilter` extracts the tenant schema from token claims
3. `TenantContext` stores tenant ID in ThreadLocal
4. `ConnectionProvider` and `TenantIdentifierResolver` switch to the correct schema
5. Operations are isolated to the tenant's schema
6. `TenantContext` is cleared after request completion

### Authentication Flow

1. **Registration**:
   - Email verification with OTP
   - Tenant creation (for library admins)
   - User account creation

2. **Login**:
   - Username/password authentication
   - JWT token generation with tenant context

3. **Request Processing**:
   - JWT validation
   - Tenant context extraction
   - Role-based authorization

## 🔄 Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=MultiTenantConcurrencyTest
```

## 📊 API Endpoints

### Authentication

- `POST /api/auth/login` - Authenticate and get JWT token
- `POST /api/register/init` - Start registration (email verification)
- `POST /api/register/verify-email` - Verify OTP code
- `POST /api/register/complete` - Complete registration
- `POST /api/register/resend-otp` - Resend verification code

### Tenant Management

- `GET /api/tenants` - Get all tenants (admin only)
- `POST /api/tenants` - Create new tenant (admin only)

### Invitation Management

- `GET /api/invitations` - Get pending invitations (admin only)
- `POST /api/invitations` - Create employee invitation (admin only)
- `POST /api/invitations/resend/{id}` - Resend invitation (admin only)

### User Management

- `GET /users` - Get all users in current tenant
- `POST /users` - Create new user in current tenant

## 📋 Production Deployment

For production deployment:

1. Use environment variables or external configuration for sensitive data
2. Configure a secure JWT secret key
3. Set up proper database credentials and connection pooling
4. Configure mail server settings for production
5. Enable HTTPS with proper SSL certificates
6. Set appropriate log levels

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add some amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

## 📜 License

This project is licensed under the MIT License - see the LICENSE file for details.