package com.ecetasci.hrmanagement.controller;

import com.ecetasci.hrmanagement.dto.response.*;
import com.ecetasci.hrmanagement.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;


    @GetMapping("/admin/dashboard")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }


    @GetMapping("/company/dashboard/{companyId}")
    public ResponseEntity<CompanyDashboardResponse> getCompanyDashboard(@PathVariable Long companyId) {
        return ResponseEntity.ok(dashboardService.getCompanyDashboard(companyId));
    }


    @GetMapping("/employee/dashboard/{employeeId}")
    public ResponseEntity<EmployeeDashboardResponse> getEmployeeDashboard(@PathVariable Long employeeId) {
        return ResponseEntity.ok(dashboardService.getEmployeeDashboard(employeeId));
    }


    @GetMapping("/employee/calendar/{year}/{month}")
    public ResponseEntity<EmployeeCalendarResponse> getEmployeeCalendar(
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam Long employeeId) {
        return ResponseEntity.ok(dashboardService.getEmployeeCalendar(employeeId, year, month));
    }
}
