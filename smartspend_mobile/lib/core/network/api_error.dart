import 'package:dio/dio.dart';

/// Structured error that mirrors the backend ApiErrorResponse shape:
/// { "timestamp": ..., "status": 400, "code": 1001, "error": "Bad Request",
///   "message": "Username already exists", "path": "/api/auth/register" }
class ApiError {
  final int status;
  final int code;
  final String error;
  final String message;
  final String path;
  final Map<String, dynamic>? details;

  ApiError({
    required this.status,
    required this.code,
    required this.error,
    required this.message,
    required this.path,
    this.details,
  });

  factory ApiError.fromJson(Map<String, dynamic> json) {
    return ApiError(
      status: (json['status'] as num?)?.toInt() ?? 0,
      code: (json['code'] as num?)?.toInt() ?? 9001,
      error: json['error'] as String? ?? 'Unknown',
      message: json['message'] as String? ?? 'An unknown error occurred',
      path: json['path'] as String? ?? '',
      details: json['details'] is Map<String, dynamic> ? json['details'] as Map<String, dynamic> : null,
    );
  }

  /// Unpack a DioException into an ApiError, preferring the response body if it
  /// already contains the canonical ApiErrorResponse shape.
  factory ApiError.fromDioException(DioException dioError) {
    final data = dioError.response?.data;
    final path = (dioError.requestOptions.path).toString();
    if (data is Map<String, dynamic>) {
      try {
        return ApiError.fromJson(data);
      } catch (_) {
        // fall through to type-based fallback below
      }
    }
    final status = dioError.response?.statusCode ?? 0;
    switch (dioError.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return ApiError(
          status: status,
          code: 9010,
          error: 'Timeout',
          message: 'Request timed out. Please try again.',
          path: path,
        );
      case DioExceptionType.connectionError:
      case DioExceptionType.badCertificate:
        return ApiError(
          status: status,
          code: 9011,
          error: 'Network Error',
          message: 'Cannot connect to server. Check your internet or backend URL.',
          path: path,
        );
      case DioExceptionType.badResponse:
        if (status == 401) {
          return ApiError(
            status: status,
            code: 1004,
            error: 'Unauthorized',
            message: 'Login expired. Please sign in again.',
            path: path,
          );
        }
        if (status == 403) {
          return ApiError(
            status: status,
            code: 1007,
            error: 'Forbidden',
            message: 'You do not have permission to perform this action.',
            path: path,
          );
        }
        if (status == 404) {
          return ApiError(
            status: status,
            code: 1006,
            error: 'Not Found',
            message: 'The requested resource was not found.',
            path: path,
          );
        }
        if (status >= 500) {
          return ApiError(
            status: status,
            code: 5000,
            error: 'Server Error',
            message: 'Server error. Please try again later.',
            path: path,
          );
        }
        return ApiError(
          status: status,
          code: 9012,
          error: 'Bad Response',
          message: 'Server returned an unexpected response (HTTP $status).',
          path: path,
        );
      case DioExceptionType.cancel:
        return ApiError(
          status: status,
          code: 9013,
          error: 'Cancelled',
          message: 'Request was cancelled.',
          path: path,
        );
      case DioExceptionType.unknown:
        return ApiError(
          status: status,
          code: 9001,
          error: 'Unexpected Error',
          message:
              (dioError.message ?? '').trim().isEmpty ? 'Unknown error.' : dioError.message!,
          path: path,
        );
    }
  }

  bool get isNetworkIssue =>
      status == 0 || (code == 9001 && message.trim().isEmpty);

  @override
  String toString() => 'ApiError($code/$status): $message';
}

/// Unpacks ANY error shape (ApiError, DioException, Map, Exception, String)
/// into a single user-facing string. Falls back to a generic "Network error"
/// message ONLY when every other structured channel is empty.
String userFriendlyMessage(
  dynamic error, {
  String fallback = 'Network error, please check your connection',
}) {
  try {
    // Case 1: already a structured ApiError (thrown by our interceptor / ApiServices)
    if (error is ApiError) return error.message;

    // Case 2: DioException — most common path. Always inspect the response body
    // first, because the server almost always returns a rich ApiErrorResponse.
    if (error is DioException) {
      final data = error.response?.data;
      if (data is Map<String, dynamic>) {
        return ApiError.fromJson(data).message;
      }
      if (data is List) {
        final first = data.isNotEmpty ? data.first : null;
        if (first is Map<String, dynamic>) {
          final msg = first['message'];
          if (msg is String && msg.isNotEmpty) return msg;
        }
      }
      if (data is String && data.trim().isNotEmpty) return data.trim();

      // No response body: categorize by DioExceptionType for user-friendliness
      switch (error.type) {
        case DioExceptionType.connectionTimeout:
        case DioExceptionType.receiveTimeout:
        case DioExceptionType.sendTimeout:
          return 'Request timed out. Please try again.';
        case DioExceptionType.connectionError:
          return 'Cannot reach server. Check your internet or the backend URL.';
        case DioExceptionType.badCertificate:
          return 'Security certificate error. Please contact support.';
        case DioExceptionType.badResponse:
          final sc = error.response?.statusCode ?? 0;
          if (sc == 401) return 'Login expired — please sign in again.';
          if (sc == 403) return 'You do not have permission to do that.';
          if (sc == 404) return 'The requested resource was not found.';
          if (sc >= 500) return 'Server error — please try again later.';
          return 'Server returned an invalid response (HTTP $sc).';
        case DioExceptionType.cancel:
          return 'Request was cancelled.';
        case DioExceptionType.unknown:
          final msg = error.message ?? '';
          if (msg.toLowerCase().contains('socket') ||
              msg.toLowerCase().contains('failed host')) {
            return 'Cannot connect to server. Check your backend URL & internet.';
          }
          if (msg.isNotEmpty) return msg;
          return fallback;
      }
    }

    // Case 3: bare Map (old code paths or mocks)
    if (error is Map<String, dynamic>) {
      final msg = error['message'];
      if (msg is String && msg.isNotEmpty) return msg;
      final hint = error['hint'];
      if (hint is String && hint.isNotEmpty) return hint;
      final e = error['error'];
      if (e is String && e.isNotEmpty) return e;
    }

    // Case 4: nested Exception -> String -> Fallback toString
    if (error is Exception) {
      final s = error.toString().replaceAll('Exception: ', '').trim();
      if (s.isNotEmpty) return s;
    }
    if (error is String && error.trim().isNotEmpty) return error.trim();
  } catch (e) {
    // NEVER crash inside the error formatter itself — that's a UX disaster.
  }
  return fallback;
}
