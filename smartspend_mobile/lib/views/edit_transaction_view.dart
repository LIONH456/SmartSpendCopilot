import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:smartspend_mobile/models/transaction.dart';
import 'package:smartspend_mobile/view_models/expense_view_model.dart';

class EditTransactionView extends StatefulWidget {
  final Transaction transaction;

  const EditTransactionView({super.key, required this.transaction});

  @override
  State<EditTransactionView> createState() => _EditTransactionViewState();
}

class _EditTransactionViewState extends State<EditTransactionView> {
  late final TextEditingController _amountController;
  late final TextEditingController _categoryController;
  late final TextEditingController _merchantController;
  late final TextEditingController _descriptionController;
  final _formKey = GlobalKey<FormState>();
  bool _isSaving = false;
  late String _currency;

  @override
  void initState() {
    super.initState();
    _amountController = TextEditingController(text: widget.transaction.amount.toString());
    _categoryController = TextEditingController(text: widget.transaction.category);
    _merchantController = TextEditingController(text: widget.transaction.merchant);
    _descriptionController = TextEditingController(text: widget.transaction.originalDescription);
    _currency = widget.transaction.currency.isEmpty ? 'USD' : widget.transaction.currency;
  }

  @override
  void dispose() {
    _amountController.dispose();
    _categoryController.dispose();
    _merchantController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _isSaving = true);
    final vm = context.read<ExpenseViewModel>();
    final updated = Transaction(
      id: widget.transaction.id,
      amount: double.tryParse(_amountController.text.trim()) ?? widget.transaction.amount,
      category: _categoryController.text.trim(),
      merchant: _merchantController.text.trim(),
      originalDescription: _descriptionController.text.trim(),
      currency: _currency,
      originalCurrency: widget.transaction.originalCurrency,
    );
    final ok = await vm.updateTransaction(updated);
    if (!mounted) return;
    if (ok) {
      Navigator.of(context).pop();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Transaction updated successfully')),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(vm.errorMessage ?? 'Failed to update transaction')),
      );
    }
    setState(() => _isSaving = false);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Edit Transaction')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                TextFormField(
                  controller: _amountController,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(labelText: 'Amount', border: OutlineInputBorder()),
                  validator: (value) {
                    if ((value ?? '').trim().isEmpty) return 'Please enter an amount';
                    return null;
                  },
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _categoryController,
                  decoration: const InputDecoration(labelText: 'Category', border: OutlineInputBorder()),
                  validator: (value) => (value ?? '').trim().isEmpty ? 'Please enter a category' : null,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _merchantController,
                  decoration: const InputDecoration(labelText: 'Merchant', border: OutlineInputBorder()),
                  validator: (value) => (value ?? '').trim().isEmpty ? 'Please enter a merchant' : null,
                ),
                const SizedBox(height: 16),
                DropdownButtonFormField<String>(
                  initialValue: _currency,
                  decoration: const InputDecoration(labelText: 'Currency', border: OutlineInputBorder()),
                  items: const [
                    DropdownMenuItem(value: 'USD', child: Text('USD')),
                    DropdownMenuItem(value: 'VND', child: Text('VND')),
                  ],
                  onChanged: (value) => setState(() => _currency = value ?? 'USD'),
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _descriptionController,
                  maxLines: 3,
                  decoration: const InputDecoration(labelText: 'Description', border: OutlineInputBorder()),
                ),
                const SizedBox(height: 24),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: _isSaving ? null : _save,
                    icon: _isSaving
                        ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                        : const Icon(Icons.save_outlined),
                    label: Text(_isSaving ? 'Saving…' : 'Save Changes'),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
