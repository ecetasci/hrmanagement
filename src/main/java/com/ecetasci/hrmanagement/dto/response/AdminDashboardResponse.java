package com.ecetasci.hrmanagement.dto.response;

import com.ecetasci.hrmanagement.dto.request.ExpiringSubscriptionDto;

import java.util.List;

public record AdminDashboardResponse(Long totalCompanies,
                                     Long totalUsers,
                                     List<ExpiringSubscriptionDto> expiringSubscriptions) {}