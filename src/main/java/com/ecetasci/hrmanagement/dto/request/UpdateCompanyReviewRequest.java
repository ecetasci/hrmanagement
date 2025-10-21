package com.ecetasci.hrmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateCompanyReviewRequest(@NotNull Long companyId, String title, String content, Integer rating) {
}
