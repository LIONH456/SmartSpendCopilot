package com.smartspend.copilot.service;

import com.smartspend.copilot.client.GeminiClient;
import com.smartspend.copilot.entity.Transaction;
import com.smartspend.copilot.exception.AppException;
import com.smartspend.copilot.exception.ClarificationRequiredException;
import com.smartspend.copilot.exception.ErrorCode;
import com.smartspend.copilot.exception.InvalidTransactionDescriptionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AIService {

    // =====================================================================
    // CURRENCY WHITELIST — ONLY USD AND VND. NO EUR/GBP/KHR/ETC.
    // =====================================================================
    private static final List<String> SUPPORTED_CURRENCIES = Arrays.asList("USD", "VND");

    // Curre
    // Currency symbols / keywords that are allowed / recognized (USD & VND only)
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:\\$|USD|\\s|\\u0111|VND|DONG)?\\s*(\\d+(?:[.,]\\d{1,2})?)\\s*(?:\\$|USD|\\u0111|VND|DONG)?"
    );

    // Any other 3-letter ISO currency code (blocking non-USD/VND)
    private static final Pattern FOREIGN_CURRENCY_PATTERN = Pattern.compile(
            "(?i)(?<![A-Za-z])(EUR|GBP|JPY|CNY|KHR|THB|SGD|MYR|PHP|IDR|KRW|AUD|CAD|CHF|HKD|TWD|NZD|INR|RUB|BRL|MXN|ARS|CLP|COP|PEN|VES|BOB|PYG|UYU)(?![A-Za-z])"
    );

    private static final Pattern GIBBERISH_PATTERN = Pattern.compile("^(?i)([a-z])\\1{2,}$");
    private static final Pattern FREE_NOTICE_PATTERN = Pattern.compile("(?i)\\b(free|gift|complimentary|reward|coupon|on the house|no charge|no cost)\\b");

    // Separators that indicate a new transaction should follow
    private static final Pattern SPLIT_PATTERN = Pattern.compile(
            "(?i)\\s+(?:and|or|plus|also|then|&|\\+|,)\\s+|\\s*[\\n\\r]+|\\s*;\\s*|\\s*[\\u2022\\-]\\s*"
    );

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public AIService(GeminiClient geminiClient, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    private boolean hasExplicitPrice(String text) {
        return AMOUNT_PATTERN.matcher(text).find();
    }

    private boolean hasFreeIndicator(String text) {
        return FREE_NOTICE_PATTERN.matcher(text).find();
    }

    private void validateDescription(String normalized) {
        if (!hasExplicitPrice(normalized) && !hasFreeIndicator(normalized)) {
            throw new InvalidTransactionDescriptionException(
                    "Invalid transaction description. Please include a valid price or indicate if it was free (e.g., free coffee)."
            );
        }
    }

    private void detectAmbiguousAmounts(String normalized) {
        String[] chunks = SPLIT_PATTERN.split(normalized);
        for (String chunk : chunks) {
            if (chunk.isBlank()) continue;
            Matcher matcher = AMOUNT_PATTERN.matcher(chunk);
            List<Double> amounts = new ArrayList<>();
            while (matcher.find()) {
                try {
                    amounts.add(Double.parseDouble(matcher.group(1).replace(',', '.')));
                } catch (NumberFormatException ignored) {
                }
            }
            if (amounts.size() > 1) {
                String itemOnly = chunk.replaceAll(AMOUNT_PATTERN.pattern(), "").trim();
                itemOnly = itemOnly.replaceAll("[\\$USDVNDvndđdongs]*", "").trim();
                if (itemOnly.isBlank()) {
                    itemOnly = "Item";
                }
                double val1 = amounts.get(0);
                double val2 = amounts.get(amounts.size() - 1);
                throw new ClarificationRequiredException(itemOnly, val1, val2);
            }
        }
    }

    /**
     * LEGACY single-transaction entry point. Preserves backwards compatibility.
     */
    public Transaction parseTransaction(String description) {
        List<Transaction> results = parseTransactions(description);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * NEW multi-transaction parser (USD/VND only)
     *
     * Pipeline:
     *   1. Algorithmic pre-validation (blank / gibberish / unsupported-currency blocker)
     *   2. Gemini call with strict white-listed-currency array-envelope schema
     *   3. Strict JSON parsing + per-item coercion
     *   4. Post-validation (amount >= 0, non-empty category/merchant)
     *   5. Regex fallback if AI produces invalid payload
     */
    public List<Transaction> parseTransactions(String description) {
        // ============= LAYER 1: ALGORITHMIC PRE-VALIDATION =============
        if (description == null || description.isBlank()) {
            throw new AppException(ErrorCode.DESCRIPTION_BLANK);
        }

        String normalized = description.trim();
        String lowered = normalized.toLowerCase(Locale.ROOT);

        // Gibberish guard: single repeated char like "aaaaaa" or "bbbb"
        if (GIBBERISH_PATTERN.matcher(lowered.replaceAll("\\s+", "")).matches()) {
            throw new InvalidTransactionDescriptionException(
                    "Invalid transaction description. Please include a valid price or indicate if it was free (e.g., free coffee)."
            );
        }

        validateDescription(normalized);
        detectAmbiguousAmounts(normalized);

        // ============= UNSUPPORTED CURRENCY BLOCKER (LAYER 1B) =============
        // If user types EUR/GBP/KHR etc. BEFORE hitting Gemini: block immediately
        // with a structured error so the UI can show a friendly message.
        Matcher foreignMatcher = FOREIGN_CURRENCY_PATTERN.matcher(normalized);
        if (foreignMatcher.find()) {
            String found = foreignMatcher.group(1).toUpperCase(Locale.ROOT);
            log.warn("Unsupported currency detected in input: {}", found);
            throw new AppException(
                    ErrorCode.UNSUPPORTED_CURRENCY_PAIR,
                    "Unsupported currency: " + found + " - system only supports USD and VND."
            );
        }

        // Split description into candidate chunks (heuristic: before sending to AI)
        String[] chunks = SPLIT_PATTERN.split(normalized);
        List<Double> regexAmounts = new ArrayList<>();
        for (String c : chunks) {
            Matcher m = AMOUNT_PATTERN.matcher(c);
            while (m.find()) {
                try {
                    String raw = m.group(1).replace(',', '.');
                    double v = Double.parseDouble(raw);
                    regexAmounts.add(v);
                } catch (NumberFormatException ignored) {}
            }
        }
        int expectedCount = regexAmounts.size();

        // ============= LAYER 2: GEMINI CALL WITH USD/VND ONLY ARRAY SCHEMA =============
        String systemInstruction = """
                You are an expense parser for an app that SUPPORTS ONLY TWO CURRENCIES: USD and VND.
                DO NOT EVER output any other currency.
                RULES (follow EVERY ONE):
                1) INPUT VALIDATION — if the text does not contain any valid price or any free indicator, return EXACTLY:
                   {"status":"INVALID","reason":"Invalid transaction description. Please include a valid price or indicate if it was free (e.g., free coffee)."}
                2) MISSING CONTEXT RULE — if the user provides an isolated amount such as "spend 15$" or "spent 20000 VND" without specifying what was bought, where it was bought, or any merchant/item context, return EXACTLY:
                   {"status":"INVALID","reason":"Please specify what you spent the money on (e.g., spend 15$ on lunch)."}
                3) CURRENCY WHITELIST — if you detect EUR, GBP, JPY, KHR, CNY, SGD, THB, MYR, PHP, IDR, KRW, AUD, CAD, CHF, HKD, TWD, NZD, INR, RUB, BRL, MXN, ARS, CLP, COP, PEN, VES, BOB, PYG, UYU or any other currency NOT in {USD, VND}:
                   RETURN EXACTLY: {"error":"UNSUPPORTED_CURRENCY","hint":"System supports USD and VND only."}  — and nothing else.
                4) OUTPUT SHAPE: always return a JSON object {"transactions": Array<Transaction>}.
                   Each Transaction = { amount: number, category: string, merchant: string }.
                5) AMOUNT RULE:
                   - amount MUST be >= 0. amount == 0 is explicitly ALLOWED (free item, comp, reward, coupon).
                   - NEVER use a NEGATIVE number.
                   - If one item has multiple amounts with no clear separator, return {"status":"CLARIFICATION_REQUIRED","options":{"item":"ice cream","val1":1.0,"val2":2.0}}.
                6) CATEGORY must be one of: Food,Dining,Transport,Travel,Utilities,Bills,Shopping,Entertainment,Healthcare,Education,Other.
                7) MERCHANT: non-empty string. Infer if unclear and fill "Unknown".
                8) SPLITTING RULES (very important — output list length):
                   Split on EVERY separator listed below — within ONE line OR across NEWLINES:
                     • and, or, plus, &, +, also, then
                     • comma ",", semicolon ";", bullet "•", dash "-"
                     • newline \n or \r\n
                     • period followed by capital letter
                   Each separated chunk with a money amount → 1 Transaction.
                9) EXAMPLES:
                   - "eat ice cream for 1$ and buy shoes for 12$"
                     → {"transactions":[{"amount":1,"category":"Food","merchant":"Unknown"},{"amount":12,"category":"Shopping","merchant":"Unknown"}]}
                   - "coffee 3$, sandwich 8$"
                     → 2 transactions.
                   - "ice cream 1$ 2$" (no separator between amounts)
                     → {"status":"CLARIFICATION_REQUIRED","options":{"item":"ice cream","val1":1.0,"val2":2.0}}.
                   - "free pizza from dominos"  → 1 transaction amount=0, category=Food, merchant=Dominos.
                10) IF completely unparseable or context is missing return {"status":"INVALID","reason":"Please specify what you spent the money on (e.g., spend 15$ on lunch)."}.
                11) NEVER include markdown fences ```json or ``` in the output — raw JSON ONLY.
                12) Final JSON array length MUST match the number of distinct money amounts in input unless amounts clearly represent corrections within ONE item.
                """;

        String escaped = normalized.replace("\"", "\\\"");
        String requestBody = String.format("""
                {
                  "contents": [{
                    "parts":[{"text": "%s"}]
                  }],
                  "systemInstruction": {
                    "parts": [{"text": "%s"}]
                  },
                  "generationConfig": {
                    "responseMimeType": "application/json",
                    "temperature": 0.0
                  }
                }
                """, escaped, systemInstruction.replace("\"", "\\\""));

        String response = geminiClient.generateContent(requestBody);

        // ============= LAYER 3: STRICT JSON PARSING =============
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            String aiJson = rootNode
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            aiJson = aiJson
                    .replaceAll("(?s)^```(?:json)?\\s*", "")
                    .replaceAll("(?s)\\s*```$", "")
                    .trim();

            JsonNode payload = objectMapper.readTree(aiJson);

            // UNSUPPORTED CURRENCY NEGATIVE RESPONSE
            if (payload.has("error")) {
                String err = payload.path("error").asText("");
                if ("UNSUPPORTED_CURRENCY".equalsIgnoreCase(err)) {
                    String hint = payload.path("hint").asText("System supports USD and VND only.");
                    throw new AppException(ErrorCode.UNSUPPORTED_CURRENCY_PAIR, hint);
                }
            }

            if (payload.has("status")) {
                String status = payload.path("status").asText("");
                if ("INVALID".equalsIgnoreCase(status)) {
                    String reason = payload.path("reason").asText(
                            "Invalid transaction description. Please include a valid price or indicate if it was free (e.g., free coffee)."
                    );
                    throw new InvalidTransactionDescriptionException(reason);
                }
                if ("CLARIFICATION_REQUIRED".equalsIgnoreCase(status)) {
                    JsonNode options = payload.path("options");
                    String item = options.path("item").asText("Item");
                    double val1 = options.path("val1").asDouble(0.0);
                    double val2 = options.path("val2").asDouble(0.0);
                    throw new ClarificationRequiredException(item, val1, val2);
                }
            }

            JsonNode txsNode;
            if (payload.isArray()) {
                txsNode = payload;
            } else if (payload.has("transactions") && payload.get("transactions").isArray()) {
                txsNode = payload.get("transactions");
            } else {
                txsNode = objectMapper.createArrayNode().add(payload);
            }

            List<Transaction> parsed = objectMapper.convertValue(
                    txsNode, new TypeReference<List<Transaction>>() {}
            );

            // ============= LAYER 4: POST-VALIDATION (amount >= 0 allowed) =============
            List<Transaction> validated = new ArrayList<>();
            for (Transaction tx : parsed) {
                if (tx == null) continue;
                // ---- AMOUNT RULE: allow 0 (free/comp), block negative ----
                if (tx.getAmount() == null || tx.getAmount() < 0) continue;
                if (tx.getCategory() == null || tx.getCategory().isBlank()) tx.setCategory("Other");
                if (tx.getMerchant() == null || tx.getMerchant().isBlank()) tx.setMerchant("Unknown");
                validated.add(tx);
            }

            // If AI under-counted but regex found more amounts → build fallback rows
            if (validated.size() < expectedCount) {
                List<Transaction> fallback = buildFallbackFromRegex(normalized);
                for (Transaction fbt : fallback) {
                    boolean duplicate = false;
                    for (Transaction v : validated) {
                        if (Math.abs(v.getAmount() - fbt.getAmount()) < 0.001
                                && v.getCategory().equalsIgnoreCase(fbt.getCategory())) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) validated.add(fbt);
                }
            }

            if (validated.isEmpty() && expectedCount == 0) {
                // No amount regex found but description is still valid (free / 0$ case: try to still create single 0 row)
                Transaction t = new Transaction();
                t.setAmount(0.0);
                t.setCategory(inferCategory(normalized));
                t.setMerchant(inferMerchant(normalized));
                validated.add(t);
            }

            if (validated.isEmpty()) {
                    return buildFallbackFromRegex(normalized);
            }

            return validated;

        } catch (AppException ae) {
            // rethrow structured app exceptions (unsupported currency etc.)
            throw ae;
        } catch (Exception e) {
            log.error("AI parsing pipeline failed; attempting regex fallback", e);
            if (expectedCount > 0) {
                return buildFallbackFromRegex(normalized);
            }
            throw new AppException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }

    /**
     * Algorithmic fallback split+parse when Gemini output unusable but regex found amounts.
     * Handles the 1$ 2$ / multi-amount / comma / connectors and or + +\n cases deterministically.
     */
    private List<Transaction> buildFallbackFromRegex(String description) {
        // Split with separators first
        String[] parts = SPLIT_PATTERN.split(description);
        List<Transaction> out = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) continue;
            Matcher m = AMOUNT_PATTERN.matcher(part);
            List<Double> amountsInChunk = new ArrayList<>();
            while (m.find()) {
                String raw = m.group(1).replace(',', '.');
                try {
                    amountsInChunk.add(Double.parseDouble(raw));
                } catch (NumberFormatException ignored) {}
            }
            if (amountsInChunk.isEmpty()) continue;
            // ---- AMOUNT CORRECTION RULE (ice cream 1$ 2$ -> take last) ----
            double amount;
            if (amountsInChunk.size() == 1) {
                amount = amountsInChunk.get(0);
            } else {
                amount = amountsInChunk.get(amountsInChunk.size() - 1);
            }
            if (amount < 0) continue; // block negatives, allow 0
            Transaction tx = new Transaction();
            tx.setAmount(amount);
            tx.setCategory(inferCategory(part));
            tx.setMerchant(inferMerchant(part));
            out.add(tx);
        }
        // If separator-based split got nothing try whole string
        if (out.isEmpty()) {
            Matcher m = AMOUNT_PATTERN.matcher(description);
            List<Double> all = new ArrayList<>();
            while (m.find()) {
                try {
                    all.add(Double.parseDouble(m.group(1).replace(',', '.')));
                } catch (NumberFormatException ignored) {}
            }
            for (Double v : all) {
                if (v < 0) continue;
                Transaction tx = new Transaction();
                tx.setAmount(v);
                tx.setCategory(inferCategory(description));
                tx.setMerchant("Unknown");
                out.add(tx);
            }
        }
        if (out.isEmpty()) {
            throw new AppException(ErrorCode.AI_PARSING_FAILED);
        }
        return out;
    }

    private String inferCategory(String text) {
        String d = text.toLowerCase(Locale.ROOT);
        if (d.contains("pizza") || d.contains("food") || d.contains("eat") || d.contains("coffee")
                || d.contains("lunch") || d.contains("dinner") || d.contains("ice cream")
                || d.contains("burger") || d.contains("sandwich") || d.contains("cà phê")
                || d.contains("meal") || d.contains("snack")) return "Food";
        if (d.contains("shoes") || d.contains("shopping") || d.contains("buy") || d.contains("shop")
                || d.contains("clothes") || d.contains("mall") || d.contains("purchase")) return "Shopping";
        if (d.contains("taxi") || d.contains("uber") || d.contains("bus") || d.contains("train")
                || d.contains("gas") || d.contains("fuel") || d.contains("xe ") || d.contains("xe")) return "Transport";
        if (d.contains("electric") || d.contains("water") || d.contains("bill") || d.contains("internet")
                || d.contains("phone") || d.contains("tiền điện") || d.contains("tiền nước")) return "Utilities";
        if (d.contains("movie") || d.contains("cinema") || d.contains("game") || d.contains("concert")
                || d.contains("netflix") || d.contains("spotify")) return "Entertainment";
        if (d.contains("doctor") || d.contains("medicine") || d.contains("pharmacy") || d.contains("hospital")) return "Healthcare";
        if (d.contains("book") || d.contains("school") || d.contains("study") || d.contains("course")) return "Education";
        return "Other";
    }

    private String inferMerchant(String text) {
        String d = text.toLowerCase(Locale.ROOT);
        if (d.contains("domino") || d.contains("dominos")) return "Dominos";
        if (d.contains("starbuck") || d.contains("starbucks")) return "Starbucks";
        if (d.contains("mcdonald") || d.contains("mc donald") || d.contains("mcdo")) return "McDonald's";
        if (d.contains("kfc")) return "KFC";
        if (d.contains("lotteria")) return "Lotteria";
        if (d.contains("circle k") || d.contains("circlek")) return "Circle K";
        if (d.contains("uber")) return "Uber";
        if (d.contains("grab")) return "Grab";
        if (d.contains("lazada")) return "Lazada";
        if (d.contains("shopee")) return "Shopee";
        if (d.contains("tiki")) return "Tiki";
        return "Unknown";
    }
}
