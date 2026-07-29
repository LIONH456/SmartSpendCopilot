import 'dart:async';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:smartspend_mobile/core/network/api_error.dart';
import 'package:smartspend_mobile/models/transaction.dart';
import 'package:smartspend_mobile/services/api_service.dart';

/// ViewModel managing SmartSpend dashboard state: transaction list, loading,
/// filters, error banners, currency toggle, and all AI-triggered batch adds.
///
/// State-change strategy (by explicit user requirement):
///   • cross-account data wipe → ONLY happens inside clearLocalState()
///   • normal loadTransactions() → preserves the LAST KNOWN GOOD list during
///     the async network gap to avoid UI white flashes; replaces the list atomically
///     on success so users don't see "no transactions yet" flickering.
class ExpenseViewModel extends ChangeNotifier {
  final ApiServices _apiServices = ApiServices();
  List<Transaction> _transactions = [];
  bool _isLoading = false;
  String? _errorMessage;
  ApiError? _lastApiError;
  String? _categoryFilter;
  String? _merchantFilter;
  String _sortField = 'amount';
  String _sortOrder = 'desc';

  String _displayCurrency = 'USD';
  double _exchangeRate = 1.0;
  String? _currentUsername;

  List<Transaction> get transactions => List.unmodifiable(_transactions);
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  ApiError? get lastApiError => _lastApiError;
  bool get clarificationRequired => _lastApiError?.details?['clarification'] == true;
  String? get categoryFilter => _categoryFilter;
  String? get merchantFilter => _merchantFilter;
  String get sortField => _sortField;
  String get sortOrder => _sortOrder;
  String get displayCurrency => _displayCurrency;
  double get exchangeRate => _exchangeRate;
  String? get currentUsername => _currentUsername;

  double get totalExpenses {
    final sum = _transactions.fold<double>(
      0.0,
      (sum, item) => sum + (item.amount < 0 ? 0.0 : item.amount),
    );
    return _displayCurrency == 'VND' ? sum * _exchangeRate : sum;
  }

  Timer? _rateTimer;

  Future<void> initExchange({bool providerIsRateLimited = false}) async {
    if (providerIsRateLimited) {
      try {
        _exchangeRate =
            await _apiServices.getExchangeRate(base: 'USD', target: 'VND');
      } catch (e) {
        _errorMessage = userFriendlyMessage(e);
      }
      notifyListeners();
      return;
    }

    try {
      _exchangeRate =
          await _apiServices.getExchangeRate(base: 'USD', target: 'VND');
    } catch (e) {
      _errorMessage = userFriendlyMessage(e);
    }
    notifyListeners();

    _rateTimer?.cancel();
    _rateTimer = Timer.periodic(const Duration(seconds: 30), (_) async {
      try {
        final rate =
            await _apiServices.getExchangeRate(base: 'USD', target: 'VND');
        _exchangeRate = rate;
        notifyListeners();
      } catch (_) {
        // polling errors silent; keep last known rate
      }
    });
  }

  void stopExchangeUpdates() {
    _rateTimer?.cancel();
    _rateTimer = null;
  }

  /// Invoked by AuthService on login/logout.
  /// This is the ONLY place where we wipe the in-memory transaction list.
  void clearLocalState() {
    _transactions = [];
    _errorMessage = null;
    _categoryFilter = null;
    _merchantFilter = null;
    _sortField = 'amount';
    _sortOrder = 'desc';
    _exchangeRate = 1.0;
    _displayCurrency = 'USD';
    _currentUsername = null;
    notifyListeners();
  }

  void setCurrentUser({String? username}) {
    _currentUsername = username != null && username.trim().isNotEmpty
        ? username.trim()
        : null;
    notifyListeners();
  }

  /// Called by dashboard clear-input button. Only wipes the error banner,
  /// leaving loaded transactions intact.
  void clearInputErrorIfAny() {
    if (_errorMessage != null || _lastApiError != null) {
      _errorMessage = null;
      _lastApiError = null;
      notifyListeners();
    }
  }

  ApiError? _extractApiError(dynamic error) {
    if (error is ApiError) return error;
    if (error is DioException && error.error is ApiError) {
      return error.error as ApiError;
    }
    return null;
  }

