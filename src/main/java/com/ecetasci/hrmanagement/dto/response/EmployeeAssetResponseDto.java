package com.ecetasci.hrmanagement.dto.response;

import com.ecetasci.hrmanagement.enums.EmployeeAssetStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class EmployeeAssetResponseDto {
    private Long id;
    private String employeeNumber;
    private String employeeName;
    private String assetName;
    private LocalDate assignedDate;
    private EmployeeAssetStatus status;
    private String employeeNote;
}
