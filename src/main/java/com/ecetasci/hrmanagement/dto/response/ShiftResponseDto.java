package com.ecetasci.hrmanagement.dto.response;

import com.ecetasci.hrmanagement.entity.Company;

import java.time.LocalDateTime;


public record ShiftResponseDto(Long id,
                               String name,
                               LocalDateTime startTime,
                               LocalDateTime endTime,
                               Long companyId,
                               String companyName) {
    public void setCompany(Company companyNotFound) {
    }
    public void setCompanyId(Long companyId) {
    }
}

