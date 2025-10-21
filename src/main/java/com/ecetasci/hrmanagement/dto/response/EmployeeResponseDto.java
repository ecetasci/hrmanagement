package com.ecetasci.hrmanagement.dto.response;

public record EmployeeResponseDto(Long id,
                                  String employeeNumber,
                                  String name,
                                  String email,
                                  String position,
                                  String department
                                  ) {
}