  /// Fetch transactions from the backend.
  ///
  /// DESIGN RULE (requested): we do NOT clear the local list before the HTTP
  /// call.  If a network call fails users still see the previously-loaded
  /// rows, providing a smooth "still-usable" dashboard instead of a
  /// flickering empty state.
  Future<void> loadTransactions({
    String? category,
    String? merchant,
    String? sortField,
    String? sortOrder,
  }) async {
    _isLoading = true;
    _errorMessage = null;
    // NOTE: no `_transactions = []` here — keep last-known-good list displayed
    // while the network fetch is in flight.
    notifyListeners();

    if (category != null) {
      _categoryFilter = category.trim().isNotEmpty ? category.trim() : null;
    }
    if (merchant != null) {
      _merchantFilter = merchant.trim().isNotEmpty ? merchant.trim() : null;
    }
    if (sortField != null && sortField.trim().isNotEmpty) {
      _sortField = sortField.trim();
    }
    if (sortOrder != null && sortOrder.trim().isNotEmpty) {
      _sortOrder = sortOrder.trim();
    }

    try {
      final fetched = await _apiServices.getTransactions(
        category: _categoryFilter,
        merchant: _merchantFilter,
        sort: _sortField,
        order: _sortOrder,
      );
      // ATOMIC replace — entire list switches at once, no partial updates.
      _transactions = fetched;
    } catch (e) {
      _errorMessage = userFriendlyMessage(e);
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> clearFilters() async {
    _categoryFilter = null;
    _merchantFilter = null;
    await loadTransactions(sortField: _sortField, sortOrder: _sortOrder);
  }

  Future<bool> updateTransaction(Transaction transaction) async {
    _errorMessage = null;
    _isLoading = true;
    notifyListeners();
    try {
      final updated = await _apiServices.updateTransaction(transaction.id!, transaction);
      final index = _transactions.indexWhere((item) => item.id == transaction.id);
      if (index != -1) {
        _transactions[index] = updated;
      }
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = userFriendlyMessage(e);
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> deleteTransaction(int id) async {
    _errorMessage = null;
    final index = _transactions.indexWhere((item) => item.id == id);
    Transaction? removed;
    if (index != -1) {
      removed = _transactions.removeAt(index);
      notifyListeners();
    }

    _isLoading = true;
    notifyListeners();

    try {
      await _apiServices.deleteTransaction(id);
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      if (removed != null) {
        _transactions.insert(index, removed);
      }
      _errorMessage = userFriendlyMessage(e);
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<void> toggleCurrency() async {
    if (_displayCurrency == 'USD') {
      _isLoading = true;
      _errorMessage = null;
      notifyListeners();
      try {
        final rate =
            await _apiServices.getExchangeRate(base: 'USD', target: 'VND');
        _exchangeRate = rate;
        _displayCurrency = 'VND';
      } catch (e) {
        _errorMessage = userFriendlyMessage(e);
      } finally {
        _isLoading = false;
        notifyListeners();
      }
    } else {
      _displayCurrency = 'USD';
      _exchangeRate = 1.0;
      notifyListeners();
    }
  }

  /// Process raw user text into 1..N batch transactions.
  ///
  /// Returns: number of transactions created (0 on failure).
  /// On failure also sets `_errorMessage` so the dashboard can both show
  /// the inline banner AND pop a SnackBar.
  Future<int> processRawExpense(String text) async {
    final trimmed = text.trim();
    if (trimmed.isEmpty) return 0;
    _isLoading = true;
    _errorMessage = null;
    _lastApiError = null;
    notifyListeners();

    try {
      final newTxs = await _apiServices.processExpenseBatch(trimmed);
      if (newTxs.isEmpty) {
        throw ApiError(
          status: 500,
          code: 4001,
          error: 'AI Parsing Failed',
          message: 'No transactions could be parsed from that description.',
          path: '/api/transactions/process-batch',
        );
      }
      for (final tx in newTxs.reversed) {
        _transactions.insert(0, tx);
      }
      _lastApiError = null;
      _isLoading = false;
      notifyListeners();
      return newTxs.length;
    } catch (e) {
      _lastApiError = _extractApiError(e);
      _errorMessage = userFriendlyMessage(e);
      _isLoading = false;
      notifyListeners();
      return 0;
    }
  }
}
