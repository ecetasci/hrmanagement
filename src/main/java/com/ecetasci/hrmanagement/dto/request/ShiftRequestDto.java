// ShiftRequestDto.java
package com.ecetasci.hrmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;


public record ShiftRequestDto(
        @NotBlank(message = "Shift name cannot be blank")
        String name,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime,

        @NotNull(message = "Company ID is required")
        Long companyId
) {}
