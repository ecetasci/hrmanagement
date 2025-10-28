package com.ecetasci.hrmanagement.dto.request;

import com.ecetasci.hrmanagement.enums.Role;
import jakarta.validation.constraints.NotBlank;

public record CompanyRequest(@NotBlank String companyName,  String address,String companyEmail, String phoneNumber, String taxNumber, String website) {
}
