import 'package:flutter/material.dart';
import '../view_models/expense_view_model.dart';

class DashboardView extends StatefulWidget {
  // Declares a variable to hold the logic layer for managing expenses. 
  // It's marked final because this reference shouldn't change once the widget is drawn.
  final ExpenseViewModel viewModel;

  const DashboardView({super.key, required this.viewModel});

  @override
  State<DashboardView> createState() => _DashboardViewState();
}

class _DashboardViewState extends State<DashboardView> {
  // Instantiates a tracking engine for your user input text box. 
  // It lets you extract text or clear out whatever the user typed.
  final TextEditingController _inputController = TextEditingController();
  final TextEditingController _filterController = TextEditingController();

  // When the widget is first created, this method runs. 
  // It schedules a task to load transactions from the database right after the first frame is rendered
  @override
  void initState() {
    super.initState();
    // Initial fetch from backing DB
    WidgetsBinding.instance.addPostFrameCallback((_) {
      widget.viewModel.loadTransactions();
    });
  }

  @override
  void dispose() {
    _inputController.dispose();
    _filterController.dispose();
    super.dispose();
  }

  // Apply a category filter and refresh the transaction list.
  void _applyFilter() async {
    final filterText = _filterController.text.trim();
    await widget.viewModel.loadTransactions(category: filterText.isEmpty ? null : filterText);
  }

  // Clear filter state and reload all transactions.
  void _clearFilter() async {
    _filterController.clear();
    await widget.viewModel.clearFilters();
  }

  // Change the sort field and toggle order if the same field is selected.
  void _changeSort(String sortField) async {
    final currentField = widget.viewModel.sortField;
    final currentOrder = widget.viewModel.sortOrder;
    final nextOrder = currentField == sortField && currentOrder == 'desc' ? 'asc' : 'desc';
    await widget.viewModel.loadTransactions(sortField: sortField, sortOrder: nextOrder);
  }

