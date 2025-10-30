package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.EmployeeShift;
import com.ecetasci.hrmanagement.entity.Shift;
import com.ecetasci.hrmanagement.exceptions.BusinessException;
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import com.ecetasci.hrmanagement.repository.EmployeeShiftRepository;
import com.ecetasci.hrmanagement.repository.ShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeShiftServiceTest {

    @Mock private EmployeeShiftRepository employeeShiftRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private ShiftRepository shiftRepository;

    @InjectMocks
    private EmployeeShiftService service;

    private Company companyA;
    private Company companyB;
    private Employee employee;
    private Shift shift;

    @BeforeEach
    void setUp() {
        companyA = new Company();
        companyA.setId(1L);
        companyA.setCompanyName("A");

        companyB = new Company();
        companyB.setId(2L);
        companyB.setCompanyName("B");

        employee = new Employee();
        employee.setId(10L);
        employee.setName("John");
        employee.setCompany(companyA);

        shift = new Shift();
        shift.setId(100L);
        shift.setName("Sabah");
        shift.setCompany(companyA);
        shift.setStartTime(LocalDateTime.of(
                LocalDate.now(),      // bugünün tarihi
                LocalTime.of(9, 0)    // 09:00
        ));

        shift.setEndTime(LocalDateTime.of(
                LocalDate.now(),      // bugünün tarihi
                LocalTime.of(17, 30)  // 17:30
        ));

    }

    // assignShift
    @Test
    void assignShift_whenEmployeeNotFound_throwsResourceNotFound() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.assignShift(10L, 100L, LocalDate.now()));
        assertEquals("Employee not found", ex.getMessage());
    }

    @Test
    void assignShift_whenShiftNotFound_throwsResourceNotFound() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(shiftRepository.findById(100L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.assignShift(10L, 100L, LocalDate.now()));
        assertEquals("Shift not found", ex.getMessage());
    }

    @Test
    void assignShift_whenAlreadyAssignedOnDate_throwsBusinessException() {
        LocalDate day = LocalDate.of(2025, 5, 1);
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(shiftRepository.findById(100L)).thenReturn(Optional.of(shift));
        when(employeeShiftRepository.existsByEmployeeIdAndAssignedDate(10L, day)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assignShift(10L, 100L, day));
        assertEquals("Çalışan bu tarihte zaten bir vardiyada!", ex.getMessage());
    }

    @Test
    void assignShift_whenCompanyMismatch_throwsBusinessException() {
        LocalDate day = LocalDate.of(2025, 5, 1);
        Shift otherCompanyShift = new Shift();
        otherCompanyShift.setId(200L);
        otherCompanyShift.setName("Gece");
        otherCompanyShift.setCompany(companyB);
        otherCompanyShift.setStartTime(LocalDateTime.of(
                LocalDate.now(), // bugünün tarihi
                LocalTime.of(22, 0) // 22:00
        ));

        otherCompanyShift.setEndTime(LocalDateTime.of(
                LocalDate.now().plusDays(1), // gece vardiyasıysa ertesi gün
                LocalTime.of(6, 0) // 06:00
        ));


        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(shiftRepository.findById(200L)).thenReturn(Optional.of(otherCompanyShift));
        when(employeeShiftRepository.existsByEmployeeIdAndAssignedDate(10L, day)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assignShift(10L, 200L, day));
        assertEquals("Çalışanın şirketi ile vardiya şirketi aynı değil!", ex.getMessage());
    }

    @Test
    void assignShift_success_persistsAndReturnsSavedShift() {
        LocalDate day = LocalDate.of(2025, 5, 10);
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(shiftRepository.findById(100L)).thenReturn(Optional.of(shift));
        when(employeeShiftRepository.existsByEmployeeIdAndAssignedDate(10L, day)).thenReturn(false);
        when(employeeShiftRepository.save(any(EmployeeShift.class))).thenAnswer(inv -> {
            EmployeeShift es = inv.getArgument(0);
            es.setId(999L);
            return es;
        });

        EmployeeShift saved = service.assignShift(10L, 100L, day);

        assertEquals(999L, saved.getId());
        assertEquals(employee, saved.getEmployee());
        assertEquals(shift, saved.getShift());
        assertEquals(day, saved.getAssignedDate());
        assertEquals(shift.getStartTime(), saved.getStartTime());
        assertEquals(shift.getEndTime(), saved.getEndTime());

        ArgumentCaptor<EmployeeShift> captor = ArgumentCaptor.forClass(EmployeeShift.class);
        verify(employeeShiftRepository).save(captor.capture());
        EmployeeShift toSave = captor.getValue();
        assertEquals(employee, toSave.getEmployee());
        assertEquals(shift, toSave.getShift());
        assertEquals(day, toSave.getAssignedDate());
    }

    // getShiftsByEmployee
    @Test
    void getShiftsByEmployee_returnsListFromRepository() {
        EmployeeShift es1 = new EmployeeShift(); es1.setId(1L);
        EmployeeShift es2 = new EmployeeShift(); es2.setId(2L);
        when(employeeShiftRepository.findByEmployeeId(10L)).thenReturn(List.of(es1, es2));

        List<EmployeeShift> list = service.getShiftsByEmployee(10L);

        assertEquals(2, list.size());
        verify(employeeShiftRepository).findByEmployeeId(10L);
    }

    // removeShift
    @Test
    void removeShift_whenNotExists_throwsResourceNotFound() {
        when(employeeShiftRepository.existsById(5L)).thenReturn(false);
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.removeShift(5L));
        assertEquals("Employee shift not found", ex.getMessage());
        verify(employeeShiftRepository, never()).deleteById(anyLong());
    }

    @Test
    void removeShift_whenExists_deletesById() {
        when(employeeShiftRepository.existsById(5L)).thenReturn(true);
        service.removeShift(5L);
        verify(employeeShiftRepository).deleteById(5L);
    }
}

