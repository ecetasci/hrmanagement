package com.ecetasci.hrmanagement.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LeaveRequestDto(



        String employeeNumber,

        @NotNull(message = "Leave type id cannot be null")
        Long leaveTypeId,

        @NotNull(message = "Start date cannot be null")
        @FutureOrPresent(message = "Start date cannot be in the past")
        LocalDate startDate,

        @NotNull(message = "End date cannot be null")
        LocalDate endDate,

        String employeeNote
) {}