  // Ask the user to confirm deletion before removing a transaction.
  Future<void> _confirmDelete(int? id) async {
    if (id == null) return;

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Delete transaction'),
          content: const Text('Are you sure you want to remove this transaction?'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed: () => Navigator.of(context).pop(true),
              child: const Text('Delete', style: TextStyle(color: Colors.red)),
            ),
          ],
        );
      },
    );

    if (confirmed == true) {
      final success = await widget.viewModel.deleteTransaction(id);
      if (success && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Transaction removed successfully')),
        );
      }
    }
  }

  // A helper function to determine which icon to show based on the transaction category.
  IconData _getCategoryIcon(String category) {
    switch (category.toLowerCase()) {
      case 'food':
      case 'dining':
        return Icons.fastfood_rounded;
      case 'transport':
      case 'travel':
        return Icons.directions_car_rounded;
      case 'utilities':
      case 'bills':
        return Icons.electric_bolt_rounded;
      case 'shopping':
        return Icons.shopping_bag_rounded;
      case 'entertainment':
        return Icons.movie_creation_rounded;
      default:
        return Icons.account_balance_wallet_rounded;
    }
  }

  // This function is called when the user taps the submit button. 
  // It takes the text from the input box, checks if it's not empty, and then sends it to the view model to be processed. If the processing is successful, it clears the input box and shows a success message.
  void _handleSubmit() async {
    final text = _inputController.text;
    if (text.trim().isEmpty) return;

    // Passes the text over to the AI processing view-model method and pauses execution context until it answers back with a true/false status.
    final success = await widget.viewModel.processRawExpense(text);
    if (success) {
      _inputController.clear();
      // An essential safety check! This ensures the user hasn't backed out of this screen while the asynchronous AI call was running.
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Expense logged successfully via Gemini Engine'),
            backgroundColor: Colors.greenAccent,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0B0D14), // Premium Dark Slate
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: const Text(
          'SmartSpend AI',
          style: TextStyle(fontWeight: FontWeight.w800, color: Colors.white, fontSize: 22),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: Colors.white70),
            onPressed: () => widget.viewModel.loadTransactions(),
          ),
          TextButton(
            onPressed: widget.viewModel.isLoading ? null : () => widget.viewModel.toggleCurrency(),
            child: Text(widget.viewModel.displayCurrency, style: const TextStyle(color: Colors.white70)),
          ),
        ],
      ),
      body: ListenableBuilder(
        listenable: widget.viewModel,
        builder: (context, _) {
          return Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 10),
                // Premium Banking Expense Display Card
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(24),
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [Color(0xFF2E5BFF), Color(0xFF1E32AA)],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    borderRadius: BorderRadius.circular(24),
                    boxShadow: [
                      BoxShadow(
                        color: const Color.fromRGBO(46, 91, 255, 0.3),
                        blurRadius: 20,
                        offset: const Offset(0, 10),
                      )
                    ],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'TOTAL TRACKED EXPENSES',
                        style: TextStyle(color: Colors.white70, fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 1.2),
                      ),
                      const SizedBox(height: 8),
                      Builder(builder: (context) {
                        final total = widget.viewModel.totalExpenses;
                        final isVnd = widget.viewModel.displayCurrency == 'VND';
                        final rate = widget.viewModel.exchangeRate;
                        final totalText = isVnd ? '₫${total.toStringAsFixed(0)}' : '\$${total.toStringAsFixed(2)}';
                        return Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              totalText,
                              style: const TextStyle(color: Colors.white, fontSize: 36, fontWeight: FontWeight.w900),
                            ),
                            const SizedBox(height: 6),
                            Text(
                              '1 USD = ${rate.toStringAsFixed(isVnd ? 0 : 2)} VND',
                              style: const TextStyle(color: Colors.white70, fontSize: 12),
                            ),
                          ],
                        );
                      }),
                    ],
                  ),
                ),
                const SizedBox(height: 24),
                // AI Copilot Input Interface
                const Text(
                  'AI EXPENSE COPILOT',
                  style: TextStyle(color: Colors.white54, fontSize: 12, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _inputController,
                        style: const TextStyle(color: Colors.white),
                        decoration: InputDecoration(
                          hintText: 'e.g., Bought pizza at Dominos for \$15',
                          hintStyle: const TextStyle(color: Colors.white30),
                          fillColor: const Color(0xFF161925),
                          filled: true,
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(16),
                            borderSide: BorderSide.none,
                          ),
                          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    GestureDetector(
                      onTap: widget.viewModel.isLoading ? null : _handleSubmit,
                      child: Container(
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: const Color(0xFF2E5BFF),
                          borderRadius: BorderRadius.circular(16),
                        ),
                        child: widget.viewModel.isLoading
                            ? const SizedBox(
                                width: 24,
                                height: 24,
                                child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2),
                              )
                            : const Icon(Icons.bolt_rounded, color: Colors.white),
                      ),
                    )
                  ],
                ),
                if (widget.viewModel.errorMessage != null) ...[
                  const SizedBox(height: 12),
                  Text(
                    widget.viewModel.errorMessage!,
                    style: const TextStyle(color: Colors.redAccent, fontSize: 13),
                  ),
                ],
                const SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _filterController,
                        style: const TextStyle(color: Colors.white),
                        decoration: InputDecoration(
                          hintText: 'Filter by category',
                          hintStyle: const TextStyle(color: Colors.white30),
                          fillColor: const Color(0xFF161925),
                          filled: true,
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(16),
                            borderSide: BorderSide.none,
                          ),
                          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    IconButton(
                      icon: const Icon(Icons.search, color: Colors.white70),
                      onPressed: widget.viewModel.isLoading ? null : _applyFilter,
                    ),
                    const SizedBox(width: 4),
                    IconButton(
                      icon: const Icon(Icons.clear, color: Colors.white70),
                      onPressed: widget.viewModel.isLoading ? null : _clearFilter,
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Sort by: ${widget.viewModel.sortField.toUpperCase()} ${widget.viewModel.sortOrder.toUpperCase()}',
                      style: const TextStyle(color: Colors.white70, fontSize: 12),
                    ),
                    PopupMenuButton<String>(
                      icon: const Icon(Icons.sort, color: Colors.white70),
                      color: const Color(0xFF161925),
                      onSelected: _changeSort,
                      itemBuilder: (context) => const [
                        PopupMenuItem(value: 'amount', child: Text('Amount')),
                        PopupMenuItem(value: 'category', child: Text('Category')),
                        PopupMenuItem(value: 'merchant', child: Text('Merchant')),
                      ],
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                const Text(
                  'TRANSACTION HISTORY',
                  style: TextStyle(color: Colors.white54, fontSize: 12, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 12),
                // Transaction History List View
                Expanded(
                  child: widget.viewModel.transactions.isEmpty && !widget.viewModel.isLoading
                      ? const Center(
                          child: Text(
                            'No transactions documented yet.',
                            style: TextStyle(color: Colors.white30),
                          ),
                        )
                      : ListView.builder(
                          itemCount: widget.viewModel.transactions.length,
                          physics: const BouncingScrollPhysics(),
                          itemBuilder: (context, index) {
                            final tx = widget.viewModel.transactions[index];
                            return Container(
                              margin: const EdgeInsets.only(bottom: 12),
                              padding: const EdgeInsets.all(16),
                              decoration: BoxDecoration(
                                color: const Color(0xFF161925),
                                borderRadius: BorderRadius.circular(16),
                              ),
                              child: Row(
                                children: [
                                  Container(
                                    padding: const EdgeInsets.all(12),
                                    decoration: BoxDecoration(
                                      color: const Color(0xFF21263C),
                                      borderRadius: BorderRadius.circular(12),
                                    ),
                                    child: Icon(
                                      _getCategoryIcon(tx.category),
                                      color: const Color(0xFF2E5BFF),
                                    ),
                                  ),
                                  const SizedBox(width: 16),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          tx.merchant,
                                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16),
                                        ),
                                        const SizedBox(height: 4),
                                        Text(
                                          tx.originalDescription,
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                          style: const TextStyle(color: Colors.white38, fontSize: 12),
                                        ),
                                        if (tx.originalCurrency != 'USD') ...[
                                          const SizedBox(height: 4),
                                          Text(
                                            'Original: ${tx.originalCurrency}',
                                            style: const TextStyle(color: Colors.white54, fontSize: 10),
                                          ),
                                        ],
                                      ],
                                    ),
                                  ),
                                  Column(
                                    crossAxisAlignment: CrossAxisAlignment.end,
                                    children: [
                                      Builder(builder: (context) {
                                        final isVnd = widget.viewModel.displayCurrency == 'VND';
                                        final rate = widget.viewModel.exchangeRate;
                                        final amt = isVnd ? (tx.amount * rate) : tx.amount;
                                        final amtText = isVnd ? '₫${amt.toStringAsFixed(0)}' : '\$${amt.toStringAsFixed(2)}';
                                        return Text(
                                          '-$amtText',
                                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16),
                                        );
                                      }),
                                      const SizedBox(height: 4),
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                                        decoration: BoxDecoration(
                                          color: const Color(0xFF21263C),
                                          borderRadius: BorderRadius.circular(6),
                                        ),
                                        child: Text(
                                          tx.category,
                                          style: const TextStyle(color: Colors.white70, fontSize: 10),
                                        ),
                                      ),
                                      const SizedBox(height: 8),
                                      IconButton(
                                        icon: const Icon(Icons.delete_outline, color: Colors.redAccent, size: 20),
                                        onPressed: widget.viewModel.isLoading ? null : () => _confirmDelete(tx.id),
                                      ),
                                    ],
                                  )
                                ],
                              ),
                            );
                          },
                        ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}