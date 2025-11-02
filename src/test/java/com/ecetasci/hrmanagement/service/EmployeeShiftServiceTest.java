package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.EmployeeShift;
import com.ecetasci.hrmanagement.entity.Shift;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import com.ecetasci.hrmanagement.repository.EmployeeShiftRepository;
import com.ecetasci.hrmanagement.repository.ShiftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeShiftServiceTest {

    @Mock
    private EmployeeShiftRepository employeeShiftRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @InjectMocks
    private EmployeeShiftService service;

    @Test
    void assignShift_success() {
        Long empId = 1L;
        Long shiftId = 2L;
        LocalDate date = LocalDate.now();

        Company company = new Company(); company.setId(9L);

        Employee emp = new Employee();
        emp.setId(empId);
        emp.setCompany(company);

        Shift shift = new Shift();
        shift.setId(shiftId);
        shift.setCompany(company);

        when(employeeRepository.findById(empId)).thenReturn(Optional.of(emp));
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        when(employeeShiftRepository.existsByEmployeeIdAndAssignedDate(empId, date)).thenReturn(false);
        EmployeeShift saved = new EmployeeShift();
        saved.setId(5L);
        when(employeeShiftRepository.save(any(EmployeeShift.class))).thenReturn(saved);

        EmployeeShift result = service.assignShift(empId, shiftId, date);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        verify(employeeShiftRepository).save(any(EmployeeShift.class));
    }

    @Test
    void assignShift_employeeNotFound_throws() {
        when(employeeRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.assignShift(1L, 2L, LocalDate.now()));
    }

    @Test
    void removeShift_notFound_throws() {
        when(employeeShiftRepository.existsById(10L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.removeShift(10L));
    }
}
