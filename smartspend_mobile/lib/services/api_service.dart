// Handles all HTTP interactions with the SmartSpend backend through a
// platform-aware Dio instance created via DioClient.
//
// KEY ARCHITECTURAL CHANGE:
//   ALL requests use RELATIVE paths (e.g. '/api/transactions') instead of
//   absolute URLs. This way the platform-aware baseUrl configured inside
//   DioClient is used for EVERY call — fixing the iOS Simulator "10.0.2.2
//   does not exist" bug, and ensuring Android Emulator continues to work.
//
// RESPONSE-SHAPE COMPATIBILITY:
//   GET /api/transactions returns PaginatedResponse -> we read the `.content`
//   array.  We tolerate a bare array as a fallback for older backends.

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../core/network/dio_client.dart';
import '../core/network/api_error.dart';
import '../models/transaction.dart';

class ApiServices {
  late final Dio dio;

  ApiServices() {
    const storage = FlutterSecureStorage();
    dio = DioClient.createDio(storage);
  }

  // -------------------------------------------------------------------------
  // Private helpers: structured error re-throw + PaginatedResponse unpacking
  // -------------------------------------------------------------------------

  Never _throwStructured(dynamic error, String fallbackPath) {
    if (error is DioException && error.error is ApiError) {
      throw error.error as ApiError;
    }
    if (error is DioException) {
      final data = error.response?.data;
      if (data is Map<String, dynamic>) {
        throw ApiError.fromJson(data);
      }
    }
    if (error is ApiError) throw error;
    throw ApiError(
      status: 0,
      code: 9001,
      error: 'Client Error',
      message: error.toString().replaceAll('Exception: ', '').trim(),
      path: fallbackPath,
    );
  }

