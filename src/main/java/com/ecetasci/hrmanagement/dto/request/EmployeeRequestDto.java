package com.ecetasci.hrmanagement.dto.request;

import jakarta.validation.constraints.Email;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeRequestDto(String name,
                                 @Email String email,
                                 String password,
                                 LocalDate birthDate,
                                 LocalDate hireDate,
                                 String position,
                                 String department,
                                 BigDecimal salary,
                                 String phoneNumber,
                                 String address,
                                 String emergencyContact) {
}
