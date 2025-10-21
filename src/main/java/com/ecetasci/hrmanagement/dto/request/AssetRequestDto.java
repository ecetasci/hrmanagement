package com.ecetasci.hrmanagement.dto.request;

import com.ecetasci.hrmanagement.enums.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetRequestDto {
    @NotBlank(message = "Name cannot be blank")
    private String name;
    private String brand;
    private String model;

    private String serialNumber;
    private BigDecimal value;

    private AssetType type;
    @NotNull(message = "Company id must be specified")
    private Long companyId;
}
