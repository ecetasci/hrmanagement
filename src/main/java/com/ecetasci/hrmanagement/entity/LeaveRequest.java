package com.ecetasci.hrmanagement.entity;

import com.ecetasci.hrmanagement.enums.LeaveStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;  // İzin talebini yapan çalışan

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType; // İzin tipi (örn. Yıllık İzin, Hastalık)

    @Column(nullable = false)
    private LocalDate startDate; // İzin başlangıcı

    @Column(nullable = false)
    private LocalDate endDate;   // İzin bitişi

    @Column(nullable = false) //holidayutilden çağrılıp hesaplanacak
    private Integer totalDays;   // Tatil günleri hariç hesaplanan gün sayısı

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LeaveStatus status = LeaveStatus.PENDING; // Varsayılan: PENDING

    @Column(length = 500)
    private String employeeNote; // Çalışanın açıklaması

    @Column(length = 500)
    private String managerNote;  // Yöneticinin eklediği not

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Employee approvedBy; // Onaylayan yöneticiyi tutar

    private LocalDateTime approvedAt; // Onay tarihi
}