  List<Transaction> _unpackTransactions(dynamic payload) {
    List<dynamic> list;
    if (payload is List) {
      list = payload;
    } else if (payload is Map<String, dynamic>) {
      // PaginatedResponse { content, page, size, totalElements, ... }
      final c = payload['content'];
      if (c is List) {
        list = c;
      } else if (payload['transactions'] is List) {
        list = payload['transactions'] as List<dynamic>;
      } else {
        // Tolerate a single-object endpoint response: wrap as 1-element list
        list = <dynamic>[payload];
      }
    } else {
      throw ApiError(
        status: 500,
        code: 9001,
        error: 'Malformed Response',
        message: 'The server returned malformed data. Please try again later.',
        path: '/api/transactions',
      );
    }
    return list
        .whereType<Map<String, dynamic>>()
        .map((item) => Transaction.fromJson(item))
        .toList();
  }

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /// Fetches the currently authenticated user's transactions with optional
  /// category/merchant filtering and server-side pagination.
  Future<List<Transaction>> getTransactions({
    String? category,
    String? merchant,
    String sort = 'amount',
    String order = 'desc',
    int page = 0,
    int size = 10,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        'sort': sort,
        'order': order,
      };
      if (category != null && category.trim().isNotEmpty) {
        queryParams['category'] = category.trim();
      }
      if (merchant != null && merchant.trim().isNotEmpty) {
        queryParams['merchant'] = merchant.trim();
      }

      final response = await dio.get<dynamic>(
        '/api/transactions',
        queryParameters: queryParams,
      );
      if (response.statusCode == 200) {
        // NOTE: response.data will be either Map<String,dynamic> (PaginatedResponse)
        // or List<dynamic> depending on the Dart JSON decoder.  Both are
        // handled by _unpackTransactions.
        return _unpackTransactions(response.data);
      }
      throw ApiError(
        status: response.statusCode ?? 0,
        code: 9001,
        error: 'Unexpected Status',
        message: 'Unexpected HTTP status ${response.statusCode}',
        path: '/api/transactions',
      );
    } catch (e) {
      throw _throwStructured(e, '/api/transactions');
    }
  }

  /// NEW — Batch parser endpoint.  Parses one line that may contain
  /// multi-action descriptions (and/or/comma/newline/multi-amount) into a
  /// list of 1..N transactions.  Always returns a List of Transaction objects.
  Future<List<Transaction>> processExpenseBatch(String description) async {
    final trimmed = description.trim();
    if (trimmed.isEmpty) {
      throw ApiError(
        status: 400,
        code: 5002,
        error: 'Bad Request',
        message: 'Description cannot be blank',
        path: '/api/transactions/process-batch',
      );
    }
    try {
      final response = await dio.post<dynamic>(
        '/api/transactions/process-batch',
        data: {'description': trimmed},
      );
      if (response.statusCode == 200) {
        final list = _unpackTransactions(response.data);
        if (list.isEmpty) {
          throw ApiError(
            status: 500,
            code: 4001,
            error: 'AI Parsing Failed',
            message: 'No transactions could be parsed from that description.',
            path: '/api/transactions/process-batch',
          );
        }
        return list;
      }
      throw ApiError(
        status: response.statusCode ?? 0,
        code: 9001,
        error: 'Unexpected Status',
        message: 'Unexpected HTTP status ${response.statusCode}',
        path: '/api/transactions/process-batch',
      );
    } catch (e) {
      throw _throwStructured(e, '/api/transactions/process-batch');
    }
  }

  /// Legacy single-item parser (kept for backwards compatibility).
  /// Delegates to the batch endpoint internally.
  Future<Transaction> processExpense(String description) async {
    final batch = await processExpenseBatch(description);
    return batch.first;
  }

  /// Exchange rate — 3-tier fallback (third party API → backend endpoint →
  /// hardcoded 25000 USD/VND).  Mirrors the backend ExchangeRateService chain.
  Future<double> getExchangeRate({
    String base = 'USD',
    String target = 'VND',
  }) async {
    final b = base.trim().toUpperCase();
    final t = target.trim().toUpperCase();
    if (b == t) return 1.0;

    const defaultUsdToVnd = 25000.0;
    const defaultVndToUsd = 1.0 / 25000.0;

    // Tier 1 — public third-party exchangerate.host
    try {
      final response = await dio.get<Map<String, dynamic>>(
        'https://api.exchangerate.host/latest',
        queryParameters: {'base': b, 'symbols': t},
      );
      if (response.statusCode == 200 && response.data is Map) {
        final rates = (response.data as Map)['rates'];
        if (rates is Map) {
          final rateVal = rates[t];
          if (rateVal is num) return rateVal.toDouble();
        }
      }
    } catch (e) {
      if (kDebugMode) print('exchangerate.host tier skipped: $e');
    }

    // Tier 2 — our own backend /api/transactions/rate
    try {
      final response = await dio.get<Map<String, dynamic>>(
        '/api/transactions/rate',
        queryParameters: {'base': b, 'target': t},
      );
      if (response.statusCode == 200 && response.data is Map) {
        final rateVal = (response.data as Map)['rate'];
        if (rateVal is num) return rateVal.toDouble();
        if (rateVal != null) return double.tryParse('$rateVal') ?? 0;
      }
    } catch (e) {
      if (kDebugMode) print('backend /rate tier skipped: $e');
    }

    // Tier 3 — absolute hardcoded fallback
    if (b == 'USD' && t == 'VND') return defaultUsdToVnd;
    if (b == 'VND' && t == 'USD') return defaultVndToUsd;
    throw ApiError(
      status: 400,
      code: 3001,
      error: 'Bad Request',
      message: 'Unsupported currency pair: $b -> $t',
      path: '/api/transactions/rate',
    );
  }

  Future<Transaction> updateTransaction(int id, Transaction transaction) async {
    try {
      final response = await dio.put<Map<String, dynamic>>(
        '/api/transactions/$id',
        data: {
          'amount': transaction.amount,
          'category': transaction.category,
          'merchant': transaction.merchant,
          'currency': transaction.currency,
          'originalDescription': transaction.originalDescription,
        },
      );
      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        return Transaction.fromJson(response.data! as Map<String, dynamic>);
      }
      throw ApiError(
        status: response.statusCode ?? 0,
        code: 9001,
        error: 'Update Failed',
        message: 'Failed to update transaction (HTTP ${response.statusCode})',
        path: '/api/transactions/$id',
      );
    } catch (e) {
      throw _throwStructured(e, '/api/transactions/$id');
    }
  }

  Future<void> deleteTransaction(int id) async {
    try {
      final response = await dio.delete<void>('/api/transactions/$id');
      if (response.statusCode != 204) {
        throw ApiError(
          status: response.statusCode ?? 0,
          code: 9001,
          error: 'Delete Failed',
          message: 'Failed to delete transaction (HTTP ${response.statusCode})',
          path: '/api/transactions/$id',
        );
      }
    } catch (e) {
      throw _throwStructured(e, '/api/transactions/$id');
    }
  }
}
