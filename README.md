# SmartSpend AI

> AI-powered personal finance tracking with natural-language expense input, premium dark-slate analytics, and live USD/VND exchange conversion.

![SmartSpend AI Placeholder](./screenshots/dashboard.png)

## What is SmartSpend AI?
SmartSpend AI turns raw expense text into structured transaction data using a Gemini-powered backend. Enter sentences like `Bought coffee at Starbuck for 45k VND`, and the app extracts `amount`, `merchant`, and `category` automatically.

## Value Proposition
SmartSpend AI removes the friction of manual budget tracking by:
- Parsing conversational expense text into ledger entries
- Displaying a premium financial dashboard with totals and history
- Supporting live USD/VND currency conversion for international users
- Providing backend persistence plus AI-driven categorization

## Key Features
- **AI Expense Copilot**
  - Converts free-form text into structured transaction objects.
  - Uses a Gemini Engine backend to preserve reliability and semantic accuracy.

- **Premium Financial Analytics UI**
  - Dark-slate theme with rich cards, clean typography, and responsive controls.
  - Total tracked expenses display with live exchange-rate visibility.

- **Dynamic Exchange Rate Integration**
  - Fetches live rates from `exchangerate.host`.
  - Uses server-side caching to minimize external provider load.
  - Supports two strategies:
    - `Boot-cache mode`: fetch once at startup for rate-limited providers.
    - `Polling mode`: refresh rates every 30 seconds when provider limits are generous.

- **Reactive History Ledger**
  - Scrollable transaction list with delete, filter, and sort controls.
  - Reflects currency toggles instantly across the record list.

## Architecture & Technical Stack
### Frontend
- Flutter / Dart mobile app
- MVVM architecture with:
  - `models/` for data structures
  - `view_models/` for business logic and state
  - `views/` for UI rendering
- Reactive state updates via `ChangeNotifier` and `ListenableBuilder`

### Backend
- Java Spring Boot service
- Lombok for entity/data annotations
- H2 database for local persistence
- AI service layer for Gemini interaction

### Exchange Rate Integration
- `ApiServices` calls `exchangerate.host` first
- If the provider is unavailable, the backend `/api/transactions/rate` endpoint provides a cached fallback and if not then default 1USD = 25000VND

## Currency Handling
- The backend automatically detects VND prompts when user text includes `VND`, `đ`, `dong`, or `đồng`.
- If a VND prompt is detected, the backend normalizes the stored amount to USD and tracks original currency metadata.
- The frontend lets users toggle between USD and VND, converting totals and transaction values using the latest rate.

## Code Structure
- Frontend entry: `smartspend_mobile/lib/main.dart`
- Dashboard view: `smartspend_mobile/lib/views/dashboard_view.dart`
- ViewModel logic: `smartspend_mobile/lib/view_models/expense_view_model.dart`
- Backend controller: `copilot/src/main/java/com/smartspend/copilot/controller/TransactionController.java`
- Backend exchange service: `copilot/src/main/java/com/smartspend/copilot/service/ExchangeRateService.java`

## Setup and Installation
### Backend
1. Open a terminal in `copilot`
2. Install dependencies and compile:
   ```bash
   cd "copilot"
   ./mvnw -DskipTests compile
   ```
3. Run the backend:
   ```bash
   ./mvnw spring-boot:run
   ```
4. Verify the rate endpoint:
   ```bash
   curl "http://localhost:8080/api/transactions/rate?base=USD&target=VND"
   ```

### Frontend
1. Open a terminal in `smartspend_mobile`
2. Get Dart packages:
   ```bash
   cd "smartspend_mobile"
   flutter pub get
   ```
3. Run the app:
   ```bash
   flutter run
   ```
4. If using Android emulator, the app will use `10.0.2.2` to reach `localhost:8080`.
   - For iOS simulator, update `ApiServices.baseUrl` to `http://localhost:8080/api/transactions`.

## Recommended Workflow
1. Start the backend
2. Start the Flutter app
3. Enter a natural expense description
4. Use the currency toggle to switch between USD and VND
5. Filter and sort transactions through the dashboard controls


- `./screenshots/dashboard.png`

## Notes
- The current backend exchange service uses `exchangerate.host` and caches rates for 30 minutes.
- The system is intentionally built to allow a provider fallback via the backend if external rate access fails.
- For production, replace the public provider with a secure server-side API key integration and persistent caching.
