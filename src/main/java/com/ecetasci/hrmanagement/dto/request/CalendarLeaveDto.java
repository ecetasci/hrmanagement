package com.ecetasci.hrmanagement.dto.request;

import java.time.LocalDate;

public record CalendarLeaveDto(LocalDate startDate, LocalDate endDate, String type) {}