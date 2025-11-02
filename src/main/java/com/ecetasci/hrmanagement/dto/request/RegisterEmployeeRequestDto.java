package com.ecetasci.hrmanagement.dto.request;

import com.ecetasci.hrmanagement.enums.Role;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterEmployeeRequestDto(@NotBlank String name, Long companyId, @NotBlank String password,
                                         Role role, String email, String position, String department, LocalDate birthDate, LocalDate hiredDay, BigDecimal salary,  String phoneNumber, String address, String emergencyContact
                                         ) {

    public RegisterEmployeeRequestDto(String name, Long companyId,String password,Role role, String email, String position, String department,LocalDate birthDate, LocalDate hiredDay, BigDecimal salary,  String phoneNumber, String address, String emergencyContact) {
        if (role != Role.EMPLOYEE) {
            throw new IllegalArgumentException("Role must be EMPLOYEE for employee registration");
        }
        this.name = name;
        this.companyId = companyId;
        this.password = password;
        this.role = role;
        this.email = email;
        this.position = position;
        this.department = department;
        this.birthDate= birthDate;
        this.hiredDay = hiredDay;
        this.salary = salary;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.emergencyContact = emergencyContact;

    }
}
