package com.ecetasci.hrmanagement.dto.request;

import com.ecetasci.hrmanagement.enums.Role;
import jakarta.validation.constraints.*;

public record RegisterRequestDto(@NotNull @Size(min = 3,max = 64,message = "3-64 karakter olmalı") String username,
                                 @NotBlank
                                 @Pattern(message = "Şifre en az 8 karakter olmalı. Şifrenizde en az 1 " +
		                                 "büyük karakter 1 küçük karakter 1 sayı ve 1 özel karakter olmalıdır",
		                                 regexp ="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.])" +
				                                 "[A-Za-z\\d@$!%*?&]{8,}$"
                                 ) String password,
                                 @NotBlank String rePassword,
								 @NotNull Role role,
                                 @Email(message = "Email formatınızı kontrol ediniz.") String email,
								 Long companyId) {
}