import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:smartspend_mobile/core/network/api_error.dart' show userFriendlyMessage;
import 'package:smartspend_mobile/services/auth_service.dart';

class RegisterPage extends StatefulWidget {
  const RegisterPage({super.key});

  @override
  State<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends State<RegisterPage> {
  final _usernameController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmController = TextEditingController();
  final _formKey = GlobalKey<FormState>();
  bool _isSubmitting = false;
  bool _obscurePassword = true;
  bool _obscureConfirm = true;

  @override
  void dispose() {
    _usernameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _confirmController.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    if (!_formKey.currentState!.validate()) return;
    if (_isSubmitting) return;
    setState(() => _isSubmitting = true);
    try {
      await context.read<AuthService>().register(
            _usernameController.text.trim(),
            _emailController.text.trim(),
            _passwordController.text,
          );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Account created successfully'),
          backgroundColor: Color(0xFF16A34A),
          duration: Duration(seconds: 2),
        ),
      );
      await Navigator.of(context).pushReplacementNamed('/login');
    } catch (e) {
      final message = userFriendlyMessage(e,
          fallback: 'Sign up failed. Please try again.');
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(message),
          backgroundColor: const Color(0xFFDC2626),
          duration: const Duration(seconds: 4),
        ),
      );
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey.shade50,
      appBar: AppBar(backgroundColor: Colors.grey.shade50, elevation: 0),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding:
                const EdgeInsets.symmetric(horizontal: 24, vertical: 24),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Create your account',
                    style:
                        TextStyle(fontSize: 26, fontWeight: FontWeight.w800),
                  ),
                  const SizedBox(height: 8),
                  Text(
                      'Takes under a minute — smart bookkeeping starts here.',
                      style: TextStyle(
                          color: Colors.grey.shade600, fontSize: 14)),
                  const SizedBox(height: 28),
                  TextFormField(
                    key: const ValueKey('regUsernameField'),
                    controller: _usernameController,
                    autocorrect: false,
                    decoration: InputDecoration(
                      labelText: 'Username (3-20 chars)',
                      prefixIcon: const Icon(Icons.person_outline),
                      border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12)),
                      filled: true,
                      fillColor: Colors.white,
                    ),
                    validator: (v) {
                      final value = (v ?? '').trim();
                      if (value.isEmpty) return 'Please enter a username';
                      if (value.length < 3) return 'At least 3 characters';
                      if (value.length > 20) return 'At most 20 characters';
                      return null;
                    },
                    onFieldSubmitted: (_) => _handleSubmit(),
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    key: const ValueKey('regEmailField'),
                    controller: _emailController,
                    keyboardType: TextInputType.emailAddress,
                    autocorrect: false,
                    decoration: InputDecoration(
                      labelText: 'Email',
                      prefixIcon: const Icon(Icons.email_outlined),
                      border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12)),
                      filled: true,
                      fillColor: Colors.white,
                    ),
                    validator: (v) {
                      final value = (v ?? '').trim();
                      if (value.isEmpty) return 'Please enter your email';
                      if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$')
                          .hasMatch(value)) {
                        return 'Please enter a valid email address';
                      }
                      return null;
                    },
                    onFieldSubmitted: (_) => _handleSubmit(),
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    key: const ValueKey('regPasswordField'),
                    controller: _passwordController,
                    obscureText: _obscurePassword,
                    decoration: InputDecoration(
                      labelText:
                          'Password (8+ chars, upper, lower, digit, special)',
                      prefixIcon: const Icon(Icons.lock_outline),
                      suffixIcon: IconButton(
                        icon: Icon(_obscurePassword ? Icons.visibility_off : Icons.visibility),
                        onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                      ),
                      border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12)),
                      filled: true,
                      fillColor: Colors.white,
                    ),
                    validator: (v) {
                      final value = v ?? '';
                      if (value.isEmpty) return 'Please enter a password';
                      if (value.length < 8) return 'At least 8 characters';
                      final p = value;
                      final ok = p.contains(RegExp(r'[a-z]')) &&
                          p.contains(RegExp(r'[A-Z]')) &&
                          p.contains(RegExp(r'\d')) &&
                          p.contains(RegExp(r'[@$!%*?&]'));
                      if (!ok) {
                        return r'Needs upper, lower, digit, and @$!%*?&';
                      }
                      return null;
                    },
                    onFieldSubmitted: (_) => _handleSubmit(),
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    key: const ValueKey('regConfirmPasswordField'),
                    controller: _confirmController,
                    obscureText: _obscureConfirm,
                    decoration: InputDecoration(
                      labelText: 'Confirm password',
                      prefixIcon: const Icon(Icons.lock_outline),
                      suffixIcon: IconButton(
                        icon: Icon(_obscureConfirm ? Icons.visibility_off : Icons.visibility),
                        onPressed: () => setState(() => _obscureConfirm = !_obscureConfirm),
                      ),
                      border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12)),
                      filled: true,
                      fillColor: Colors.white,
                    ),
                    validator: (v) {
                      if ((v ?? '').isEmpty) return 'Please confirm password';
                      if (v != _passwordController.text) {
                        return 'Passwords do not match';
                      }
                      return null;
                    },
                    onFieldSubmitted: (_) => _handleSubmit(),
                  ),
                  const SizedBox(height: 24),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      key: const ValueKey('registerButton'),
                      onPressed: _isSubmitting ? null : _handleSubmit,
                      style: FilledButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12)),
                      ),
                      child: _isSubmitting
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(
                                  strokeWidth: 2, color: Colors.white),
                            )
                          : const Text('Create Account',
                              style: TextStyle(fontSize: 16)),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text('Already have an account?',
                          style: TextStyle(color: Colors.grey.shade600)),
                      TextButton(
                        onPressed: _isSubmitting
                            ? null
                            : () => Navigator.of(context).pop(),
                        child: const Text('Sign In'),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
