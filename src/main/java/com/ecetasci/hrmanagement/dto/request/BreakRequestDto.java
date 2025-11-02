package com.ecetasci.hrmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record BreakRequestDto(
        @NotNull
        @Size(min = 1)
        String name,
        @NotNull
        LocalTime startTime,
        @NotNull
        LocalTime endTime,
        Integer duration,
        @NotNull
        Long shiftId
) {}

