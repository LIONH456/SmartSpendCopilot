import 'package:dio/dio.dart';

class AuthApi {
  final Dio dio;

  AuthApi({required this.dio});

  Future<Response> register({
    required String username,
    required String email,
    required String password,
  }) {
    return dio.post(
      '/api/auth/register',
      data: {
        'username': username,
        'email': email,
        'password': password,
      },
    );
  }

  Future<Response> login({
    required String login,
    required String password,
  }) {
    return dio.post(
      '/api/auth/login',
      data: {
        'login': login,
        'password': password,
      },
    );
  }
}
