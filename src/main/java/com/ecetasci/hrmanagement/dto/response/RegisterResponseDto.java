package com.ecetasci.hrmanagement.dto.response;

import com.ecetasci.hrmanagement.enums.Role;
import jakarta.validation.constraints.NotBlank;

public record RegisterResponseDto(@NotBlank String username, Long id, String email) {
}