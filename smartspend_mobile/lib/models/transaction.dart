class Transaction{
  final int? id;
  final double amount;
  final String category;
  final String merchant;
  final String originalDescription;
  final String currency;
  final String originalCurrency;

  // constructor
  Transaction({
    this.id,
    required this.amount,
    required this.category,
    required this.merchant,
    required this.originalDescription,
    required this.currency,
    required this.originalCurrency,
  });

  // convert raw Map (json) into a Dart object
  factory Transaction.fromJson(Map<String, dynamic> json) {
    return Transaction(
      id: json['id'] is int ? json['id'] as int : (json['id'] is num ? (json['id'] as num).toInt() : null),
      amount: json['amount'] is num ? (json['amount'] as num).toDouble() : 0.0,
      category: json['category'] as String? ?? '',
      merchant: json['merchant'] as String? ?? '',
      originalDescription: json['original_description'] as String?
          ?? json['originalDescription'] as String?
          ?? '',
      currency: json['currency'] as String? ?? 'USD',
      originalCurrency: json['original_currency'] as String?
          ?? json['originalCurrency'] as String?
          ?? 'USD',
    );
  }

  // convert Dart object back into a raw Map (json)
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'amount': amount,
      'category': category,
      'merchant': merchant,
      'currency': currency,
      'original_currency': originalCurrency,
      'original_description': originalDescription,
    };
  }
}