package com.ecetasci.hrmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class ResetPasswordRequestDto {
        @NotBlank(message = "Token cannot be blank")
        private String token;

        @NotBlank(message = "New password cannot be blank")
        private String newPassword;
    }


