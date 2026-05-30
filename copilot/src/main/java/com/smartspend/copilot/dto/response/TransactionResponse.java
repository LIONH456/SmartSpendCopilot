package com.smartspend.copilot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class TransactionResponse {
    @Schema(description = "Transaction ID", example = "1")
    Long id;

    @Schema(description = "Transaction amount in normalized currency", example = "15.0")
    Double amount;

    @Schema(description = "Transaction category", example = "Food")
    String category;

    @Schema(description = "Merchant name", example = "Dominos")
    String merchant;

    @Schema(description = "Normalized currency", example = "USD")
    String currency;

    @Schema(description = "Original Currency before normalized", example = "USD")
    String originalCurrency;

    @Schema(description = "Original description that user input (raw text)", example = "spent 15$ on pizza at Dominos")
    String originalDescription;
}
