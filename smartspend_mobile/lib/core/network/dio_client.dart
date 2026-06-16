import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class DioClient {
  // For Android emulator, use 10.0.2.2 to access host's localhost
  static const String baseUrl = kIsWeb
      ? 'http://localhost:8080'
      : 'http://10.0.2.2:8080';
  static const String _tokenKey = 'auth_token';

  static Dio createDio(FlutterSecureStorage storage) {
    final dio = Dio(BaseOptions(
      baseUrl: baseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 10),
      headers: {
        'Content-Type': 'application/json',
      },
    ));

    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        final token = await storage.read(key: _tokenKey);
        if (token != null) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        return handler.next(options);
      },
      onError: (error, handler) {
        if (error.response != null) {
          final statusCode = error.response?.statusCode;
          if (statusCode == 401) {
            // Token 过期或无效，可以在这里处理跳转登录
          }
          return handler.reject(DioException(
            requestOptions: error.requestOptions,
            response: error.response,
            error: error.response?.data,
          ));
        }
        return handler.next(error);
      },
    ));

    return dio;
  }
}
