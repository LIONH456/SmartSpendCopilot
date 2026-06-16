# SmartSpend AI

> AI-powered expense tracking platform with natural-language transaction parsing, real-time currency conversion, pagination, API documentation, and production-ready backend architecture. **Now with complete JWT authentication!**

![Java Version](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-green)
![Flutter](https://img.shields.io/badge/Flutter-3.x-blue)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## 📋 Project Overview

SmartSpend AI is a full-stack personal finance application that allows users to record expenses using natural language.

Instead of manually filling forms, users can enter descriptions like:

```text
"Paid 240K VND for Grab ride"
```

The system automatically extracts structured transaction data using AI and stores normalized financial records into a relational database.

**New Features (v2):**
- ✅ Complete JWT Authentication System
- ✅ User Registration & Login
- ✅ Secure Password Encryption
- ✅ Token-based Authorization
- ✅ Flutter Frontend Authentication
- ✅ Comprehensive Test Coverage

---

## 🚀 Key Features

### AI Expense Parsing

Convert natural-language expense descriptions into structured transaction records.

Example:

```text
"Spent 15 dollars on pizza at Dominos"
```

Automatically becomes:

```json
{
  "amount": 15.0,
  "merchant": "Dominos",
  "category": "Food",
  "currency": "USD"
}
```

---

### 🔐 JWT Authentication

- Secure user registration with password complexity validation
- User login with username/email + password
- JWT token-based authentication
- Automatic token injection in API requests
- Token expiration handling
- Password encryption using BCrypt

**Password Requirements:**
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character (!@#$%^&*)

---

### Currency Conversion & Normalization

* Automatic VND → USD normalization
* Real-time exchange rate integration
* Cached exchange rates for API optimization
* Fallback default exchange rate handling
* Original currency tracking support

---

### Transaction Management

Supports:

* Create transactions
* Delete transactions
* Filter transactions
* Sort transactions
* Pagination

Filtering:

* Category
* Merchant

Sorting:

* Amount
* Merchant
* Category
* ID

Pagination:

* Page number
* Page size
* Total pages
* Total elements
* Last page detection

---

### Swagger / OpenAPI Documentation

Integrated Swagger UI for interactive API testing and documentation.

Features:

* Interactive endpoint testing
* Request/response schemas
* Validation documentation
* Error response examples
* Pagination response structure
* JWT Authentication support

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

---

### Docker Support

SmartSpend AI supports Docker containerization for portable deployment.

Features:

* Containerized Spring Boot backend
* Environment variable support
* Secure API key management
* Docker Compose ready
* Production deployment preparation

---

### Clean Backend Architecture

Structured using layered architecture:

```text
Controller
↓
Service
↓
Repository
↓
Database
```

Additional architecture layers:

* DTO
* Mapper
* Exception Handling
* External API Clients
* Pagination Response Wrapper
* JWT Filter & Security Config

---

### Validation & Error Handling

* Request validation using `@Valid`
* Global centralized exception handling
* Standardized API error responses
* Custom business exceptions and error codes
* JWT exception handling

Example Error Response:

```json
{
  "timestamp": "2026-05-29T09:36:34.651Z",
  "status": 400,
  "code": 1001,
  "error": "Bad Request",
  "message": "Username already exists",
  "path": "/api/auth/register"
}
```

---

### 🧪 Testing Coverage

#### Unit Testing

* Service Layer
* Controller Layer
* JWT Service
* Exception Handler

#### Integration Testing

* Repository Integration Tests
* Service Integration Tests
* Controller Integration Tests
* Authentication Integration Tests

#### Testing Tools

* JUnit 5
* Mockito
* MockMvc
* H2 In-Memory Database

---

## 🛠 Tech Stack

### Backend

* Java 21
* Spring Boot 4.0.6
* Spring MVC
* Spring Security 6
* Spring Data JPA
* Hibernate
* Maven
* Lombok
* MapStruct
* Swagger / OpenAPI (SpringDoc)
* JWT (JJWT 0.12.5)
* BCrypt Password Encoder

---

### Database

* MySQL (Production/Development)
* H2 Database (Testing)

---

### Frontend

* Flutter
* Dart
* MVVM Architecture
* Provider (State Management)
* Dio (HTTP Client)
* Flutter Secure Storage

---

### AI & External APIs

* Gemini API
* Exchange Rate API

---

## 🏗️ Backend Engineering Concepts Applied

This project demonstrates understanding of:

* RESTful API Design
* Layered Architecture
* DTO / Entity Separation
* ORM with Hibernate
* Repository Abstraction
* Dependency Injection
* JWT Authentication & Authorization
* Validation
* Global Exception Handling
* Pagination
* API Documentation
* Docker Containerization
* Unit Testing
* Integration Testing
* API Integration
* Object Mapping
* Clean Code Practices
* Spring Security Configuration

---

## 📡 API Endpoints

### Authentication Endpoints (New!)

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john.doe@example.com",
  "password": "SecurePass123!"
}
```

**Response:** 201 Created

#### Login User
```http
POST /api/auth/login
Content-Type: application/json

{
  "login": "john_doe",
  "password": "SecurePass123!"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Transaction Endpoints (Requires Authentication)

All transaction endpoints require a valid JWT token in the Authorization header:
```
Authorization: Bearer <token>
```

#### Process Transaction
```http
POST /api/transactions/process
Authorization: Bearer <token>
Content-Type: application/json

{
  "description": "spent 15$ on pizza at Dominos"
}
```

#### Get Transactions
```http
GET /api/transactions
Authorization: Bearer <token>
```

Supports:
* filtering
* sorting
* pagination

Query Parameters:
```text
?page=0
&size=10
&sort=amount
&order=desc
&category=Food
&merchant=Dominos
```

#### Delete Transaction
```http
DELETE /api/transactions/{id}
Authorization: Bearer <token>
```

#### Exchange Rate
```http
GET /api/transactions/rate
Authorization: Bearer <token>

Query Parameters:
?base=USD&target=VND
```

---

## 📱 Frontend Features

### Authentication Flow
- Login screen with validation
- Registration screen with password complexity hints
- JWT token storage using Flutter Secure Storage
- Automatic token injection using Dio interceptors
- Token expiration handling
- Logout functionality

### Dashboard
- Real-time expense tracking
- Currency toggle (USD/VND)
- Transaction filtering & sorting
- Pagination support
- Clean, dark-themed UI

---

## 📂 Project Structure

### Backend
```text
copilot/
├── src/main/java/com/smartspend/copilot/
│   ├── client/              # External API clients (Gemini, Exchange Rate)
│   ├── config/              # Configuration (Security, JWT Filter, App Config)
│   ├── controller/          # REST Controllers
│   ├── dto/                 # Data Transfer Objects
│   │   ├── request/         # Request DTOs (Register, Login, etc.)
│   │   └── response/        # Response DTOs
│   ├── entity/              # JPA Entities
│   ├── exception/           # Exceptions & Error Handling
│   ├── mapper/              # MapStruct Mappers
│   ├── repository/          # Spring Data JPA Repositories
│   └── service/             # Business Logic Services
└── src/test/java/com/smartspend/copilot/
    ├── unit/                # Unit Tests
    │   ├── config/
    │   ├── controller/
    │   ├── exception/
    │   └── service/
    └── integration/         # Integration Tests
        ├── AuthenticationIntegrationTest
        ├── controller/
        ├── repository/
        └── service/
```

### Frontend
```text
smartspend_mobile/lib/
├── core/
│   ├── network/             # Dio Client & API Configuration
│   └── services/            # Auth Service
├── features/auth/
│   ├── data/                # Auth API & Repository
│   └── presentation/        # Login & Register Screens
├── models/                  # Data Models
├── services/                # API Services
├── view_models/             # ViewModels (MVVM)
└── views/                   # UI Screens (Dashboard)
```

---

## 🔧 Setup & Installation

### Backend

#### 1. Navigate to backend
```bash
cd copilot
```

#### 2. Create environment variables
Create a `.env` file based on `.env.example`:

```env
# Database Configuration
DB_NAME=smartspend
DB_USERNAME=root
DB_PASSWORD=your_password

# JWT Configuration (use a strong secret in production)
JWT_SECRET=your_strong_jwt_secret_key_here_min_256_bits

# Gemini AI API Key
GEMINI_API_TOKEN=your_api_key
```

#### 3. Install dependencies
```bash
./mvnw clean install
```

#### 4. Run the application
```bash
./mvnw spring-boot:run
```

Or with Maven Wrapper on Windows:
```bash
mvnw.cmd spring-boot:run
```

#### 5. Open Swagger UI
```text
http://localhost:8080/swagger-ui.html
```

---

### Docker Setup

#### Build Docker image
```bash
docker build -t smartspend .
```

#### Run Docker container
```bash
docker run --env-file .env -p 8080:8080 smartspend
```

---

### Frontend

#### 1. Navigate to frontend
```bash
cd smartspend_mobile
```

#### 2. Install Flutter packages
```bash
flutter pub get
```

#### 3. Run the application
```bash
flutter run
```

---

### Running Tests

#### Backend Tests
```bash
cd copilot
./mvnw test
```

#### Frontend Tests
```bash
cd smartspend_mobile
flutter test
```

---

## 📊 JWT Authentication Flow

```
1. User Registration
   ├─ Client sends username, email, password
   ├─ Server validates input complexity
   ├─ Server checks for duplicate username/email
   └─ Server creates new user with BCrypt encrypted password

2. User Login
   ├─ Client sends login identifier (username/email) + password
   ├─ Server retrieves user from database
   ├─ Server verifies password with BCrypt
   └─ Server generates and returns JWT token

3. Authenticated Requests
   ├─ Client adds JWT token to Authorization header
   ├─ JwtAuthenticationFilter validates token
   ├─ SecurityContext is populated with user details
   └─ Request proceeds to protected endpoint

4. Token Expiration
   ├─ Filter detects expired token
   ├─ Returns standardized 401 error response
   └─ Client can redirect to login screen
```

---

## 🔍 API Documentation Audit

### Swagger Coverage:
- ✅ All endpoints documented with @Operation
- ✅ All DTOs with @Schema annotations
- ✅ Response codes documented with @ApiResponse
- ✅ Authentication requirements specified
- ✅ Error responses documented

### Authentication Documentation:
- ✅ Register endpoint with validation constraints
- ✅ Login endpoint with token response
- ✅ JWT header requirements
- ✅ Error codes for authentication failures

---

## 📈 Recommended Workflow

1. Start backend
2. Open Swagger UI and register a new user
3. Login and copy the JWT token
4. Use token in Swagger UI to authorize requests
5. Start Flutter frontend
6. Register/login on mobile
7. Enter natural-language expense descriptions
8. Track transactions in dashboard
9. Filter & sort transaction history
10. Navigate paginated transaction pages
11. Toggle between USD and VND

---

## 🚀 Future Improvements

* OAuth2 (Google, Apple) Integration
* Role-based Authorization
* Redis Caching
* CI/CD Pipeline
* Analytics Dashboard
* Budget Tracking
* Multi-Currency Support (native, not just normalization)
* Transaction Editing
* Receipt Upload & OCR
* Budget Alerts
* Cloud Database Hosting
* Kubernetes Deployment
* Dark/Light Theme Toggle

---

## 📝 Notes

* Exchange rates are cached server-side to reduce API calls.
* Fallback exchange-rate logic ensures system stability if external providers fail.
* Architecture is intentionally designed for future scalability and production upgrades.
* Pagination is implemented server-side for scalability and database efficiency.
* JWT tokens are signed and verified for security.
* All sensitive data (JWT secrets, API keys) should be properly secured in production.

---

## 🎓 What I Learned

Building SmartSpend AI strengthened my understanding of:
* Spring Boot backend development
* Spring Security & JWT Authentication
* API design principles
* DTO & entity separation
* Hibernate & ORM concepts
* Database interaction
* Validation & exception handling
* Pagination architecture
* Swagger/OpenAPI documentation
* Docker containerization
* Unit & integration testing
* Full-stack application architecture
* Clean backend engineering practices
* Flutter state management with Provider
* Secure token storage on mobile
* Dio interceptors for automatic authentication
