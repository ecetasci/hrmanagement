package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.SubscriptionRequestDto;
import com.ecetasci.hrmanagement.dto.response.SubscriptionResponseDto;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.CompanySubscription;

import com.ecetasci.hrmanagement.enums.SubscriptionType;

import com.ecetasci.hrmanagement.repository.CompanyRepository;
import com.ecetasci.hrmanagement.repository.CompanySubscriptionRepository;
import com.ecetasci.hrmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiteAdminServiceTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private UserRepository userRepository;
    @Mock private CompanySubscriptionRepository companySubscriptionRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private SiteAdminService service;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setCompanyName("ACME");
        company.setCompanyEmail("acme@x.com");
        company.setSubscriptions(new ArrayList<>());
    }

    // approveCompanyApplication (company-based in service)
    @Test
    void approveCompanyApplication_whenPending_setsApprovedAndSendsEmail_andSaves() {
        company.setCompanyStatus(null);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        service.approveCompanyApplication(1L);

        assertEquals(com.ecetasci.hrmanagement.enums.CompanyStatus.APPROVED, company.getCompanyStatus());
        verify(companyRepository).save(company);
        verify(emailService).send(eq(company.getCompanyEmail()), contains("Approved"), anyString());
    }

    @Test
    void rejectCompanyApplication_setsRejectedAndSendsEmail_andSaves() {
        when(companyRepository.findById(2L)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        service.rejectCompanyApplication(2L);

        assertEquals(com.ecetasci.hrmanagement.enums.CompanyStatus.REJECTED, company.getCompanyStatus());
        verify(companyRepository).save(company);
        verify(emailService).send(eq(company.getCompanyEmail()), contains("Rejected"), anyString());
    }

    // createSubscription
    @Test
    void createSubscription_companyNotFound_throws() {
        SubscriptionRequestDto req = new SubscriptionRequestDto(99L, SubscriptionType.MONTHLY, BigDecimal.TEN, LocalDate.now(), LocalDate.now().plusDays(30));
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.createSubscription(req));
        verify(companySubscriptionRepository, never()).save(any());
    }

    @Test
    void createSubscription_success_savesSubscription_andReturnsResponse() {
        SubscriptionRequestDto req = new SubscriptionRequestDto(1L, SubscriptionType.YEARLY, BigDecimal.valueOf(199.99), LocalDate.of(2025,1,1), LocalDate.of(2025,12,31));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companySubscriptionRepository.save(any(CompanySubscription.class))).thenAnswer(inv -> {
            CompanySubscription cs = inv.getArgument(0);
            cs.setId(777L);
            return cs;
        });

        SubscriptionResponseDto res = service.createSubscription(req);

        assertEquals(777L, res.subId());
        assertEquals("Subscription saved", res.message());

        // subscription alanları doğru set edilmiş mi?
        ArgumentCaptor<CompanySubscription> captor = ArgumentCaptor.forClass(CompanySubscription.class);
        verify(companySubscriptionRepository).save(captor.capture());
        CompanySubscription saved = captor.getValue();
        assertEquals(company, saved.getCompany());
        assertEquals(req.subscriptionType(), saved.getSubscriptionType());
        assertEquals(req.price(), saved.getPrice());
        assertEquals(req.startDate(), saved.getStartDate());
        assertEquals(req.endDate(), saved.getEndDate());

        // şirket listesine eklendi mi?
        assertTrue(company.getSubscriptions().contains(saved));
    }
}
