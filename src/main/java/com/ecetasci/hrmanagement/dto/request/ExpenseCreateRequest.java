package com.ecetasci.hrmanagement.dto.request;

import com.ecetasci.hrmanagement.enums.ExpenseStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseCreateRequest(String description, BigDecimal amount,
                                   LocalDate expenseDate) {

}
