import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:dio/dio.dart';
import 'package:smartspend_mobile/core/network/dio_client.dart';
import 'package:smartspend_mobile/core/network/api_error.dart';
import 'package:smartspend_mobile/view_models/expense_view_model.dart';

/// JWT-persistent auth service.
///
/// Cross-account safety: every account boundary (login / register / logout)
/// explicitly invokes `ExpenseViewModel.clearLocalState()` so the next
/// authenticated user NEVER sees the previously-authenticated user's rows.
class AuthService {
  final DioClient _dioClient;
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();
  final ExpenseViewModel expenseViewModel;

  AuthService({DioClient? dioClient, required this.expenseViewModel})
      : _dioClient = dioClient ?? DioClient();

  Dio get dio => _dioClient.dio;

  Future<String?> _readToken() => _secureStorage.read(key: 'auth_token');
  Future<void> _writeToken(String t) =>
      _secureStorage.write(key: 'auth_token', value: t);
  Future<void> _deleteToken() => _secureStorage.delete(key: 'auth_token');

  Future<void> deleteToken() => _deleteToken();

  /// Backend `AuthResponse` only ships the `token` field.  The user's email &
  /// username are not echoed back on the auth HTTP body to save bytes, so we
  /// persist whatever the caller submitted on login/register.  That's fine:
  /// both values are user-controlled and never used for security decisions
  /// (only the JWT is authoritative).
  Future<void> _saveUserDetails({String? email, String? username}) async {
    await Future.wait([
      _secureStorage.write(key: 'user_email', value: (email ?? '').trim()),
      _secureStorage.write(
          key: 'user_username', value: (username ?? email ?? '').trim()),
    ]);
  }

  Future<String?> getUserEmail() => _secureStorage.read(key: 'user_email');
  Future<String?> getUsername() => _secureStorage.read(key: 'user_username');

  Future<bool> isAuthenticated() async {
    final token = await _readToken();
    final authenticated = token != null && token.trim().isNotEmpty;
    if (authenticated) {
      final username = await getUsername();
      expenseViewModel.setCurrentUser(username: username);
    } else {
      expenseViewModel.clearLocalState();
    }
    return authenticated;
  }

  Future<String?> _extractUsername(Map<String, dynamic> payload) async {
    final candidates = [
      payload['username'],
      payload['userName'],
      payload['name'],
      (payload['data'] is Map<String, dynamic>)
          ? payload['data']['username'] ?? payload['data']['userName']
          : null,
    ];
    for (final raw in candidates) {
      if (raw == null) continue;
      final str = raw.toString().trim();
      if (str.isNotEmpty) return Future.value(str);
    }
    return null;
  }

  Future<String> _extractRawToken(Map<String, dynamic> payload) {
    // Try top-level token first, then common wrappers -> nested .data.token.
    final candidates = [
      payload['token'],
      payload['accessToken'],
      payload['access_token'],
      (payload['data'] is Map<String, dynamic>)
          ? payload['data']['token'] ?? payload['data']['accessToken']
          : null,
    ];
    for (final raw in candidates) {
      if (raw == null) continue;
      final str = raw.toString().trim();
      if (str.isNotEmpty) return Future.value(str);
    }
    throw ApiError(
      status: 500,
      code: 1005,
      error: 'Token Missing',
      message: 'Server did not return an authentication token.',
      path: '/auth/login',
    );
  }

