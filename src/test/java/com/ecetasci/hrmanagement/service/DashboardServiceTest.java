package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.CalendarLeaveDto;
import com.ecetasci.hrmanagement.dto.request.CalendarShiftDto;
import com.ecetasci.hrmanagement.dto.request.UpcomingBirthdayDto;
import com.ecetasci.hrmanagement.dto.response.AdminDashboardResponse;
import com.ecetasci.hrmanagement.dto.response.CompanyDashboardResponse;
import com.ecetasci.hrmanagement.dto.response.EmployeeCalendarResponse;
import com.ecetasci.hrmanagement.dto.response.EmployeeDashboardResponse;
import com.ecetasci.hrmanagement.entity.*;
import com.ecetasci.hrmanagement.enums.ExpenseStatus;
import com.ecetasci.hrmanagement.enums.LeaveStatus;
import com.ecetasci.hrmanagement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private UserRepository userRepository;
    @Mock private CompanySubscriptionRepository companySubscriptionRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private EmployeeShiftRepository employeeShiftRepository;

    @InjectMocks
    private DashboardService service;

    private Company company;
    private Employee employee;

    @BeforeEach
    void setup() {
        company = new Company();
        company.setId(1L);
        company.setCompanyName("ACME");

        employee = new Employee();
        employee.setId(10L);
        employee.setName("John Doe");
        employee.setLeaveBalance(12);
        employee.setCompany(company);
        employee.setEmail("john@example.com");
    }

    @Test
    void getAdminDashboard_returnsCountsAndExpiringSubs() {
        LocalDate today = LocalDate.now();
        LocalDate within = today.plusDays(5);
        LocalDate within2 = today.plusDays(10);

        CompanySubscription s1 = buildSub(company, within);
        CompanySubscription s2 = buildSub(company, within2);

        when(companyRepository.count()).thenReturn(7L);
        when(userRepository.count()).thenReturn(25L);
        when(companySubscriptionRepository.findByEndDateBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(s1, s2));

        AdminDashboardResponse res = service.getAdminDashboard();

        assertEquals(7L, res.totalCompanies());
        assertEquals(25L, res.totalUsers());
        assertEquals(2, res.expiringSubscriptions().size());
        assertEquals("ACME", res.expiringSubscriptions().get(0).companyName());
        assertEquals(within, res.expiringSubscriptions().get(0).endDate());
    }

    @Test
    void getCompanyDashboard_filtersBirthdaysAndCountsPendings() {
        LocalDate today = LocalDate.now();
        LocalDate upcoming = today.plusDays(7);

        Employee ePast = new Employee(); ePast.setName("Past"); ePast.setBirthDate(today.minusDays(1));
        Employee eToday = new Employee(); eToday.setName("Today"); eToday.setBirthDate(today);
        Employee eEdge = new Employee(); eEdge.setName("Edge"); eEdge.setBirthDate(upcoming); // excluded (isBefore(upcoming) false)
        Employee eMid = new Employee(); eMid.setName("Mid"); eMid.setBirthDate(today.plusDays(3));
        Employee eNull = new Employee(); eNull.setName("Null"); eNull.setBirthDate(null);

        when(employeeRepository.findByCompany_Id(1L)).thenReturn(List.of(ePast, eToday, eEdge, eMid, eNull));
        when(leaveRequestRepository.findByEmployee_Company_IdAndStatus(1L, LeaveStatus.PENDING))
                .thenReturn(List.of(new LeaveRequest(), new LeaveRequest())); // size = 2
        when(expenseRepository.findByEmployee_Company_IdAndStatus(1L, ExpenseStatus.PENDING))
                .thenReturn(List.of(new Expense())); // size = 1

        CompanyDashboardResponse res = service.getCompanyDashboard(1L);

        assertEquals(5L, res.employeeCount());
        // birthdays should include Today and Mid only
        List<UpcomingBirthdayDto> bdays = res.upcomingBirthdays();
        assertEquals(2, bdays.size());
        assertTrue(bdays.stream().anyMatch(b -> b.name().equals("Today")));
        assertTrue(bdays.stream().anyMatch(b -> b.name().equals("Mid")));
        assertEquals(2, res.pendingLeaveRequests());
        assertEquals(1, res.pendingExpenses());
    }

    @Test
    void getEmployeeDashboard_employeeNotFound_throws() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getEmployeeDashboard(99L));
        assertEquals("Personel bulunamadı.", ex.getMessage());
    }

    @Test
    void getEmployeeDashboard_withShiftsAndExpenses_buildsSummary() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));

        // Shifts between now and now+7 (service picks first element's name and startDate)
        Shift shift = new Shift();
        shift.setName("Sabah Vardiyası");
        EmployeeShift sh1 = new EmployeeShift(); sh1.setShift(shift); sh1.setStartTime(LocalDateTime.now().plusDays(1));
        EmployeeShift sh2 = new EmployeeShift(); sh2.setShift(shift); sh2.setStartTime(LocalDateTime.now().plusDays(2));
        when(employeeShiftRepository.findByEmployee_IdAndStartTimeBetween(eq(10L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(sh1, sh2));

        Expense ex1 = Expense.builder().description("Laptop Case").amount(BigDecimal.TEN).expenseDate(LocalDate.now()).status(ExpenseStatus.APPROVED).employee(employee).build();
        Expense ex2 = Expense.builder().description("Taxi").amount(BigDecimal.ONE).expenseDate(LocalDate.now().minusDays(1)).status(ExpenseStatus.PENDING).employee(employee).build();
        when(expenseRepository.findTop3ByEmployee_IdOrderByExpenseDateDesc(10L)).thenReturn(List.of(ex1, ex2));

        EmployeeDashboardResponse res = service.getEmployeeDashboard(10L);

        assertEquals("John Doe", res.employeeName());
        assertEquals(12, res.leaveBalance());
        assertTrue(res.upcomingShift().contains("Sabah Vardiyası"));
        assertTrue(res.upcomingShift().contains(sh1.getStartTime().toString()));
        assertEquals(List.of("Laptop Case", "Taxi"), res.recentExpenses());
    }

    @Test
    void getEmployeeDashboard_noUpcomingShift_messageShown() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(employeeShiftRepository.findByEmployee_IdAndStartTimeBetween(eq(10L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(expenseRepository.findTop3ByEmployee_IdOrderByExpenseDateDesc(10L)).thenReturn(List.of());

        EmployeeDashboardResponse res = service.getEmployeeDashboard(10L);
        assertEquals("Yaklaşan vardiya yok", res.upcomingShift());
        assertEquals(0, res.recentExpenses().size());
    }

    @Test
    void getEmployeeCalendar_includesApprovedOverlappingLeaves_andShifts() {
        int year = 2025, month = 5; // May 2025
        Long empId = 10L;
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        // Leaves: only APPROVED and overlapping with [start, end]
        LeaveType annual = LeaveType.builder().name("Annual").build();
        LeaveRequest lr1 = LeaveRequest.builder()
                .employee(employee)
                .leaveType(annual)
                .startDate(start.minusDays(2))
                .endDate(start.plusDays(2))
                .totalDays(3)
                .status(LeaveStatus.APPROVED)
                .build(); // overlaps -> include
        LeaveRequest lr2 = LeaveRequest.builder()
                .employee(employee)
                .leaveType(annual)
                .startDate(end.plusDays(1))
                .endDate(end.plusDays(2))
                .totalDays(2)
                .status(LeaveStatus.APPROVED)
                .build(); // starts after -> exclude
        LeaveRequest lr3 = LeaveRequest.builder()
                .employee(employee)
                .leaveType(annual)
                .startDate(start.plusDays(5))
                .endDate(start.plusDays(6))
                .totalDays(2)
                .status(LeaveStatus.PENDING)
                .build(); // not approved -> exclude
        when(leaveRequestRepository.findByEmployee_Id(empId)).thenReturn(List.of(lr1, lr2, lr3));

        // Shifts in range
        Shift shift = new Shift(); shift.setName("Gece");
        EmployeeShift s1 = new EmployeeShift(); s1.setStartTime(start.plusDays(3).atStartOfDay()); s1.setShift(shift);
        EmployeeShift s2 = new EmployeeShift(); s2.setStartTime(end.atStartOfDay()); s2.setShift(shift);
        when(employeeShiftRepository.findByEmployee_IdAndStartTimeBetween(empId, start.atStartOfDay(), end.atStartOfDay()))
                .thenReturn(List.of(s1, s2));

        EmployeeCalendarResponse res = service.getEmployeeCalendar(empId, year, month);

        assertEquals(start.getMonth().name(), res.month());
        List<CalendarLeaveDto> leaves = res.leaves();
        assertEquals(1, leaves.size());
        assertEquals("Annual", leaves.get(0).type());
        assertEquals(lr1.getStartDate(), leaves.get(0).startDate());

        List<CalendarShiftDto> shifts = res.shifts();
        assertEquals(2, shifts.size());
        assertEquals("Gece", shifts.get(0).shiftName());
        assertEquals(end, shifts.get(1).date());
    }

    // helpers
    private CompanySubscription buildSub(Company c, LocalDate endDate) {
        CompanySubscription s = new CompanySubscription();
        s.setCompany(c);
        s.setEndDate(endDate);
        s.setPrice(BigDecimal.TEN);
        return s;
    }
}
