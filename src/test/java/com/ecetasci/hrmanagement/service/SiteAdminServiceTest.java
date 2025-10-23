package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.SubscriptionRequestDto;
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
        company.setSubscriptions(new ArrayList<>());
    }

    // approveCompanyApplication
    @Test
    void approveCompanyApplication_whenPending_setsActiveAndCompanyAdmin_andSaves() {
        User user = new User();
        user.setId(10L);
        user.setUserStatus(UserStatus.PENDING_ADMIN_APPROVAL);
        user.setRole(null);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.approveCompanyApplication(10L);

        assertEquals(UserStatus.ACTIVE, user.getUserStatus());
        assertEquals(Role.COMPANY_ADMIN, user.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void approveCompanyApplication_whenNotPending_leavesAsIs_andSaves() {
        User user = new User();
        user.setId(11L);
        user.setUserStatus(UserStatus.ACTIVE);
        user.setRole(Role.EMPLOYEE);
        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.approveCompanyApplication(11L);

        assertEquals(UserStatus.ACTIVE, user.getUserStatus());
        assertEquals(Role.EMPLOYEE, user.getRole());
        verify(userRepository).save(user);
    }

    // rejectCompanyApplication
    @Test
    void rejectCompanyApplication_setsRejectedAndNullRole_andSaves() {
        User user = new User();
        user.setId(12L);
        user.setUserStatus(UserStatus.PENDING_ADMIN_APPROVAL);
        user.setRole(Role.EMPLOYEE);
        when(userRepository.findById(12L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.rejectCompanyApplication(12L);

        assertEquals(UserStatus.REJECTED, user.getUserStatus());
        assertNull(user.getRole());
        verify(userRepository).save(user);
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
