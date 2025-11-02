package com.ecetasci.hrmanagement.repository;

import com.ecetasci.hrmanagement.entity.EmployeeShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface EmployeeShiftRepository extends JpaRepository<EmployeeShift, Long> {
    List<EmployeeShift> findByEmployeeIdAndAssignedDate(Long employeeId, LocalDate assignedDate);

    boolean existsByEmployeeIdAndAssignedDate(Long employeeId, LocalDate assignedDate);

    List<EmployeeShift> findByEmployeeId(Long employeeId);


    List<EmployeeShift> findAllByEmployee_IdAndStartTimeBetween(Long employeeId, LocalTime start, LocalTime end);

    // Added: fetch employee shifts where assignedDate between start and end (inclusive)
    List<EmployeeShift> findByEmployee_IdAndAssignedDateBetween(Long employeeId, LocalDate start, LocalDate end);

    @Modifying
    @Query("delete from EmployeeShift es where es.shift.id = :shiftId")
    void deleteByShiftId(@Param("shiftId") Long shiftId);
}