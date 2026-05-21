package com.smartspend.copilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter //Spring 反序列化 JSON 时需要 setter
public class ProcessTransactionRequest {
    @NotBlank(message = "Description cannot be blank")
    private String description;
}
