import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:provider/provider.dart';
import 'core/network/dio_client.dart';
import 'core/services/auth_service.dart';
import 'features/auth/data/auth_api.dart';
import 'features/auth/data/auth_repository.dart';
import 'features/auth/presentation/login_page.dart';
import 'views/dashboard_view.dart';
import 'view_models/expense_view_model.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final storage = const FlutterSecureStorage();
  final dio = DioClient.createDio(storage);
  final api = AuthApi(dio: dio);
  final repository = AuthRepository(api: api, storage: storage);
  final authService = AuthService(repository: repository);

  await authService.init();

  final ExpenseViewModel expenseViewModel = ExpenseViewModel();
  expenseViewModel.initExchange(providerIsRateLimited: false);

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => authService),
        Provider<ExpenseViewModel>(create: (_) => expenseViewModel),
      ],
      child: SmartSpendApp(
        authService: authService,
        expenseViewModel: expenseViewModel,
      ),
    ),
  );
}

class SmartSpendApp extends StatelessWidget {
  final AuthService authService;
  final ExpenseViewModel expenseViewModel;

  const SmartSpendApp({super.key, required this.authService, required this.expenseViewModel});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SmartSpend AI',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        brightness: Brightness.dark,
        fontFamily: 'Roboto',
      ),
      initialRoute: '/',
      routes: {
        '/': (context) => Consumer<AuthService>(
          builder: (context, authService, _) {
            return authService.isAuthenticated
                ? DashboardView(viewModel: expenseViewModel)
                : const LoginPage();
          },
        ),
        '/dashboard': (context) => DashboardView(viewModel: expenseViewModel),
        '/login': (context) => const LoginPage(),
      },
    );
  }
}
