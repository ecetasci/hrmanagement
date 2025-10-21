package com.ecetasci.hrmanagement.dto.request;

import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

//şirket yöneticisi kaydı için
public record RegisterCompanyManagerRequestDto(@NotBlank(message = "Name cannot be null") String name,
                                               @NotBlank String password,
                                               @NotBlank @Email String email,
                                               @NotNull Role role,
                                               Long companyId) {

    public RegisterCompanyManagerRequestDto(String name, String password,String email, Role role, Long companyId) {
        if (role != Role.COMPANY_ADMIN) {
            throw new IllegalArgumentException("Role must be COMPANY_ADMIN for company registration");
        }
        this.name = name;
        this.password = password;
        this.role = role;
        this.email=email;
        this.companyId=companyId;
    }
}
