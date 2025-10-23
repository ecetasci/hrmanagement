package com.ecetasci.hrmanagement.controller;

import com.ecetasci.hrmanagement.dto.response.*;
import com.ecetasci.hrmanagement.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * DashboardController — yönetici, şirket ve çalışan panoları için endpointler.
 *
 * Sağladığı işlevler:
 * - Yönetici, şirket ve çalışan dashboard verilerini döner
 * - Çalışan takvimi (calendar) verisini yıl/ay bazında döner
 */
@RestController
@RequestMapping("api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;


    /**
     * Yönetici (site admin) için dashboard özet verisini döner.
     *
     * @return AdminDashboardResponse
     */
    @GetMapping("/admin/dashboard")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }


    /**
     * Belirtilen şirket için dashboard verisini döner.
     *
     * @param companyId Şirket ID'si
     * @return CompanyDashboardResponse
     */
    @GetMapping("/company/dashboard/{companyId}")
    public ResponseEntity<CompanyDashboardResponse> getCompanyDashboard(@PathVariable Long companyId) {
        return ResponseEntity.ok(dashboardService.getCompanyDashboard(companyId));
    }


    /**
     * Belirtilen çalışan için dashboard verisini döner.
     *
     * @param employeeId Çalışan ID'si
     * @return EmployeeDashboardResponse
     */
    @GetMapping("/employee/dashboard/{employeeId}")
    public ResponseEntity<EmployeeDashboardResponse> getEmployeeDashboard(@PathVariable Long employeeId) {
        return ResponseEntity.ok(dashboardService.getEmployeeDashboard(employeeId));
    }


    /**
     * Çalışanın yıl/ay bazlı takvim bilgisini döner.
     *
     * @param year Yıl
     * @param month Ay (1-12)
     * @param employeeId Çalışan ID'si (request param)
     * @return EmployeeCalendarResponse
     */
    @GetMapping("/employee/calendar/{year}/{month}")
    public ResponseEntity<EmployeeCalendarResponse> getEmployeeCalendar(
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam Long employeeId) {
        return ResponseEntity.ok(dashboardService.getEmployeeCalendar(employeeId, year, month));
    }
}
