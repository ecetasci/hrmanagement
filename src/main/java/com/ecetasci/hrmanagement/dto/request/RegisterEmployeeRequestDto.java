package com.ecetasci.hrmanagement.dto.request;

import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.enums.Role;
import jakarta.validation.constraints.NotBlank;

public record RegisterEmployeeRequestDto(@NotBlank String name, @NotBlank Company company, @NotBlank String password,
                                         @NotBlank Role role) {

    public RegisterEmployeeRequestDto(String name, Company company,String password,Role role) {
        if (role != Role.EMPLOYEE) {
            throw new IllegalArgumentException("Role must be EMPLOYEE for employee registration");
        }
        this.name = name;
        this.company = company;
        this.password = password;
        this.role = role;
    }
}
