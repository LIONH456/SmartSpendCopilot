import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:smartspend_mobile/core/network/api_error.dart';
import 'package:smartspend_mobile/models/transaction.dart';
import 'package:smartspend_mobile/services/auth_service.dart';
import 'package:smartspend_mobile/view_models/expense_view_model.dart';
import 'package:smartspend_mobile/views/login_page.dart';

class DashboardView extends StatefulWidget {
  const DashboardView({super.key});

  @override
  State<DashboardView> createState() => _DashboardViewState();
}

class _DashboardViewState extends State<DashboardView> {
  final TextEditingController _inputController = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final vm = context.read<ExpenseViewModel>();
      vm.loadTransactions();
      vm.initExchange(providerIsRateLimited: true);
    });
  }

  @override
  void dispose() {
    _inputController.dispose();
    super.dispose();
  }

  void _handleClearInput() {
    final vm = context.read<ExpenseViewModel>();
    vm.clearInputErrorIfAny();
    _inputController.clear();
  }

  Future<void> _handleSubmit() async {
    final text = _inputController.text.trim();
    if (text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Please enter an expense description first'),
          backgroundColor: Color(0xFF0F172A),
          duration: Duration(seconds: 2),
        ),
      );
      return;
    }
    final vm = context.read<ExpenseViewModel>();
    final created = await vm.processRawExpense(text);

    if (!mounted) return;
    if (created > 0) {
      _inputController.clear();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Recorded $created transaction(s)'),
          backgroundColor: const Color(0xFF16A34A),
          duration: const Duration(seconds: 2),
        ),
      );
    } else {
      final inline = vm.errorMessage;
      final fallback = userFriendlyMessage(inline,
          fallback: 'AI failed to parse that. Please rephrase and retry.');
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(fallback),
          backgroundColor: const Color(0xFFDC2626),
          duration: const Duration(seconds: 5),
        ),
      );
    }
  }

  Future<void> _handleLogout({
    required AuthService auth,
    required NavigatorState nav,
  }) async {
    await auth.logout();
    if (!mounted) return;
    nav.pushReplacement(
      MaterialPageRoute(builder: (_) => const LoginPage()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final accent = Theme.of(context).primaryColor;
    return Scaffold(
      backgroundColor: Colors.grey.shade50,
      appBar: AppBar(
        backgroundColor: Colors.grey.shade50,
        elevation: 0,
        title: Row(
          children: [
            Icon(Icons.account_balance_wallet, color: accent),
            const SizedBox(width: 8),
            const Text('SmartSpend',
                style: TextStyle(fontWeight: FontWeight.w800)),
          ],
        ),
        actions: [
          Consumer<ExpenseViewModel>(
            builder: (ctx, vm, _) => IconButton(
              tooltip: 'Switch currency (${vm.displayCurrency})',
              icon: Text(vm.displayCurrency,
                  style: const TextStyle(fontWeight: FontWeight.w700)),
              onPressed: () async {
                final messenger = ScaffoldMessenger.of(ctx);
                await vm.toggleCurrency();
                if (vm.errorMessage != null) {
                  if (!mounted) return;
                  messenger.showSnackBar(
                    SnackBar(
                      content: Text(vm.errorMessage!),
                      backgroundColor: const Color(0xFFDC2626),
                      duration: const Duration(seconds: 4),
                    ),
                  );
                }
              },
            ),
          ),
          Builder(
            builder: (ctx) => PopupMenuButton<void>(
              icon: const Icon(Icons.more_vert),
              itemBuilder: (_) => [
                PopupMenuItem(
                  onTap: () {
                    final auth = ctx.read<AuthService>();
                    final nav = Navigator.of(ctx);
                    Future.delayed(
                      Duration.zero,
                      () => _handleLogout(auth: auth, nav: nav),
                    );
                  },
                  child: const Row(
                    children: [
                      Icon(Icons.logout, size: 18),
                      SizedBox(width: 10),
                      Text('Sign Out'),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: RefreshIndicator(
          color: accent,
          onRefresh: () =>
              context.read<ExpenseViewModel>().loadTransactions(),
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _SmartInputCard(
                  controller: _inputController,
                  onClear: _handleClearInput,
                  onSubmit: _handleSubmit,
                ),
                const SizedBox(height: 16),
                Consumer<ExpenseViewModel>(
                  builder: (ctx, vm, _) {
                    if (vm.errorMessage != null) {
                      return Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: _InlineBanner(message: vm.errorMessage!),
                      );
                    }
                    return const SizedBox.shrink();
                  },
                ),
                const _SummaryCard(),
                const SizedBox(height: 16),
                const _FilterRow(),
                const SizedBox(height: 8),
                const _TransactionList(),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _SmartInputCard extends StatelessWidget {
  final TextEditingController controller;
  final VoidCallback onClear;
  final Future<void> Function() onSubmit;

  const _SmartInputCard({
    required this.controller,
    required this.onClear,
    required this.onSubmit,
  });

  @override
  Widget build(BuildContext context) {
    final accent = Theme.of(context).primaryColor;
    final outline = OutlineInputBorder(
      borderRadius: BorderRadius.circular(16),
      borderSide: BorderSide(color: Colors.grey.shade300),
    );
    return Consumer<ExpenseViewModel>(
      builder: (ctx, vm, _) => Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(20),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.04),
              blurRadius: 18,
              offset: const Offset(0, 6),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('AI Smart Bookkeeping',
                style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16)),
            const SizedBox(height: 4),
            Text('Try: coffee 4 USD, buy 6 pizzas 72 USD',
                style: TextStyle(color: Colors.grey.shade500, fontSize: 12)),
            const SizedBox(height: 12),
            TextField(
              controller: controller,
              maxLines: 3,
              minLines: 1,
              keyboardType: TextInputType.multiline,
              textInputAction: TextInputAction.done,
              onSubmitted: (_) => onSubmit(),
              decoration: InputDecoration(
                hintText:
                    'Describe your spend. Use "and" / commas to add multiple in one shot.',
                filled: true,
                fillColor: Colors.grey.shade50,
                enabledBorder: outline,
                focusedBorder: outline.copyWith(
                  borderSide: BorderSide(color: accent, width: 1.6),
                ),
                contentPadding:
                    const EdgeInsets.fromLTRB(16, 14, 56, 14),
                suffixIcon: IconButton(
                  tooltip: 'Clear',
                  icon: const Icon(Icons.close),
                  onPressed: onClear,
                ),
              ),
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                OutlinedButton.icon(
                  onPressed: vm.isLoading ? null : onClear,
                  icon: const Icon(Icons.delete_sweep_outlined, size: 18),
                  label: const Text('Clear'),
                  style: OutlinedButton.styleFrom(
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(20)),
                    padding: const EdgeInsets.symmetric(
                        horizontal: 16, vertical: 10),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: FilledButton.icon(
                    onPressed: vm.isLoading ? null : onSubmit,
                    icon: vm.isLoading
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(
                                strokeWidth: 2, color: Colors.white),
                          )
                        : const Icon(Icons.auto_awesome, size: 18),
                    label: Text(vm.isLoading ? 'Processing…' : 'Record Expense'),
                    style: FilledButton.styleFrom(
                      backgroundColor: accent,
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(20)),
                      padding: const EdgeInsets.symmetric(vertical: 12),
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _InlineBanner extends StatelessWidget {
  final String message;
  const _InlineBanner({required this.message});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFFEF2F2),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFFFECACA)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.error_outline,
              color: Color(0xFFDC2626), size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Text(message,
                style: const TextStyle(color: Color(0xFF991B1B))),
          ),
        ],
      ),
    );
  }
}

class _SummaryCard extends StatelessWidget {
  const _SummaryCard();

  @override
  Widget build(BuildContext context) {
    final accent = Theme.of(context).primaryColor;
    return Consumer<ExpenseViewModel>(
      builder: (ctx, vm, _) {
        final symbol = vm.displayCurrency == 'VND' ? '₫' : r'$';
        final formatted = vm.displayCurrency == 'VND'
            ? vm.totalExpenses.toStringAsFixed(0)
            : vm.totalExpenses.toStringAsFixed(2);
        return Container(
          width: double.infinity,
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: accent,
            borderRadius: BorderRadius.circular(20),
            gradient: LinearGradient(
              colors: [accent, accent.withValues(alpha: 0.85)],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Total Spent',
                  style: TextStyle(color: Colors.white, fontSize: 13)),
              const SizedBox(height: 6),
              Text(
                '$symbol $formatted',
                style: const TextStyle(
                    color: Colors.white,
                    fontSize: 30,
                    fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: 8),
              Text(
                'Rate: 1 USD ≈ ${vm.exchangeRate.toStringAsFixed(0)} VND',
                style: TextStyle(
                    color: Colors.white.withValues(alpha: 0.85),
                    fontSize: 12),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _FilterRow extends StatefulWidget {
  const _FilterRow();

  @override
  State<_FilterRow> createState() => _FilterRowState();
}

class _FilterRowState extends State<_FilterRow> {
  final TextEditingController _merchantCtl = TextEditingController();
  final TextEditingController _categoryCtl = TextEditingController();

  @override
  void dispose() {
    _merchantCtl.dispose();
    _categoryCtl.dispose();
    super.dispose();
  }

  Future<void> _apply() async {
    await context.read<ExpenseViewModel>().loadTransactions(
          category: _categoryCtl.text.trim(),
          merchant: _merchantCtl.text.trim(),
        );
  }

  Future<void> _clear() async {
    _categoryCtl.clear();
    _merchantCtl.clear();
    await context.read<ExpenseViewModel>().clearFilters();
  }

  @override
  Widget build(BuildContext context) {
    final outline = OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: BorderSide(color: Colors.grey.shade300),
    );
    return Consumer<ExpenseViewModel>(
      builder: (ctx, vm, _) => Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Filter & Sort',
                style: TextStyle(fontWeight: FontWeight.w700)),
            const SizedBox(height: 10),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _categoryCtl,
                    decoration: InputDecoration(
                      hintText: 'Category',
                      isDense: true,
                      contentPadding: const EdgeInsets.symmetric(
                          horizontal: 12, vertical: 12),
                      border: outline,
                      enabledBorder: outline,
                      focusedBorder: outline,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: TextField(
                    controller: _merchantCtl,
                    decoration: InputDecoration(
                      hintText: 'Merchant',
                      isDense: true,
                      contentPadding: const EdgeInsets.symmetric(
                          horizontal: 12, vertical: 12),
                      border: outline,
                      enabledBorder: outline,
                      focusedBorder: outline,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Wrap(
              spacing: 8,
              runSpacing: 10,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                DropdownButtonHideUnderline(
                  child: DropdownButton<String>(
                    value: vm.sortField,
                    items: const [
                      DropdownMenuItem(value: 'amount', child: Text('Amount')),
                      DropdownMenuItem(value: 'id', child: Text('Time')),
                      DropdownMenuItem(
                          value: 'category', child: Text('Category')),
                      DropdownMenuItem(
                          value: 'merchant', child: Text('Merchant')),
                    ],
                    onChanged: (v) async {
                      if (v == null) return;
                      await context
                          .read<ExpenseViewModel>()
                          .loadTransactions(sortField: v);
                    },
                  ),
                ),
                FilledButton.tonalIcon(
                  onPressed: vm.isLoading
                      ? null
                      : () async {
                          final order =
                              vm.sortOrder == 'asc' ? 'desc' : 'asc';
                          await context
                              .read<ExpenseViewModel>()
                              .loadTransactions(sortOrder: order);
                        },
                  icon: Icon(vm.sortOrder == 'asc'
                      ? Icons.arrow_upward
                      : Icons.arrow_downward),
                  label:
                      Text(vm.sortOrder == 'asc' ? 'Ascending' : 'Descending'),
                ),
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    TextButton(
                      onPressed: vm.isLoading ? null : _clear,
                      child: const Text('Clear'),
                    ),
                    FilledButton(
                      onPressed: vm.isLoading ? null : _apply,
                      child: const Text('Apply'),
                    ),
                  ],
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _TransactionList extends StatelessWidget {
  const _TransactionList();

  @override
  Widget build(BuildContext context) {
    return Consumer<ExpenseViewModel>(
      builder: (ctx, vm, _) {
        if (vm.isLoading && vm.transactions.isEmpty) {
          return const Padding(
            padding: EdgeInsets.all(32),
            child: Center(child: CircularProgressIndicator()),
          );
        }
        if (vm.transactions.isEmpty) {
          return Padding(
            padding: const EdgeInsets.all(32),
            child: Center(
              child: Column(
                children: [
                  Icon(Icons.receipt_long_outlined,
                      size: 52, color: Colors.grey.shade400),
                  const SizedBox(height: 12),
                  Text('No transactions yet. Try the AI box above!',
                      style: TextStyle(color: Colors.grey.shade500)),
                ],
              ),
            ),
          );
        }
        return Column(
          children: [
            for (final tx in vm.transactions) _TxTile(tx: tx),
          ],
        );
      },
    );
  }
}

class _TxTile extends StatelessWidget {
  final Transaction tx;
  const _TxTile({required this.tx});

  @override
  Widget build(BuildContext context) {
    final accent = Theme.of(context).primaryColor;
    final currency = tx.currency;
    final symbol = currency == 'VND' ? '₫' : r'$';
    final formatted = currency == 'VND'
        ? tx.amount.toStringAsFixed(0)
        : tx.amount.toStringAsFixed(2);
    return Dismissible(
      key: Key('tx-${tx.id}-${tx.amount}-${tx.merchant}'),
      direction: DismissDirection.endToStart,
      background: Container(
        margin: const EdgeInsets.symmetric(vertical: 6),
        padding: const EdgeInsets.only(right: 20),
        alignment: Alignment.centerRight,
        decoration: BoxDecoration(
          color: const Color(0xFFDC2626),
          borderRadius: BorderRadius.circular(14),
        ),
        child: const Icon(Icons.delete_outline, color: Colors.white),
      ),
      confirmDismiss: (_) async {
        final ok = await showDialog<bool>(
          context: context,
          builder: (ctx) => AlertDialog(
            title: const Text('Delete this transaction?'),
            content: const Text(
                'This cannot be undone. The record will be removed permanently.'),
            actions: [
              TextButton(
                  onPressed: () => Navigator.of(ctx).pop(false),
                  child: const Text('Cancel')),
              FilledButton(
                  onPressed: () => Navigator.of(ctx).pop(true),
                  child: const Text('Delete')),
            ],
          ),
        );
        return ok == true;
      },
      onDismissed: (_) async {
        final id = tx.id;
        if (id == null) return;
        final ok =
            await context.read<ExpenseViewModel>().deleteTransaction(id);
        if (!ok && context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(context
                      .read<ExpenseViewModel>()
                      .errorMessage ??
                  'Failed to delete transaction'),
              backgroundColor: const Color(0xFFDC2626),
              duration: const Duration(seconds: 3),
            ),
          );
        }
      },
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 6),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(14),
        ),
        child: Row(
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: accent.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(Icons.receipt, color: accent),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    tx.merchant.isEmpty ? 'Unknown Merchant' : tx.merchant,
                    style: const TextStyle(fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    [
                      tx.category.isEmpty ? 'Uncategorized' : tx.category,
                      if (tx.originalCurrency != tx.currency)
                        'Original currency ${tx.originalCurrency}'
                    ].join('  ·  '),
                    style:
                        TextStyle(color: Colors.grey.shade600, fontSize: 12),
                  ),
                  const SizedBox(height: 3),
                  if (tx.originalDescription.isNotEmpty)
                    Text(tx.originalDescription,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                            color: Colors.grey.shade500, fontSize: 12)),
                ],
              ),
            ),
            const SizedBox(width: 12),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  '$symbol $formatted',
                  style: TextStyle(
                    fontWeight: FontWeight.w800,
                    color: tx.amount == 0
                        ? const Color(0xFF16A34A)
                        : Colors.black87,
                  ),
                ),
                const SizedBox(height: 4),
                Text(currency,
                    style: TextStyle(
                        color: Colors.grey.shade500, fontSize: 11)),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
