package com.ecetasci.hrmanagement.dto.request;

import com.ecetasci.hrmanagement.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

//şirket yöneticisi kaydı için
public class RegisterCompanyManagerRequestDto {
    @NotBlank(message = "Name cannot be null")
    private String name;
    @NotBlank
    private String password;
    @NotBlank @Email
    private String email;
    @NotNull
    private Role role;
    private Long companyId;

    public RegisterCompanyManagerRequestDto() {
        // For deserialization
    }

    public RegisterCompanyManagerRequestDto(String name, String password, String email, Role role, Long companyId) {
        if (role == null || role != Role.COMPANY_ADMIN) {
            throw new IllegalArgumentException("Role must be COMPANY_ADMIN for company registration");
        }
        this.name = name;
        this.password = password;
        this.role = role;
        this.email = email;
        this.companyId = companyId;
    }

    @NotBlank(message = "Name cannot be null")
    public String getName() {
        return name;
    }

    public void setName(@NotBlank(message = "Name cannot be null") String name) {
        this.name = name;
    }

    public @NotBlank String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank String password) {
        this.password = password;
    }

    public @NotBlank @Email String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank @Email String email) {
        this.email = email;
    }

    public @NotNull Role getRole() {
        return role;
    }

    public void setRole(@NotNull Role role) {
        this.role = role;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long compId) {
        this.companyId = compId;
    }
}
