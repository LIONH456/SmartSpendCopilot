import 'package:flutter_test/flutter_test.dart';
import 'package:smartspend_mobile/view_models/expense_view_model.dart';

void main() {
  group('ExpenseViewModel - Core Business Logic Tests', () {
    late ExpenseViewModel viewModel;

    setUp(() {
      // Create viewModel before each test
      viewModel = ExpenseViewModel();
    });

    tearDown(() {
      // Clean up after each test
      viewModel.stopExchangeUpdates();
    });

    // ==================== LOADING STATE TESTS ====================

    test(
      'Should initialize with empty transaction list and isLoading=false',
      () {
        // Assert: Initial state should be empty
        expect(viewModel.transactions, isEmpty);
        expect(viewModel.isLoading, false);
        expect(viewModel.errorMessage, isNull);
      },
    );

    test(
      'Should initialize displayCurrency as USD',
      () {
        // Assert: Initial currency should be USD
        expect(viewModel.displayCurrency, 'USD');
        expect(viewModel.exchangeRate, 1.0);
      },
    );

    test(
      'Should calculate totalExpenses correctly in USD when empty',
      () async {
        // If transactions list is empty, totalExpenses should be 0
        expect(viewModel.totalExpenses, 0.0);
      },
    );
  });
}
