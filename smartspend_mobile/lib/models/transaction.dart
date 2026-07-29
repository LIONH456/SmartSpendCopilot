/// Transaction value model.
///
/// All numeric deserialization goes through the generic `num` type first,
/// then calls `.toDouble()`, which protects against:
///   • JSON integer 0  → no _CastError on (0 as double)
///   • JSON float 0.0  → works as before
///   • JSON string (rare) → caught by try-parse so we never crash
///
/// This is mandatory for the "amount == 0 explicitly allowed (free items)"
/// policy: int-to-double coercion was the #1 source of silent 0-amount rows
/// being dropped by parse-time exceptions in previous versions.
class Transaction {
  final int? id;
  final double amount;
  final String category;
  final String merchant;
  final String originalDescription;
  final String currency;
  final String originalCurrency;

  Transaction({
    this.id,
    required this.amount,
    required this.category,
    required this.merchant,
    required this.originalDescription,
    required this.currency,
    required this.originalCurrency,
  });

  /// SAFE deserializer — every `num` goes through `.toDouble()`.
  /// Uses `String? ?? ''` so null keys always default to empty string (not null).
  factory Transaction.fromJson(Map<String, dynamic> json) {
    // ---------------------------------------------------------------
    // amount: SAFE num cast -> toDouble()
    // Accepts: int (0), double (0.0), string "5.25", or absent -> default 0.0
    // ---------------------------------------------------------------
    final Object? rawAmount = json['amount'];
    double amount;
    if (rawAmount is num) {
      amount = rawAmount.toDouble();
    } else if (rawAmount is String) {
      amount = double.tryParse(rawAmount.trim()) ?? 0.0;
    } else {
      amount = 0.0;
    }

    // id: num.toInt() for the same int-vs-double-JSON reason
    final Object? rawId = json['id'];
    int? id;
    if (rawId is num) {
      id = rawId.toInt();
    } else if (rawId is String) {
      id = int.tryParse(rawId.trim());
    }

    // String fallback chain: accepts both snake_case and camelCase keys because
    // the backend TransactionResponse uses camelCase via Jackson, but some
    // dev configurations may return snake_case via SNAKE_CASE ObjectMapper.
    String readString(String camel, String snake) {
      final Object? v = json[camel] ?? json[snake];
      if (v == null) return '';
      if (v is String) return v;
      return v.toString().trim();
    }

    return Transaction(
      id: id,
      amount: amount,
      category: readString('category', 'category').trim(),
      merchant: readString('merchant', 'merchant').trim(),
      originalDescription:
          readString('originalDescription', 'original_description').trim(),
      currency: readString('currency', 'currency').trim().isEmpty
          ? 'USD'
          : readString('currency', 'currency').trim().toUpperCase(),
      originalCurrency:
          readString('originalCurrency', 'original_currency').trim().isEmpty
              ? 'USD'
              : readString('originalCurrency', 'original_currency')
                  .trim()
                  .toUpperCase(),
    );
  }

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
