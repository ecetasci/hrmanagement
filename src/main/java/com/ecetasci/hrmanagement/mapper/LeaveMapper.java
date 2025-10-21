package com.ecetasci.hrmanagement.mapper;

import com.ecetasci.hrmanagement.dto.request.LeaveRequestDto;
import com.ecetasci.hrmanagement.dto.response.LeaveResponseDto;
import com.ecetasci.hrmanagement.entity.LeaveRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LeaveMapper {

    @Mapping(source = "employee.employeeNumber", target = "employeeNumber")
    @Mapping(source = "leaveType.name", target = "leaveTypeName")
    @Mapping(source = "status", target = "status")
    LeaveResponseDto toDto(LeaveRequest entity);

    @Mapping(target = "employee", ignore = true)   // service set edecek
    @Mapping(target = "leaveType", ignore = true)  // service set edecek
    @Mapping(target = "totalDays", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "managerNote", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(source = "startDate", target = "startDate")
    @Mapping(source = "endDate", target = "endDate")
    LeaveRequest toEntity(LeaveRequestDto dto);
}
