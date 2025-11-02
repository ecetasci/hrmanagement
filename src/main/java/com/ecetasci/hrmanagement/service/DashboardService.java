package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.CalendarLeaveDto;
import com.ecetasci.hrmanagement.dto.request.CalendarShiftDto;
import com.ecetasci.hrmanagement.dto.request.ExpiringSubscriptionDto;
import com.ecetasci.hrmanagement.dto.request.UpcomingBirthdayDto;
import com.ecetasci.hrmanagement.dto.response.*;
import com.ecetasci.hrmanagement.entity.*;
import com.ecetasci.hrmanagement.enums.*;
import com.ecetasci.hrmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final CompanySubscriptionRepository companySubscriptionRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ExpenseRepository expenseRepository;
    private final EmployeeShiftRepository employeeShiftRepository;

    // 🧭 Site Admin Dashboard
    public AdminDashboardResponse getAdminDashboard() {
        LocalDate today = LocalDate.now();
        LocalDate soon = today.plusDays(15);

        List<ExpiringSubscriptionDto> expiringList = new ArrayList<>();
        List<CompanySubscription> subs = companySubscriptionRepository.findByEndDateBetween(today, soon);
        for (CompanySubscription s : subs) {
            ExpiringSubscriptionDto dto = new ExpiringSubscriptionDto(
                    s.getCompany().getCompanyName(),
                    s.getEndDate()
            );
            expiringList.add(dto);
        }

        return new AdminDashboardResponse(
                companyRepository.count(),
                userRepository.count(),
                expiringList
        );
    }

    // Company Dashboard
    public CompanyDashboardResponse getCompanyDashboard(Long companyId) {
        LocalDate today = LocalDate.now();
        LocalDate upcoming = today.plusDays(7);

        List<Employee> employees = employeeRepository.findByCompany_Id(companyId);
        List<UpcomingBirthdayDto> birthdays = new ArrayList<>();

        for (Employee e : employees) {
            if (e.getBirthDate() != null &&
                !e.getBirthDate().isBefore(today) &&
                e.getBirthDate().isBefore(upcoming)) {
                birthdays.add(new UpcomingBirthdayDto(e.getName(), e.getBirthDate()));
            }
        }

        int pendingLeaves = leaveRequestRepository
                .findByEmployee_Company_IdAndStatus(companyId, LeaveStatus.PENDING)
                .size();

        int pendingExpenses = expenseRepository.findByEmployee_Company_IdAndStatus(companyId, ExpenseStatus.PENDING)
                .size();

        return new CompanyDashboardResponse(
                (long) employees.size(),
                birthdays,
                pendingLeaves,
                pendingExpenses
        );
    }

    // Employee Dashboard
    public EmployeeDashboardResponse getEmployeeDashboard(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Personel bulunamadı."));

        //
        List<EmployeeShift> shifts = employeeShiftRepository
                .findAllByEmployee_IdAndStartTimeBetween(employeeId, LocalTime.now(), LocalTime.now().plusHours(8));

        String nextShift = "Yaklaşan vardiya yok";
        if (!shifts.isEmpty()) {
            EmployeeShift s = shifts.get(0);
            nextShift = s.getShift().getName() + " (" + s.getStartTime() + ")";

        }

        List<Expense> expenses = expenseRepository.findTop3ByEmployee_IdOrderByExpenseDateDesc(employeeId);
        List<String> lastExpenses = new ArrayList<>();
        for (Expense e : expenses) {
            lastExpenses.add(e.getDescription());
        }

        return new EmployeeDashboardResponse(
                employee.getName(),
                employee.getLeaveBalance(),
                nextShift,
                lastExpenses
        );
    }

    //  Calendar
    public EmployeeCalendarResponse getEmployeeCalendar(Long employeeId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<CalendarLeaveDto> leaveList = new ArrayList<>();
        List<LeaveRequest> leaves = leaveRequestRepository.findByEmployee_Id(employeeId);
        for (LeaveRequest l : leaves) {
            if (l.getStatus() == LeaveStatus.APPROVED &&
                !l.getStartDate().isAfter(end) &&
                !l.getEndDate().isBefore(start)) {
                leaveList.add(new CalendarLeaveDto(l.getStartDate(), l.getEndDate(), l.getLeaveType().getName()));
            }
        }

        List<CalendarShiftDto> shiftList = new ArrayList<>();
        //// Fetch shifts between start and end dates
        // Use assignedDate range for calendar month view — this maps a shift to the date it was assigned
        List<EmployeeShift> shifts = employeeShiftRepository.findByEmployee_IdAndAssignedDateBetween(employeeId, start, end);
        for (EmployeeShift s : shifts) {
            shiftList.add(new CalendarShiftDto(s.getAssignedDate(), s.getShift().getName()));
        }

        return new EmployeeCalendarResponse(start.getMonth().name(), leaveList, shiftList);
    }
}
