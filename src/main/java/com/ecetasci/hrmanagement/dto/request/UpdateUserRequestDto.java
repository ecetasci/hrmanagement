package com.ecetasci.hrmanagement.dto.request;

import jakarta.validation.constraints.*;

public record UpdateUserRequestDto(
		Long id,
		String username,
		@NotBlank
		@Pattern(message = "Yeni şifre en az 8 karakter olmalı. Şifrenizde en az 1 " +
				"büyük karakter 1 küçük karakter 1 sayı ve 1 özel karakter olmalıdır",
				regexp ="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.])" +
						"[A-Za-z\\d@$!%*?&]{8,}$"
		)
		String password,
		@Email(message = "Email formatınızı kontrol ediniz.")
		String email
		) {
}