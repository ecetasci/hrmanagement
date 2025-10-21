package com.ecetasci.hrmanagement.dto.response;

import com.ecetasci.hrmanagement.dto.request.UpcomingBirthdayDto;

import java.util.List;

public record CompanyDashboardResponse(Long employeeCount,
                                       List<UpcomingBirthdayDto> upcomingBirthdays,
                                       Integer pendingLeaveRequests,
                                       Integer pendingExpenses) {}