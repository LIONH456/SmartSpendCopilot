# SmartSpend AI

> AI-powered expense tracking platform with natural-language transaction parsing, real-time currency conversion, pagination, API documentation, and production-ready backend architecture.

---

# Overview

SmartSpend AI is a full-stack personal finance application that allows users to record expenses using natural language.

Instead of manually filling forms, users can enter descriptions like:

```text
"Paid 240K VND for Grab ride"
```

The system automatically extracts structured transaction data using AI and stores normalized financial records into a relational database.

The platform includes:

* AI-driven expense parsing
* Currency normalization
* Pagination & filtering
* OpenAPI / Swagger documentation
* RESTful API architecture
* DTO + Mapper separation
* Global exception handling
* Docker containerization
* Unit & integration testing
* Responsive frontend dashboard

---

# Key Features

## AI Expense Parsing

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

## Currency Conversion & Normalization

* Automatic VND → USD normalization
* Real-time exchange rate integration
* Cached exchange rates for API optimization
* Fallback default exchange rate handling
* Original currency tracking support

---

## Transaction Management

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

## Swagger / OpenAPI Documentation

Integrated Swagger UI for interactive API testing and documentation.

Features:

* Interactive endpoint testing
* Request/response schemas
* Validation documentation
* Error response examples
* Pagination response structure

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

---

## Docker Support

SmartSpend AI supports Docker containerization for portable deployment.

Features:

* Containerized Spring Boot backend
* Environment variable support
* Secure API key management
* Docker Compose ready
* Production deployment preparation

---

## Clean Backend Architecture

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

---

## Validation & Error Handling

* Request validation using `@Valid`
* Global centralized exception handling
* Standardized API error responses
* Custom exceptions:

  * `TransactionNotFoundException`
  * `AIParsingException`

Example Error Response:

```json
{
  "timestamp": 1716999999999,
  "status": 400,
  "error": "Bad Request",
  "message": "Description cannot be blank"
}
```

---

## Testing Coverage

### Unit Testing

* Service Layer
* Controller Layer

### Integration Testing

* Repository Integration Tests
* Service Integration Tests
* Controller Integration Tests

### Testing Tools

* JUnit 5
* Mockito
* MockMvc
* H2 In-Memory Database

---

# Tech Stack

## Backend

* Java 21
* Spring Boot
* Spring MVC
* Spring Data JPA
* Hibernate
* Maven
* Lombok
* MapStruct
* Swagger / OpenAPI

---

## Database

* MySQL
* H2 Database (Testing)

---

## Frontend

* Flutter
* Dart
* MVVM Architecture
* ChangeNotifier
* ListenableBuilder

---

## AI & External APIs

* Gemini API
* Exchange Rate API

---

# Backend Engineering Concepts Applied

This project demonstrates understanding of:

* RESTful API Design
* Layered Architecture
* DTO / Entity Separation
* ORM with Hibernate
* Repository Abstraction
* Dependency Injection
* Validation
* Exception Handling
* Pagination
* API Documentation
* Docker Containerization
* Unit Testing
* Integration Testing
* API Integration
* Object Mapping
* Clean Code Practices

---

# API Endpoints

## Process Transaction

```http
POST /api/transactions/process
```

---

## Get Transactions

```http
GET /api/transactions
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
```

---

## Delete Transaction

```http
DELETE /api/transactions/{id}
```

---

## Exchange Rate

```http
GET /api/transactions/rate
```

---

# Frontend Screenshots

## Dashboard

![Dashboard](./screenshots/dashboard.png)

---

## Transaction History

![Transaction History](./screenshots/transactions.png)

---

## Expense Analytics

![Analytics](./screenshots/analytics.png)

---

## Add Transaction

![Add Transaction](./screenshots/add-transaction.png)

---

## Currency Conversion

![Currency](./screenshots/currency.png)

---

## Mobile UI

![Mobile UI](./screenshots/mobile-ui.png)

---

# Project Structure

## Backend

```text
copilot/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
│   ├── request/
│   └── response/
├── mapper/
├── exception/
├── client/
├── integration/
│   ├── controller/
│   ├── service/
│   └── repository/
└── config/
```

---

## Frontend

```text
smartspend_mobile/lib/
├── models/
├── services/
├── view_models/
├── views/
└── widgets/
```

---

# Setup & Installation

# Backend

## 1. Navigate to backend

```bash
cd copilot
```

---

## 2. Create environment variables

Create a `.env` file:

```env
GEMINI_API_TOKEN=your_api_key
```

---

## 3. Install dependencies

```bash
./mvnw clean install
```

---

## 4. Run the application

```bash
./mvnw spring-boot:run
```

---

## 5. Open Swagger UI

```text
http://localhost:8080/swagger-ui.html
```

---

# Docker Setup

## Build Docker image

```bash
docker build -t smartspend .
```

---

## Run Docker container

```bash
docker run --env-file .env -p 8080:8080 smartspend
```

---

# Frontend

## 1. Navigate to frontend

```bash
cd smartspend_mobile
```

---

## 2. Install Flutter packages

```bash
flutter pub get
```

---

## 3. Run the application

```bash
flutter run
```

---

# Recommended Workflow

1. Start backend
2. Start Flutter frontend
3. Enter natural-language expense descriptions
4. Track transactions
5. Filter & sort transaction history
6. Navigate paginated transaction pages
7. Toggle between USD and VND

---

# Future Improvements

* JWT Authentication
* Spring Security
* Role-based Authorization
* Redis Caching
* CI/CD Pipeline
* Analytics Dashboard
* Budget Tracking
* Multi-Currency Support
* Docker Compose Deployment
* Cloud Database Hosting
* Kubernetes Deployment

---

# Notes

* Exchange rates are cached server-side to reduce API calls.
* Fallback exchange-rate logic ensures system stability if external providers fail.
* Architecture is intentionally designed for future scalability and production upgrades.
* Pagination is implemented server-side for scalability and database efficiency.

---

# What I Learned

Building SmartSpend AI strengthened my understanding of:

* Spring Boot backend development
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

---
