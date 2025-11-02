package com.ecetasci.hrmanagement.dto.response;

import com.ecetasci.hrmanagement.entity.Company;

import java.time.LocalTime;


public record ShiftResponseDto(Long id,
                               String name,
                               LocalTime startTime,
                               LocalTime endTime,
                               Long companyId,
                               String companyName) {
    public void setCompany(Company companyNotFound) {
    }
    public void setCompanyId(Long companyId) {
    }
}
