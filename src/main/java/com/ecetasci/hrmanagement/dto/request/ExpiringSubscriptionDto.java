package com.ecetasci.hrmanagement.dto.request;

import java.time.LocalDate;

public record ExpiringSubscriptionDto(String companyName, LocalDate endDate) {}
