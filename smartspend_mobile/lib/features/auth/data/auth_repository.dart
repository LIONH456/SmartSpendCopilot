import 'package:flutter_secure_storage/flutter_secure_storage.dart';
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
    await api.register(
      username: username,
      email: email,
      password: password,
    );
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
