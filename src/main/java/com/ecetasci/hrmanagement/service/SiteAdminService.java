package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.CompanyRequest;
import com.ecetasci.hrmanagement.dto.request.ExpiringSubscriptionDto;
import com.ecetasci.hrmanagement.dto.request.SubscriptionRequestDto;
import com.ecetasci.hrmanagement.dto.response.AdminDashboardResponse;
import com.ecetasci.hrmanagement.dto.response.CompanyResponse;
import com.ecetasci.hrmanagement.dto.response.SubscriptionResponseDto;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.CompanySubscription;
import com.ecetasci.hrmanagement.entity.User;
import com.ecetasci.hrmanagement.enums.CompanyStatus;
import com.ecetasci.hrmanagement.enums.Role;
import com.ecetasci.hrmanagement.enums.SubscriptionType;
import com.ecetasci.hrmanagement.enums.UserStatus;
import com.ecetasci.hrmanagement.repository.CompanyRepository;

import com.ecetasci.hrmanagement.repository.CompanySubscriptionRepository;
import com.ecetasci.hrmanagement.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteAdminService {
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    private final CompanySubscriptionRepository companySubscriptionRepository;
    private final EmailService emailService;

    private List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }


    @Transactional
    public CompanyResponse createApplication(CompanyRequest companyRequest) {
        Company company = new Company();
        company.setCompanyName(companyRequest.companyName());
        company.setAddress(companyRequest.address());
        company.setCompanyEmail(companyRequest.companyEmail());
        company.setPhoneNumber(companyRequest.phoneNumber());
        company.setWebsite(companyRequest.website());
        company.setCreatedAt(LocalDateTime.now());
         Company saved = companyRepository.save(company);

        CompanyResponse companyResponse = new CompanyResponse(
               saved.getId(),
                saved.getCompanyName(),
                saved.getCompanyEmail(),
                saved.getPhoneNumber(),
                saved.getAddress(),
                saved.getTaxNumber(),
                saved.getWebsite(),
                saved.getEmployeeCount(),
                saved.getFoundedDate()

        );

        userRepository.findAllByRole(Role.SITE_ADMIN).forEach
                (user -> {
                    String subject = "New Company Application Submitted";
                    String body = "A new company application has been submitted by " + saved.getCompanyName() +
                            ". Please review and approve or reject the application in the admin panel.";
                   // emailService.send(user.getEmail(), subject, body);
                });

        return companyResponse;
    }

    public List<Company> getApprovedCompanies(){
        return companyRepository.findAllByCompanyStatus(CompanyStatus.APPROVED);
    }

    public void approveCompanyApplication(Long id) {
    Company company = companyRepository.findById(id).orElseThrow(()->new RuntimeException("Company not found"));
    company.setCompanyStatus(CompanyStatus.APPROVED);
    companyRepository.save(company);
    emailService.send(
            company.getCompanyEmail(),
            "Company Application Approved",
            "Congratulations! Your company application has been approved." +
                    " Please select and create your subscription plan to get started."
    );
    }


    public void rejectCompanyApplication(Long id) {
        Company company = companyRepository.findById(id).orElseThrow(()->new RuntimeException("Company not found"));
        company.setCompanyStatus(CompanyStatus.REJECTED);
         companyRepository.save(company);
        emailService.send(
                company.getCompanyEmail(),
                "Company Application Rejected",
                "We regret to inform you that your company application has been rejected." +
                        " For more information, please contact our support team."
        );

    }

    ////Subscriptions///

    @Transactional
    public SubscriptionResponseDto createSubscription(SubscriptionRequestDto subscriptionRequestDto) {
        Company company = companyRepository.findById(subscriptionRequestDto.companyId()).orElseThrow();
        CompanySubscription companySubscription = new CompanySubscription();
        companySubscription.setCompany(company);
        companySubscription.setStartDate(subscriptionRequestDto.startDate());
        companySubscription.setSubscriptionType(subscriptionRequestDto.subscriptionType());
        companySubscription.setPrice(subscriptionRequestDto.price());
        companySubscription.setCreatedAt(LocalDateTime.now());
        companySubscription.setEndDate(subscriptionRequestDto.endDate());

        company.getSubscriptions().add(companySubscription);
        // companyRepository.save(company);//fetchden dolayı sanırım bu iki kayda neden oldu
        CompanySubscription saved = companySubscriptionRepository.save(companySubscription);

        return new SubscriptionResponseDto(saved.getId(), "Subscription saved");


    }
}
