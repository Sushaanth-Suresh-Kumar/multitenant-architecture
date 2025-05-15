# Bookly - Multi-tenant Library Management System

![Status](https://img.shields.io/badge/Status-Work%20In%20Progress-yellow)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-green)
![License](https://img.shields.io/badge/License-MIT-blue)

## 🚧 Work In Progress 🚧

Bookly is a multi-tenant library management system built with Spring Boot, designed to support multiple libraries on a single platform. Each library operates in its own isolated tenant environment with dedicated data storage.

## 🔑 Key Features

- **Multi-tenancy**: Schema-based tenant isolation for data security
- **JWT Authentication**: Secure authentication with ECDSA-signed JWTs
- **Email Verification**: OTP-based email verification during registration
- **Role-based Access Control**: Library administrators and employees have different permissions
- **RESTful API**: Well-structured API with proper documentation
- **Swagger UI**: Interactive API documentation

## 🛠️ Technologies Used

- **Java 21**
- **Spring Boot 3.4.5**
- **Spring Security**
- **Spring Data JPA**
- **PostgreSQL**
- **Flyway** for database migrations
- **JWT** for authentication
- **Docker** for containerization
- **SpringDoc OpenAPI** for API documentation

## 🚀 Getting Started

### Prerequisites

- Java 21
- Docker and Docker Compose
- Maven
- PostgreSQL (or use the provided Docker Compose setup)

### Setup

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/bookly.git
   cd bookly
   ```

2. Start the PostgreSQL database:
   ```
   docker-compose up -d
   ```

3. Run the application:
   ```
   ./mvnw spring-boot:run
   ```

4. Access the API documentation:
   ```
   http://localhost:8080/swagger-ui
   ```

## 📝 API Endpoints

### Authentication

- `POST /api/auth/register/library` - Register a new library
- `POST /api/auth/register/employee` - Register a library employee
- `POST /api/auth/login` - Authenticate and get JWT token
- `POST /api/auth/verify` - Verify OTP for registration
- `POST /api/auth/resend-otp` - Resend OTP if expired

### User Management

- `GET /users` - Get all users
- `POST /users` - Create a new user

### Employee Management

- `POST /api/employees/invite` - Invite an employee to join the library

## 📋 Project Structure

```
src/main/java/dev/sushaanth/bookly/
├── BooklyApplication.java                  # Application entry point
├── config/                                 # Configuration classes
├── exception/                              # Global exception handling
├── multitenancy/                           # Multi-tenancy implementation
│   ├── context/                            # Tenant context
│   ├── data/                               # Data isolation
│   ├── resolver/                           # Tenant resolution
│   └── web/                                # Web request handling
├── security/                               # Security implementation
│   ├── controller/                         # Auth controllers
│   ├── dto/                                # Data transfer objects
│   ├── model/                              # Security entities
│   ├── repository/                         # Security repositories
│   └── service/                            # Security services
├── tenant/                                 # Tenant management
└── user/                                   # User management
```

## 📜 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🔮 Future Plans

- Implement book management
- Add borrowing functionality
- Create dashboard analytics
- Add reporting features
- Implement webhooks for integrations
- Support for SSO and OAuth 2.0
- Mobile app integration

## 👨‍💻 Contributing

As this project is still in development, contributions are welcome. Please feel free to submit a pull request or open an issue.

---

Project maintained by [Sushaanth Suresh Kumar](https://github.com/Sushaanth-Suresh-Kumar)
