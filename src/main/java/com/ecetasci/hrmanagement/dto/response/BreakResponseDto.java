package com.ecetasci.hrmanagement.dto.response;

import java.time.LocalTime;

public record BreakResponseDto(Long id,
                               String name,
                               LocalTime startTime,
                               LocalTime endTime,
                               Integer duration,
                               Long shiftId) {
}

