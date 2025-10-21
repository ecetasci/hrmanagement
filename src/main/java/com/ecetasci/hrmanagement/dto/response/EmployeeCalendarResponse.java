package com.ecetasci.hrmanagement.dto.response;

import com.ecetasci.hrmanagement.dto.request.CalendarLeaveDto;
import com.ecetasci.hrmanagement.dto.request.CalendarShiftDto;

import java.util.List;

public record EmployeeCalendarResponse(String month,
                                       List<CalendarLeaveDto> leaves,
                                       List<CalendarShiftDto> shifts) {}
