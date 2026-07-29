import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:smartspend_mobile/services/auth_service.dart';
import 'package:smartspend_mobile/view_models/expense_view_model.dart';
import 'package:smartspend_mobile/views/login_page.dart';
import 'package:smartspend_mobile/views/dashboard_view.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  final expenseViewModel = ExpenseViewModel();
  final authService = AuthService(expenseViewModel: expenseViewModel);
  runApp(SmartSpendApp(
    authService: authService,
    expenseViewModel: expenseViewModel,
  ));
}

class SmartSpendApp extends StatefulWidget {
  final AuthService authService;
  final ExpenseViewModel expenseViewModel;

  const SmartSpendApp({
    super.key,
    required this.authService,
    required this.expenseViewModel,
  });

  @override
  State<SmartSpendApp> createState() => _SmartSpendAppState();
}

class _SmartSpendAppState extends State<SmartSpendApp> {
  late Future<bool> _authCheck;

  @override
  void initState() {
    super.initState();
    _authCheck = widget.authService.isAuthenticated();
  }

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        Provider<AuthService>.value(value: widget.authService),
        ChangeNotifierProvider<ExpenseViewModel>.value(
          value: widget.expenseViewModel,
        ),
      ],
      child: MaterialApp(
        title: 'SmartSpend',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          primaryColor: const Color(0xFF16A34A),
          colorScheme: ColorScheme.fromSeed(
            seedColor: const Color(0xFF16A34A),
            primary: const Color(0xFF16A34A),
            surface: Colors.white,
          ),
          useMaterial3: true,
          fontFamily: 'Inter',
        ),
        home: FutureBuilder<bool>(
          future: _authCheck,
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.done) {
              final loggedIn = snapshot.data == true;
              return loggedIn ? const DashboardView() : LoginPage();
            }
            return const Scaffold(
              body: Center(
                child: CircularProgressIndicator(),
              ),
            );
          },
        ),
        routes: {
          '/login': (context) => LoginPage(),
          '/dashboard': (context) => const DashboardView(),
          '/register': (context) => const RegisterPage(),
        },
      ),
    );
  }
}
