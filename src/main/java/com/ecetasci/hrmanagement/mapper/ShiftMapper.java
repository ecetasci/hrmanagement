package com.ecetasci.hrmanagement.mapper;

import com.ecetasci.hrmanagement.dto.request.ShiftRequestDto;
import com.ecetasci.hrmanagement.dto.response.ShiftResponseDto;
import com.ecetasci.hrmanagement.entity.Shift;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Mapper(componentModel = "spring")
public interface ShiftMapper {

    // ENTITY → DTO
    @Mapping(source = "company.id", target = "companyId")
    @Mapping(source = "company.companyName", target = "companyName")
    ShiftResponseDto toDto(Shift entity);

    // DTO → ENTITY (ilişkileri service katmanı set edecek)
    @Mapping(target = "company", ignore = true)
    Shift toEntity(ShiftRequestDto dto);

    // MapStruct için yardımcı dönüşümler: eğer bir tarafta LocalDateTime diğer tarafta LocalTime varsa
    default LocalTime map(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalTime();
    }

    default LocalDateTime map(LocalTime time) {
        // LocalTime -> LocalDateTime dönüşümü için net bir tarih yok; burada bugünkü tarihi kullanıyoruz.
        // Bu metod sadece MapStruct'un kod üretimi için eklendi. Business layer'da tarih mantığı gerektiğinde açıkça handle edin.
        return time == null ? null : LocalDateTime.of(LocalDate.now(), time);
    }
}
