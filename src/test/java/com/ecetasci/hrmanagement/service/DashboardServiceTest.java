package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.CompanySubscription;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private UserRepository userRepository;
    @Mock private CompanySubscriptionRepository companySubscriptionRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private ExpenseRepository expenseRepository;

    @InjectMocks
    private DashboardService service;

    @Test
    void getAdminDashboard_returnsCountsAndExpiring() {
        when(companyRepository.count()).thenReturn(5L);
        when(userRepository.count()).thenReturn(10L);
        CompanySubscription cs = new CompanySubscription();
        Company c = new Company(); c.setCompanyName("ACME");
        cs.setCompany(c);
        cs.setId(1L);
        when(companySubscriptionRepository.findByEndDateBetween(any(), any())).thenReturn(List.of(cs));

        var res = service.getAdminDashboard();

        assertEquals(5L, res.totalCompanies());
        assertEquals(10L, res.totalUsers());
        assertNotNull(res.expiringSubscriptions());
        assertEquals(1, res.expiringSubscriptions().size());
    }

    @Test
    void getCompanyDashboard_countsPending() {
        Long companyId = 2L;
        Company co = new Company(); co.setId(companyId);
        Employee e = new Employee(); e.setId(1L); e.setName("E"); e.setCompany(co);
        when(employeeRepository.findByCompany_Id(companyId)).thenReturn(List.of(e));
        when(leaveRequestRepository.findByEmployee_Company_IdAndStatus(eq(companyId), any())).thenReturn(List.of());
        when(expenseRepository.findByEmployee_Company_IdAndStatus(eq(companyId), any())).thenReturn(List.of());

        var res = service.getCompanyDashboard(companyId);
        assertEquals(1L, res.employeeCount());
        assertEquals(0, res.pendingLeaveRequests());
        assertEquals(0, res.pendingExpenses());
    }
}
