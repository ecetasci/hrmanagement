package com.ecetasci.hrmanagement.dto.response;

public record LeaveTypeResponseDto(
        Long id,
        String name,
        String description,
        Integer maxDays,
        boolean isPaid,
        Long companyId
) {}

