package com.ecetasci.hrmanagement.repository;

import com.ecetasci.hrmanagement.entity.EmployeeShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface EmployeeShiftRepository extends JpaRepository<EmployeeShift, Long> {
    List<EmployeeShift> findByEmployeeIdAndAssignedDate(Long employeeId, LocalDate assignedDate);

    boolean existsByEmployeeIdAndAssignedDate(Long employeeId, LocalDate assignedDate);

    List<EmployeeShift> findByEmployeeId(Long employeeId);
    List<EmployeeShift> findByEmployee_IdAndStartTimeBetween(Long employeeId, LocalDateTime start, LocalDateTime end);

    @Modifying
    @Query("delete from EmployeeShift es where es.shift.id = :shiftId")
    void deleteByShiftId(@Param("shiftId") Long shiftId);
}