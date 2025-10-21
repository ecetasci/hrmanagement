package com.ecetasci.hrmanagement.dto.response;

import com.ecetasci.hrmanagement.enums.AssetType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AssetResponseDto {
    private Long id;
    private String name;
    private String brand;
    private String model;
    private String serialNumber;
    private BigDecimal value;
    private AssetType type;
    private Long companyId;
}

