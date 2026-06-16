import 'package:flutter/foundation.dart';
import '../../features/auth/data/auth_repository.dart';

class AuthService extends ChangeNotifier {
  final AuthRepository _repository;
  bool _isAuthenticated = false;
  String? _token;

  AuthService({required AuthRepository repository}) : _repository = repository;

  bool get isAuthenticated => _isAuthenticated;
  String? get token => _token;

  Future<void> init() async {
    _token = await _repository.getToken();
    _isAuthenticated = _token != null;
    notifyListeners();
  }

  Future<void> register(String username, String email, String password) async {
    await _repository.register(
      username: username,
      email: email,
      password: password,
    );
  }

  Future<void> login(String login, String password) async {
    _token = await _repository.login(
      login: login,
      password: password,
    );
    _isAuthenticated = true;
    notifyListeners();
  }

  Future<void> logout() async {
    await _repository.logout();
    _token = null;
    _isAuthenticated = false;
    notifyListeners();
  }
}
