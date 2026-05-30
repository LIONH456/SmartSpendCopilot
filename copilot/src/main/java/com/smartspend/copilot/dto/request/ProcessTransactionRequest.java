package com.smartspend.copilot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter //Spring 反序列化 JSON 时需要 setter
public class ProcessTransactionRequest {
    @Schema(
            description = "Raw transaction description entered by user",
            example = "spent 15$ on pizza at Dominos"
    )
    @NotBlank(message = "DESCRIPTION_BLANK")
    private String description;
}
