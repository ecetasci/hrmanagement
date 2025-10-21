package com.ecetasci.hrmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectAssetRequestDto {
    @NotBlank(message = "Employee note must be provided for rejection")
    private String employeeNote;
}