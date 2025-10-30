package com.ecetasci.hrmanagement.mapper;

import com.ecetasci.hrmanagement.dto.request.ShiftRequestDto;
import com.ecetasci.hrmanagement.dto.response.ShiftResponseDto;
import com.ecetasci.hrmanagement.entity.Shift;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShiftMapper {

    // ENTITY → DTO
    @Mapping(source = "company.id", target = "companyId")
    @Mapping(source = "company.companyName", target = "companyName")
    ShiftResponseDto toDto(Shift entity);

    // DTO → ENTITY (ilişkileri service katmanı set edecek)
    @Mapping(target = "company", ignore = true)
    Shift toEntity(ShiftRequestDto dto);
}
