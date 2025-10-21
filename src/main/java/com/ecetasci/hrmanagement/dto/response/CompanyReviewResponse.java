package com.ecetasci.hrmanagement.dto.response;

import lombok.Data;


public record CompanyReviewResponse(String companyName,String title, String content, Integer rating) {
}
