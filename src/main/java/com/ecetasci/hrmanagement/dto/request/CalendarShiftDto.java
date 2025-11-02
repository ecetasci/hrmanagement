package com.ecetasci.hrmanagement.dto.request;

import java.time.LocalDate;


public record CalendarShiftDto(LocalDate date, String shiftName) {}