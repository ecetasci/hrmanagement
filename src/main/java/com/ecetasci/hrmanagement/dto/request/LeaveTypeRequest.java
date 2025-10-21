package com.ecetasci.hrmanagement.dto.request;

public record LeaveTypeRequest(Long companyId, String name, String description,Boolean isPaid, Integer maxDay) {
}
