package com.ecetasci.hrmanagement.dto.response;



import java.time.LocalDate;

public record LeaveResponseDto(
        Long id,                     // Talebin DB'deki id'si
        String employeeNumber,       // Çalışan numarası
        String leaveTypeName,        // İzin tipi
        LocalDate startDate,         // Başlangıç tarihi
        LocalDate endDate,           // Bitiş tarihi
        String employeeNote,         // Çalışan notu
        String status                // Talebin durumu (PENDING, APPROVED, REJECTED vb.)
) {}
