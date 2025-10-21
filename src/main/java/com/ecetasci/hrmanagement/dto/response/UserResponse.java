package com.ecetasci.hrmanagement.dto.response;

import com.ecetasci.hrmanagement.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponse(Long id, String name, LocalDateTime createdAt, LocalDateTime updatedAt, String email, Role role) {
}
