package com.ecetasci.hrmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignAssetRequestDto {
    @NotNull(message = "Asset id cannot be null")
    private Long assetId;
    private LocalDate assignedDate; // null ise LocalDate.now()
}
