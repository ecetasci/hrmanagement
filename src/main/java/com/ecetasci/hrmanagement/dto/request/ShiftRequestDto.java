// ShiftRequestDto.java
package com.ecetasci.hrmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;


public record ShiftRequestDto(
        @NotBlank(message = "Shift name cannot be blank")
        String name,

        @NotNull(message = "Start time is required")
        LocalDateTime startTime,

        @NotNull(message = "End time is required")
        LocalDateTime endTime,

        @NotNull(message = "Company ID is required")
        Long companyId
) {}
