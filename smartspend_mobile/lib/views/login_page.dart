import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:smartspend_mobile/core/network/api_error.dart'
    show userFriendlyMessage;
import 'package:smartspend_mobile/services/auth_service.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _loginController = TextEditingController();
  final _passwordController = TextEditingController();
  final _formKey = GlobalKey<FormState>();
  bool _isSubmitting = false;
  bool _obscurePassword = true;

  @override
  void dispose() {
    _loginController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    if (!_formKey.currentState!.validate()) return;
    if (_isSubmitting) return;
    setState(() => _isSubmitting = true);
    try {
      await context.read<AuthService>().login(
            _loginController.text.trim(),
            _passwordController.text,
          );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Signed in successfully'),
          backgroundColor: Color(0xFF16A34A),
          duration: Duration(seconds: 2),
        ),
      );
      await Navigator.of(context).pushReplacementNamed('/dashboard');
    } catch (e) {
      final message = userFriendlyMessage(e,
          fallback: 'Sign in failed. Please try again.');
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
    final accent = Theme.of(context).primaryColor;
    return Scaffold(
      backgroundColor: Colors.grey.shade50,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding:
                const EdgeInsets.symmetric(horizontal: 24, vertical: 40),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Icon(Icons.account_balance_wallet,
                      size: 56, color: accent),
                  const SizedBox(height: 16),
                  const Text(
                    'SmartSpend',
                    style:
                        TextStyle(fontSize: 28, fontWeight: FontWeight.w800),
                  ),
                  const SizedBox(height: 8),
                  Text('Welcome back — sign in to continue',
                      style: TextStyle(
                          color: Colors.grey.shade600, fontSize: 14)),
                  const SizedBox(height: 40),
                  TextFormField(
                    key: const ValueKey('loginField'),
                    controller: _loginController,
                    keyboardType: TextInputType.emailAddress,
                    autocorrect: false,
                    decoration: InputDecoration(
                      labelText: 'Email or Username',
                      hintText: 'you@example.com or your_username',
                      prefixIcon: const Icon(Icons.alternate_email_outlined),
                      border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12)),
                      filled: true,
                      fillColor: Colors.white,
                    ),
                    validator: (v) {
                      final value = (v ?? '').trim();
                      if (value.isEmpty) {
                        return 'Please enter your email or username';
                      }
                      if (value.length < 2) {
                        return 'Must be at least 2 characters';
                      }
                      return null;
                    },
                    onFieldSubmitted: (_) => _handleSubmit(),
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    key: const ValueKey('passwordField'),
                    controller: _passwordController,
                    obscureText: _obscurePassword,
                    decoration: InputDecoration(
                      labelText: 'Password',
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
                      if ((v ?? '').isEmpty) {
                        return 'Please enter your password';
                      }
                      if ((v ?? '').length < 6) {
                        return 'Password must be at least 6 characters';
                      }
                      return null;
                    },
                    onFieldSubmitted: (_) => _handleSubmit(),
                  ),
                  const SizedBox(height: 8),
                  Align(
                    alignment: Alignment.centerRight,
                    child: TextButton(
                      onPressed: () async {
                        await showDialog<void>(
                          context: context,
                          builder: (ctx) => AlertDialog(
                            title: const Text('Password recovery'),
                            content: const Text('Password recovery options are coming soon. Please contact support if you need help accessing your account.'),
                            actions: [
                              FilledButton(
                                onPressed: () => Navigator.of(ctx).pop(),
                                child: const Text('OK'),
                              ),
                            ],
                          ),
                        );
                      },
                      child: const Text('Forgot Password?'),
                    ),
                  ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      key: const ValueKey('loginButton'),
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
                          : const Text('Sign In',
                              style: TextStyle(fontSize: 16)),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text("Don't have an account?",
                          style: TextStyle(color: Colors.grey.shade600)),
                      TextButton(
                        onPressed: _isSubmitting
                            ? null
                            : () =>
                                Navigator.of(context).pushNamed('/register'),
                        child: const Text('Sign Up'),
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

