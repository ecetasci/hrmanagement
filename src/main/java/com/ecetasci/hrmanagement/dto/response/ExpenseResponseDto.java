package com.ecetasci.hrmanagement.dto.response;

import com.ecetasci.hrmanagement.enums.ExpenseStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;


@Builder
public record ExpenseResponseDto(
        Long id,
        String description,
        BigDecimal amount,
        LocalDate expenseDate,
        ExpenseStatus status
) {}
