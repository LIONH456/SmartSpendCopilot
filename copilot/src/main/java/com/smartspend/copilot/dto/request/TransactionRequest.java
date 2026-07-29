package com.smartspend.copilot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request DTO for updating a transaction")
public class TransactionRequest {
    @Schema(description = "Transaction amount", example = "15.0")
    @NotNull(message = "VALIDATION_ERROR")
    Double amount;

    @Schema(description = "Transaction category", example = "Food")
    @NotBlank(message = "VALIDATION_ERROR")
    String category;

    @Schema(description = "Merchant name", example = "Dominos")
    @NotBlank(message = "VALIDATION_ERROR")
    String merchant;

    @Schema(description = "Currency code", example = "USD")
    @NotBlank(message = "VALIDATION_ERROR")
    String currency;

    @Schema(description = "Original description", example = "spent 15$ on pizza")
    String originalDescription;
}