  /// Accepts `login` as EITHER an email address OR a username — matches the
  /// backend `LoginRequest.login: @NotBlank String` contract.
  Future<String> login(String login, String password) async {
    final trimmedLogin = login.trim();
    try {
      final response = await dio.post(
        '/api/auth/login',
        data: {
          // ⚠️ Backend key MUST be "login" (NOT "email").
          // Accepts both:  user@example.com  OR  the_user_name
          'login': trimmedLogin,
          'password': password,
        },
        options: Options(
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
          },
          // 200..399 -> valid (200 OK, 201 Created, 302 Found, ...)
          validateStatus: (s) => s != null && s >= 200 && s < 400,
        ),
      );
      final data = response.data as Map<String, dynamic>;
      final token = await _extractRawToken(data);
      final username = await _extractUsername(data);
      expenseViewModel.clearLocalState();
      expenseViewModel.setCurrentUser(username: username);
      await _writeToken(token);
      // Persist user details: backend AuthResponse now includes username when available.
      final looksLikeEmail = trimmedLogin.contains('@');
      await _saveUserDetails(
        email: looksLikeEmail ? trimmedLogin : null,
        username: username ?? (looksLikeEmail ? null : trimmedLogin),
      );
      return token;
    } on DioException catch (e) {
      throw ApiError.fromDioException(e);
    } catch (e) {
      throw ApiError(
        status: 500,
        code: 1003,
        error: 'Unexpected Error',
        message:
            userFriendlyMessage(e, fallback: 'An unexpected error occurred during sign in.'),
        path: '/api/auth/login',
      );
    }
  }

  Future<void> register(
      String username, String email, String password) async {
    try {
      final response = await dio.post(
        '/api/auth/register',
        data: {
          'username': username.trim(),
          'email': email.trim(),
          'password': password,
        },
        options: Options(
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
          },
          validateStatus: (s) => s != null && s >= 200 && s < 400,
        ),
      );
      if (response.statusCode == 200 || response.statusCode == 201) {
        expenseViewModel.clearLocalState();
        await _saveUserDetails(email: email.trim(), username: username.trim());
        expenseViewModel.setCurrentUser(username: username.trim());
        return;
      }
      final body = response.data;
      if (body is Map<String, dynamic>) {
        throw ApiError.fromJson(body);
      }
      throw ApiError(
        status: response.statusCode ?? 0,
        code: 9001,
        error: 'Unexpected Status',
        message: 'Registration failed with HTTP ${response.statusCode}.',
        path: '/api/auth/register',
      );
    } on DioException catch (e) {
      throw ApiError.fromDioException(e);
    } catch (e) {
      throw ApiError(
        status: 500,
        code: 1002,
        error: 'Unexpected Error',
        message: userFriendlyMessage(e,
            fallback: 'An unexpected error occurred during sign up.'),
        path: '/api/auth/register',
      );
    }
  }

  Future<void> resetPassword(String oldPassword, String newPassword) async {
    final token = await _readToken();
    try {
      final response = await dio.put(
        '/api/auth/reset-password',
        data: {
          'oldPassword': oldPassword,
          'newPassword': newPassword,
        },
        options: Options(
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            if (token != null && token.trim().isNotEmpty)
              'Authorization': 'Bearer ${token.trim()}',
          },
          validateStatus: (s) => s != null && s >= 200 && s < 400,
        ),
      );
      if (response.statusCode == 200 || response.statusCode == 201) {
        return;
      }
      final body = response.data;
      if (body is Map<String, dynamic>) {
        throw ApiError.fromJson(body);
      }
      throw ApiError(
        status: response.statusCode ?? 0,
        code: 9001,
        error: 'Unexpected Status',
        message: 'Password reset failed with HTTP ${response.statusCode}.',
        path: '/api/auth/reset-password',
      );
    } on DioException catch (e) {
      throw ApiError.fromDioException(e);
    } catch (e) {
      throw ApiError(
        status: 500,
        code: 1003,
        error: 'Unexpected Error',
        message: userFriendlyMessage(e,
            fallback: 'An unexpected error occurred while resetting your password.'),
        path: '/api/auth/reset-password',
      );
    }
  }

  Future<void> logout() async {
    expenseViewModel.clearLocalState();
    try {
      final token = await _readToken();
      if (token != null && token.trim().isNotEmpty) {
        await dio.post(
          '/api/auth/logout',
          options: Options(
            headers: {
              'Accept': 'application/json',
              'Authorization': 'Bearer $token',
            },
            validateStatus: (_) => true,
          ),
        );
      }
    } catch (_) {
      // server-side revocation is best-effort
    }
    await _deleteToken();
    await Future.wait([
      _secureStorage.delete(key: 'user_email'),
      _secureStorage.delete(key: 'user_username'),
    ]);
  }
}
