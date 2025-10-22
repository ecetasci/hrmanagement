package com.ecetasci.hrmanagement.dto.request;

import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.enums.Role;
import jakarta.validation.constraints.NotBlank;

public record RegisterEmployeeRequestDto(@NotBlank String name, Long companyId, @NotBlank String password,
                                          Role role, String email, String position, String department) {

    public RegisterEmployeeRequestDto(String name, Long companyId,String password,Role role, String email, String position, String department) {
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
    }
}
