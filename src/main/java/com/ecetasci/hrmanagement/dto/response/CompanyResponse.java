package com.ecetasci.hrmanagement.dto.response;

import com.ecetasci.hrmanagement.entity.CompanyReview;
import com.ecetasci.hrmanagement.entity.CompanySubscription;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.enums.UserStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;

public record CompanyResponse(Long id,String companyName,
                              String companyEmail,String phoneNumber,
                              String address,String taxNumber,String website,Integer employeeCount,LocalDate foundedDate) {
}










