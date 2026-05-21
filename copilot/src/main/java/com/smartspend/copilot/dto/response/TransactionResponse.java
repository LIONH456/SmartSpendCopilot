package com.smartspend.copilot.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
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
