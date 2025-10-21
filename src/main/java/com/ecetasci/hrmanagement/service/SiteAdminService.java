package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.ExpiringSubscriptionDto;
import com.ecetasci.hrmanagement.dto.request.SubscriptionRequestDto;
import com.ecetasci.hrmanagement.dto.response.AdminDashboardResponse;
import com.ecetasci.hrmanagement.dto.response.SubscriptionResponseDto;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.CompanySubscription;
import com.ecetasci.hrmanagement.entity.User;
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

    //TO DO private List<Company> getApprovedCompanies(){ }

    public void approveCompanyApplication(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        if (user.getUserStatus().equals(UserStatus.PENDING_ADMIN_APPROVAL)) {
            user.setUserStatus(UserStatus.ACTIVE);
            user.setRole(Role.COMPANY_ADMIN);
        }
        userRepository.save(user);
    }


    public void rejectCompanyApplication(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setUserStatus(UserStatus.REJECTED);
        user.setRole(null);
        // emailService.send("","","");
        userRepository.save(user);

    }

    ////Subscriptions///

    @Transactional
    public SubscriptionResponseDto createSubscription( SubscriptionRequestDto subscriptionRequestDto) {
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
