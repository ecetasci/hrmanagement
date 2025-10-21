package com.ecetasci.hrmanagement.dto.response;

import java.util.List;

public record EmployeeDashboardResponse(String employeeName,
                                        Integer leaveBalance,
                                        String upcomingShift,
                                        List<String> recentExpenses) {}
