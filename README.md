# SmartSpend AI

> AI-powered expense tracking platform with natural-language transaction parsing, real-time currency conversion, and full-stack financial analytics.

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
* Transaction filtering & sorting
* RESTful API architecture
* DTO + Mapper separation
* Global exception handling
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

Filtering:

* Category
* Merchant

Sorting:

* Amount
* Merchant
* Category
* ID

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

---

## Validation & Error Handling

* Request validation using `@Valid`
* Global centralized exception handling
* Custom exceptions:

  * `TransactionNotFoundException`
  * `AIParsingException`

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

# Project Structure

## Backend

```text
copilot/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── mapper/
├── exception/
├── client/
└── integration/
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

## 2. Install dependencies

```bash
./mvnw clean install
```

---

## 3. Run the application

```bash
./mvnw spring-boot:run
```

---

## 4. Verify exchange-rate endpoint

```bash
curl "http://localhost:8080/api/transactions/rate?base=USD&target=VND"
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
6. Toggle between USD and VND

---

# Future Improvements

* Pagination
* JWT Authentication
* Spring Security
* Role-based Authorization
* Docker Deployment
* Redis Caching
* CI/CD Pipeline
* Analytics Dashboard
* Budget Tracking
* Multi-Currency Support

---

# Notes

* Exchange rates are cached server-side to reduce API calls.
* Fallback exchange-rate logic ensures system stability if external providers fail.
* Architecture is intentionally designed for future scalability and production upgrades.

---

# What I Learned

Building SmartSpend AI strengthened my understanding of:

* Spring Boot backend development
* API design principles
* DTO & entity separation
* Hibernate & ORM concepts
* Database interaction
* Validation & exception handling
* Unit & integration testing
* Full-stack application architecture
* Clean backend engineering practices

---
