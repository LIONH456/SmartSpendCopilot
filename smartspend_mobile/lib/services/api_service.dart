// api_service.dart
// This service handles all interactions with the backend API, including fetching transactions and processing new expenses.

import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/transaction.dart';
import 'package:flutter/foundation.dart';

class ApiServices {
  // 10.0.2.2 is the special Android Loopback interface targeting host's localhost.
  // Switch to 'http://localhost:8080/api/transactions' if debugging on iOS Simulator.
  static const String baseUrl = "http://10.0.2.2:8080/api/transactions";

  Uri _buildUri(String path, {Map<String, String?>? queryParams}) {
    final uri = Uri.parse('$baseUrl$path');
    final filtered = <String, String>{};
    if (queryParams != null) {
      queryParams.forEach((key, value) {
        if (value != null && value.isNotEmpty) {
          filtered[key] = value;
        }
      });
    }
    return uri.replace(queryParameters: filtered);
  }

  // Fetches transactions from the backend API and returns a list of Transaction objects.
  Future<List<dynamic>> getTransactions({String? category, String? merchant, String sort = 'amount', String order = 'desc'}) async {
    try {
      final uri = _buildUri('', queryParams: {
        'category': category,
        'merchant': merchant,
        'sort': sort,
        'order': order,
      });
      final response = await http.get(uri);
      if (response.statusCode == 200) {
        List<dynamic> parsedJson = jsonDecode(response.body);
        return parsedJson.map((item) => Transaction.fromJson(item)).toList();
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
      final response = await http.post(
        Uri.parse('$baseUrl/process'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'description': description}),
      );
      if (response.statusCode == 200) {
        return Transaction.fromJson(jsonDecode(response.body));
      } else {
        throw Exception(
          'Failed to process expense: ${response.statusCode} ${response.reasonPhrase} - ${response.body}',
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
      final providerUri = Uri.https('api.exchangerate.host', '/latest', {
        'base': base,
        'symbols': target,
      });
      final providerResp = await http.get(providerUri);
      if (providerResp.statusCode == 200) {
        final data = jsonDecode(providerResp.body) as Map<String, dynamic>?;
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
      final uri = Uri.parse('$baseUrl/rate').replace(queryParameters: {
        'base': base,
        'target': target,
      });
      final response = await http.get(uri);
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>?;
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
      final response = await http.delete(Uri.parse('$baseUrl/$id'));
      if (response.statusCode != 204) {
        throw Exception('Delete failed: ${response.statusCode} ${response.body}');
      }
    } catch (e) {
      throw Exception('Failed to delete transaction: $e');
    }
  }
}
