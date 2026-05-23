package com.smartspend.copilot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;

    private Double amount;

    private String category;

    private String merchant;

    private String currency;

    private String originalCurrency;

    private String originalDescription;
}
