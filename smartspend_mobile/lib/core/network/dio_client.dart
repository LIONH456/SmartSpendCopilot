import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'api_error.dart';

/// Creates a pre-configured Dio instance with:
///   • Platform-aware base URL (Android emulator 10.0.2.2 / iOS Simulator localhost / Web localhost / desktop localhost)
///   • Automatic JWT Authorization header injection via FlutterSecureStorage
///   • Structured error interceptor that converts every non-2xx response OR
///     network failure into a rich ApiError payload so the UI never sees a
///     generic "Network error, check connection" again.
class DioClient {
  static const String _tokenKey = 'auth_token';
  final Dio dio;

  factory DioClient({FlutterSecureStorage? storage}) {
    final s = storage ?? const FlutterSecureStorage();
    return DioClient._(storage: s);
  }

  DioClient._({required FlutterSecureStorage storage})
      : dio = createDio(storage);

  /// Resolves the backend host based on the runtime platform.
  ///
  /// Background:
  ///   • Android emulator runs behind a virtual NAT router — to reach the
  ///     development machine's localhost you MUST use the alias 10.0.2.2.
  ///   • iOS Simulator shares the host machine's network stack — localhost
  ///     works directly (as it does on Web / Linux / macOS / Windows).
  ///   • Using defaultTargetPlatform from foundation is safe even on Web and
  ///     works without importing dart:io.
  static String get baseUrl {
    if (kIsWeb) return 'http://localhost:8080';

    const isRealDevice = false;
    const computerIp = 'http://192.168.1.97';
    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        // return 'http://10.0.2.2:8080';
        return isRealDevice ? '$computerIp:8080' : 'http://10.0.2.2:8080';
      case TargetPlatform.iOS:
      case TargetPlatform.macOS:
      case TargetPlatform.linux:
      case TargetPlatform.windows:
      case TargetPlatform.fuchsia:
        return 'http://localhost:8080';
    }
  }

  static Dio createDio(FlutterSecureStorage storage) {
    final dio = Dio(BaseOptions(
      baseUrl: baseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 15),
      sendTimeout: const Duration(seconds: 10),
      headers: {
        Headers.acceptHeader: Headers.jsonContentType,
        Headers.contentTypeHeader: Headers.jsonContentType,
      },
    ));

    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        // Inject the JWT bearer token from secure storage if available.
        // The backend @RestControllerAdvice + JwtAuthenticationFilter then
        // either accepts the call or returns a rich ApiErrorResponse.
        try {
          final token = await storage.read(key: _tokenKey);
          if (token != null && token.trim().isNotEmpty) {
            options.headers['Authorization'] = 'Bearer ${token.trim()}';
          }
        } catch (_) {
          // Storage misbehaving? Continue without the token — caller will
          // get a 401 which the onError branch below will pack as ApiError.
        }
        return handler.next(options);
      },
      onError: (error, handler) {
        final res = error.response;
        ApiError structured;
        try {
          if (res != null && res.data is Map<String, dynamic>) {
            structured = ApiError.fromJson(res.data as Map<String, dynamic>);
          } else {
            // No structured body: synthesize an ApiError from the Dio type.
            structured = ApiError(
              status: res?.statusCode ?? 0,
              code: 9001,
              error: 'Network Error',
              message: _fallbackMessageFor(error.type),
              path: error.requestOptions.path,
            );
          }
        } catch (e) {
          // Absolute safety net: never let the onError interceptor itself
          // throw — if that happened, the app would crash on every failure.
          structured = ApiError(
            status: 0,
            code: 9001,
            error: 'Unknown Error',
            message: _fallbackMessageFor(error.type),
            path: error.requestOptions.path,
          );
        }
        // Reject with the ORIGINAL DioException but its .error payload replaced
        // with the structured ApiError. This preserves request metadata while
        // letting any try/catch branch do: if (e.error is ApiError) { ... }
        return handler.reject(DioException(
          requestOptions: error.requestOptions,
          response: res,
          error: structured,
          type: error.type,
          stackTrace: error.stackTrace,
          message: error.message,
        ));
      },
    ));

    return dio;
  }

  static String _fallbackMessageFor(DioExceptionType t) {
    switch (t) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.receiveTimeout:
      case DioExceptionType.sendTimeout:
        return 'Request timed out. Please try again.';
      case DioExceptionType.connectionError:
        return 'Cannot connect to server. Check your internet or backend URL.';
      case DioExceptionType.badCertificate:
        return 'Security certificate error.';
      case DioExceptionType.badResponse:
        return 'Server returned an unexpected response.';
      case DioExceptionType.cancel:
        return 'Request was cancelled.';
      case DioExceptionType.unknown:
        return 'Network error, please check your connection.';
    }
  }
}
