import 'dart:async';
import 'package:flutter/material.dart';
import 'package:smartspend_mobile/services/api_service.dart';

class ExpenseViewModel extends ChangeNotifier {
  // ViewModel for managing the state of expenses, including fetching transactions and processing new expenses through the ApiServices.
  final ApiServices _apiServices = ApiServices();
  List<dynamic> _transactions = [];
  bool _isLoading = false;
  String? _errorMessage;
  String? _categoryFilter;
  String? _merchantFilter;
  String _sortField = 'amount';
  String _sortOrder = 'desc';

  // Currency support
  String _displayCurrency = 'USD';
  double _exchangeRate = 1.0; // USD -> VND when displayCurrency == 'VND'

  // Encapsulation (Getters) to expose private fields to the UI while maintaining control over state changes.
  List<dynamic> get transactions => _transactions;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  String? get categoryFilter => _categoryFilter;
  String? get merchantFilter => _merchantFilter;
  String get sortField => _sortField;
  String get sortOrder => _sortOrder;
  String get displayCurrency => _displayCurrency;
  double get exchangeRate => _exchangeRate;

  double get totalExpenses {
    final sum = _transactions.fold(0.0, (sum, item) => sum + item.amount);
    return _displayCurrency == 'VND' ? sum * _exchangeRate : sum;
  }

  // Internal timer for periodic rate updates when polling is enabled
  Timer? _rateTimer;

  /// Initialize exchange rate handling.
  /// If [providerIsRateLimited] is true, fetches the rate exactly once and caches it.
  /// Otherwise starts a periodic poll every 30 seconds.
  Future<void> initExchange({bool providerIsRateLimited = false}) async {
    if (providerIsRateLimited) {
      try {
        final rate = await _apiServices.getExchangeRate(base: 'USD', target: 'VND');
        _exchangeRate = rate;
      } catch (e) {
        _errorMessage = e.toString().replaceAll('Exception: ', '');
      }
      notifyListeners();
      return;
    }

    // generous free tier: start polling every 30s
    try {
      final rate = await _apiServices.getExchangeRate(base: 'USD', target: 'VND');
      _exchangeRate = rate;
    } catch (e) {
      _errorMessage = e.toString().replaceAll('Exception: ', '');
    }
    notifyListeners();

    _rateTimer?.cancel();
    _rateTimer = Timer.periodic(const Duration(seconds: 30), (_) async {
      try {
        final rate = await _apiServices.getExchangeRate(base: 'USD', target: 'VND');
        _exchangeRate = rate;
        notifyListeners();
      } catch (_) {
        // ignore polling errors silently; keep last known rate
      }
    });
  }

  /// Stop periodic updates (call on app dispose if needed).
  void stopExchangeUpdates() {
    _rateTimer?.cancel();
    _rateTimer = null;
  }

  // Fetches transactions from the backend API and updates the state accordingly. Handles loading and error states to provide feedback to the UI.
  Future<void> loadTransactions({String? category, String? merchant, String? sortField, String? sortOrder}) async{
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    if (category != null) {
      _categoryFilter = category.isNotEmpty ? category : null;
    }
    if (merchant != null) {
      _merchantFilter = merchant.isNotEmpty ? merchant : null;
    }
    if (sortField != null) {
      _sortField = sortField;
    }
    if (sortOrder != null) {
      _sortOrder = sortOrder;
    }

    try{
      _transactions = await _apiServices.getTransactions(
        category: _categoryFilter,
        merchant: _merchantFilter,
        sort: _sortField,
        order: _sortOrder,
      );
    }catch (e){
      _errorMessage = e.toString().replaceAll('Exception: ', '');
    }finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> clearFilters() async {
    _categoryFilter = null;
    _merchantFilter = null;
    await loadTransactions(sortField: _sortField, sortOrder: _sortOrder);
  }

  Future<bool> deleteTransaction(int id) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      await _apiServices.deleteTransaction(id);
      _transactions.removeWhere((item) => item.id == id);
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = e.toString().replaceAll('Exception: ', '');
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  // Toggle between USD and VND display. When switching to VND, fetch the latest rate from backend.
  Future<void> toggleCurrency() async {
    if (_displayCurrency == 'USD') {
      _isLoading = true;
      _errorMessage = null;
      notifyListeners();
      try {
        final rate = await _apiServices.getExchangeRate(base: 'USD', target: 'VND');
        _exchangeRate = rate;
        _displayCurrency = 'VND';
      } catch (e) {
        _errorMessage = e.toString().replaceAll('Exception: ', '');
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

  // Takes a raw expense description, sends it to the backend for processing, and updates the transaction list with the new expense. Returns true if successful, false otherwise.
  Future<bool> processRawExpense(String text) async{
    if(text.trim().isEmpty){
      return false;
    }
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();
    
    try{
      final newTransaction = await _apiServices.processExpense(text);
      _transactions.insert(0, newTransaction);
      _isLoading = false;
      notifyListeners();
      return true;
    } catch(e){
      _errorMessage = e.toString().replaceAll('Exception: ', '');
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

}
