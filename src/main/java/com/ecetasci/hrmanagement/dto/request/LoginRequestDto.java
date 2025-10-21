package com.ecetasci.hrmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(String name,@Email String email, @NotBlank String password) {
}
