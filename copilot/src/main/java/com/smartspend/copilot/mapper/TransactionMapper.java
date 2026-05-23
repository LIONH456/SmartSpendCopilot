package com.smartspend.copilot.mapper;

import com.smartspend.copilot.dto.response.TransactionResponse;
import com.smartspend.copilot.entity.Transaction;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring") //MapStruct 自动生成 implementation 并交给 Spring 管理
public interface TransactionMapper {
    TransactionResponse toResponse(Transaction transaction);
}
