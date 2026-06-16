// api_service.dart
// This service handles all interactions with the backend API, including fetching transactions and processing new expenses.

import 'package:dio/dio.dart';
import '../models/transaction.dart';
import 'package:flutter/foundation.dart';
import '../core/network/dio_client.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class ApiServices {
  // 10.0.2.2 is the special Android Loopback interface targeting host's localhost.
  // Switch to 'http://localhost:8080/api/transactions' if debugging on iOS Simulator.
  static const String baseUrl = "http://10.0.2.2:8080/api/transactions";
  static const String apiBaseUrl = "http://10.0.2.2:8080";

  late final Dio dio;

  ApiServices() {
    const storage = FlutterSecureStorage();
    dio = DioClient.createDio(storage);
  }

  // Fetches transactions from the backend API and returns a list of Transaction objects.
  Future<List<dynamic>> getTransactions({String? category, String? merchant, String sort = 'amount', String order = 'desc'}) async {
    try {
      final queryParams = <String, String>{};
      if (category != null && category.isNotEmpty) queryParams['category'] = category;
      if (merchant != null && merchant.isNotEmpty) queryParams['merchant'] = merchant;
      queryParams['sort'] = sort;
      queryParams['order'] = order;

      final response = await dio.get(baseUrl, queryParameters: queryParams);
      if (response.statusCode == 200) {
        return (response.data as List<dynamic>).map((item) => Transaction.fromJson(item)).toList();
      } else {
        throw Exception(
          'Server failed to respond with status code: ${response.statusCode}',
        );
      }
    } catch (e) {
      throw Exception('Failed to establish connection to backend: $e');
    }
  }

  // Sends a transaction description to the backend for processing and returns the resulting Transaction object.
  Future<Transaction> processExpense(String description) async {
    try {
      final response = await dio.post(
        '$baseUrl/process',
        data: {'description': description},
      );
      if (response.statusCode == 200) {
        return Transaction.fromJson(response.data);
      } else {
        throw Exception(
          'Failed to process expense: ${response.statusCode} - ${response.data}',
        );
      }
    } catch (e) {
      throw Exception('Backend request failed: $e');
    }
  }

  Future<double> getExchangeRate({String base = 'USD', String target = 'VND'}) async {
    if (base.toUpperCase() == target.toUpperCase()) {
      return 1.0;
    }

    const defaultUsdToVnd = 25000.0;
    const defaultVndToUsd = 1.0 / 25000.0;

    try {
      final response = await dio.get(
        'https://api.exchangerate.host/latest',
        queryParameters: {
          'base': base,
          'symbols': target,
        },
      );
      if (response.statusCode == 200) {
        final data = response.data as Map<String, dynamic>?;
        final rates = data?['rates'] as Map<String, dynamic>?;
        final rateVal = rates?[target.toUpperCase()];
        if (rateVal is num) {
          return rateVal.toDouble();
        }
      }
    } catch (e) {
      if (kDebugMode) print('getExchangeRate provider error: $e');
    }

    try {
      final response = await dio.get(
        '$baseUrl/rate',
        queryParameters: {
          'base': base,
          'target': target,
        },
      );
      if (response.statusCode == 200) {
        final data = response.data as Map<String, dynamic>?;
        final rateVal = data?['rate'];
        if (rateVal is num) return rateVal.toDouble();
        if (rateVal != null) return double.parse(rateVal.toString());
      }
    } catch (e) {
      if (kDebugMode) print('getExchangeRate backend error: $e');
    }

    if (base.toUpperCase() == 'USD' && target.toUpperCase() == 'VND') {
      return defaultUsdToVnd;
    }
    if (base.toUpperCase() == 'VND' && target.toUpperCase() == 'USD') {
      return defaultVndToUsd;
    }

    throw Exception('Unsupported currency pair for fallback: $base -> $target');
  }

  Future<void> deleteTransaction(int id) async {
    try {
      final response = await dio.delete('$baseUrl/$id');
      if (response.statusCode != 204) {
        throw Exception('Delete failed: ${response.statusCode} ${response.data}');
      }
    } catch (e) {
      throw Exception('Failed to delete transaction: $e');
    }
  }
}
