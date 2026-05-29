package com.smartspend.copilot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    @Schema(description = "Transaction ID", example = "1")
    private Long id;

    @Schema(description = "Transaction amount in normalized currency", example = "15.0")
    private Double amount;

    @Schema(description = "Transaction category", example = "Food")
    private String category;

    @Schema(description = "Merchant name", example = "Dominos")
    private String merchant;

    @Schema(description = "Normalized currency", example = "USD")
    private String currency;

    @Schema(description = "Original Currency before normalized", example = "USD")
    private String originalCurrency;

    @Schema(description = "Original description that user input (raw text)", example = "spent 15$ on pizza at Dominos")
    private String originalDescription;
}
