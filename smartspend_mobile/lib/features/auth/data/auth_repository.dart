import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../../../core/network/api_error.dart';
import 'auth_api.dart';

class AuthRepository {
  final AuthApi api;
  final FlutterSecureStorage storage;

  AuthRepository({required this.api, required this.storage});

  static const String _tokenKey = 'auth_token';

  Future<void> register({
    required String username,
    required String email,
    required String password,
  }) async {
    final response = await api.register(
      username: username,
      email: email,
      password: password,
    );

    if (response.statusCode != 200 && response.statusCode != 201) {
      throw ApiError(
        status: response.statusCode ?? 0,
        code: 9001,
        error: 'Registration failed',
        message: 'Registration failed with HTTP ${response.statusCode}.',
        path: '/api/auth/register',
      );
    }
  }

  Future<String> login({
    required String login,
    required String password,
  }) async {
    final response = await api.login(
      login: login,
      password: password,
    );
    final token = response.data['token'] as String;
    await saveToken(token);
    return token;
  }

  Future<void> logout() async {
    await storage.delete(key: _tokenKey);
  }

  Future<String?> getToken() async {
    return await storage.read(key: _tokenKey);
  }

  Future<void> saveToken(String token) async {
    await storage.write(key: _tokenKey, value: token);
  }

  Future<bool> isAuthenticated() async {
    final token = await getToken();
    return token != null;
  }
}
