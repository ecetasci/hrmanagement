package com.ecetasci.hrmanagement.dto.request;

import java.time.LocalDateTime;

public record CalendarShiftDto(LocalDateTime date, String shiftName) {}