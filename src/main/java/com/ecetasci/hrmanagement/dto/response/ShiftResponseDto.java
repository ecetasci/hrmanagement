package com.ecetasci.hrmanagement.dto.response;

import java.time.LocalTime;

public record ShiftResponseDto(Long id,
                               String name,
                               LocalTime startTime,
                               LocalTime endTime,
                               Long companyId,
                               String companyName) {
}
