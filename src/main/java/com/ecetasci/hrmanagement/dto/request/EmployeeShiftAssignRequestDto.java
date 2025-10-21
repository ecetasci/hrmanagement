// EmployeeShiftAssignRequestDto.java
package com.ecetasci.hrmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EmployeeShiftAssignRequestDto(
        @NotNull Long shiftId,
        @NotNull LocalDate assignedDate
) {}
