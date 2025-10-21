package com.ecetasci.hrmanagement.dto.request;

import com.ecetasci.hrmanagement.enums.SubscriptionType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SubscriptionRequestDto(Long companyId, @NotNull(message = "Subscription type is required")
                                     SubscriptionType subscriptionType, BigDecimal price, LocalDate startDate, LocalDate endDate) {
}
