package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.LeaveRequestDto;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.LeaveRequest;
import com.ecetasci.hrmanagement.entity.LeaveType;
import com.ecetasci.hrmanagement.enums.LeaveStatus;
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;
import com.ecetasci.hrmanagement.mapper.LeaveMapper;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import com.ecetasci.hrmanagement.repository.LeaveRequestRepository;
import com.ecetasci.hrmanagement.repository.LeaveTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private LeaveMapper leaveMapper;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;

    @InjectMocks
    private LeaveService service;

    private Employee employee;
    private LeaveType leaveType;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setEmployeeNumber("E001");
        employee.setLeaveBalance(10);
        employee.setLeaveRequests(new ArrayList<>());

        leaveType = LeaveType.builder().name("Annual").maxDays(20).isPaid(true).build();
    }

    // ---- leaveRequestCreate ----
    @Test
    void leaveRequestCreate_success_setsFieldsAndSaves() {
        // Choose Mon-Fri week without official holidays: 2025-02-03 to 2025-02-07 -> 5 working days
        LocalDate start = LocalDate.of(2025, 2, 3);
        LocalDate end = LocalDate.of(2025, 2, 7);
        LeaveRequestDto dto = new LeaveRequestDto("E001", 99L, start, end, "note");

        when(leaveMapper.toEntity(dto)).thenReturn(new LeaveRequest());
        when(employeeRepository.findByEmployeeNumber("E001")).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(99L)).thenReturn(Optional.of(leaveType));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
            LeaveRequest lr = inv.getArgument(0);
            lr.setId(123L);
            return lr;
        });

        LeaveRequest saved = service.leaveRequestCreate(dto);

        assertNotNull(saved);
        assertEquals(LeaveStatus.PENDING, saved.getStatus());
        assertEquals(5, saved.getTotalDays());
        assertEquals(start, saved.getStartDate());
        assertEquals(end, saved.getEndDate());
        assertEquals(employee, saved.getEmployee());
        assertEquals(leaveType, saved.getLeaveType());

        // capture to ensure repository received correctly populated entity
        ArgumentCaptor<LeaveRequest> captor = ArgumentCaptor.forClass(LeaveRequest.class);
        verify(leaveRequestRepository).save(captor.capture());
        LeaveRequest toSave = captor.getValue();
        assertEquals(5, toSave.getTotalDays());
        assertEquals(LeaveStatus.PENDING, toSave.getStatus());
    }

    @Test
    void leaveRequestCreate_employeeNotFound_throwsNoSuchElement() {
        LocalDate start = LocalDate.of(2025, 2, 3);
        LocalDate end = LocalDate.of(2025, 2, 7);
        LeaveRequestDto dto = new LeaveRequestDto("E404", 1L, start, end, null);

        when(leaveMapper.toEntity(dto)).thenReturn(new LeaveRequest());
        when(employeeRepository.findByEmployeeNumber("E404")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.leaveRequestCreate(dto));
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void leaveRequestCreate_leaveTypeNotFound_throwsIllegalArgument() {
        LocalDate start = LocalDate.of(2025, 2, 3);
        LocalDate end = LocalDate.of(2025, 2, 7);
        LeaveRequestDto dto = new LeaveRequestDto("E001", 999L, start, end, null);

        when(leaveMapper.toEntity(dto)).thenReturn(new LeaveRequest());
        when(employeeRepository.findByEmployeeNumber("E001")).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.leaveRequestCreate(dto));
        assertTrue(ex.getMessage().contains("LeaveType not found: 999"));
    }

    @Test
    void leaveRequestCreate_nullDates_throwsIllegalArgument() {
        LeaveRequestDto dto = new LeaveRequestDto("E001", 1L, null, null, null);
        when(leaveMapper.toEntity(dto)).thenReturn(new LeaveRequest());
        when(employeeRepository.findByEmployeeNumber("E001")).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.leaveRequestCreate(dto));
        assertEquals("startDate ve endDate boş olamaz", ex.getMessage());
    }

    @Test
    void leaveRequestCreate_endBeforeStart_throwsIllegalArgument() {
        LocalDate start = LocalDate.of(2025, 2, 7);
        LocalDate end = LocalDate.of(2025, 2, 3);
        LeaveRequestDto dto = new LeaveRequestDto("E001", 1L, start, end, null);
        when(leaveMapper.toEntity(dto)).thenReturn(new LeaveRequest());
        when(employeeRepository.findByEmployeeNumber("E001")).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.leaveRequestCreate(dto));
        assertEquals("endDate, startDate'den önce olamaz", ex.getMessage());
    }

    @Test
    void leaveRequestCreate_overlap_throwsRuntime() {
        // Existing PENDING request from 2025-02-05 to 2025-02-06
        LeaveRequest existing = new LeaveRequest();
        existing.setStartDate(LocalDate.of(2025, 2, 5));
        existing.setEndDate(LocalDate.of(2025, 2, 6));
        existing.setStatus(LeaveStatus.PENDING);
        employee.getLeaveRequests().add(existing);

        LocalDate start = LocalDate.of(2025, 2, 4);
        LocalDate end = LocalDate.of(2025, 2, 7);
        LeaveRequestDto dto = new LeaveRequestDto("E001", 1L, start, end, null);

        when(leaveMapper.toEntity(dto)).thenReturn(new LeaveRequest());
        when(employeeRepository.findByEmployeeNumber("E001")).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.leaveRequestCreate(dto));
        assertEquals("Bu tarih aralığında zaten izin talebiniz var!", ex.getMessage());
    }

    @Test
    void leaveRequestCreate_insufficientBalance_throwsRuntime() {
        // Make working days = 5, but balance = 3
        employee.setLeaveBalance(3);
        LocalDate start = LocalDate.of(2025, 2, 3);
        LocalDate end = LocalDate.of(2025, 2, 7);
        LeaveRequestDto dto = new LeaveRequestDto("E001", 1L, start, end, null);

        when(leaveMapper.toEntity(dto)).thenReturn(new LeaveRequest());
        when(employeeRepository.findByEmployeeNumber("E001")).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.leaveRequestCreate(dto));
        assertEquals("Yetersiz izin bakiyesi!", ex.getMessage());
    }

    // ---- approveLeaveRequest ----
    @Test
    void approveLeaveRequest_employeeNotFound_throws() {
        when(employeeRepository.findByEmployeeNumber("E404")).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.approveLeaveRequest("E404", LocalDate.now(), "M001"));
        assertTrue(ex.getMessage().startsWith("Employee not found:"));
    }

    @Test
    void approveLeaveRequest_requestNotFound_throws() {
        employee.setLeaveRequests(List.of());
        when(employeeRepository.findByEmployeeNumber("E001")).thenReturn(Optional.of(employee));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.approveLeaveRequest("E001", LocalDate.of(2025,1,1), "M001"));
        assertTrue(ex.getMessage().startsWith("Leave request not found for startDate:"));
    }

    @Test
    void approveLeaveRequest_alreadyApproved_throws() {
        LeaveRequest req = new LeaveRequest();
        req.setStartDate(LocalDate.of(2025,1,1));
        req.setStatus(LeaveStatus.APPROVED);
        employee.setLeaveRequests(List.of(req));
        when(employeeRepository.findByEmployeeNumber("E001")).thenReturn(Optional.of(employee));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.approveLeaveRequest("E001", req.getStartDate(), "M001"));
        assertEquals("Leave request already approved", ex.getMessage());
    }

    @Test
    void approveLeaveRequest_alreadyRejected_throws() {
        LeaveRequest req = new LeaveRequest();
        req.setStartDate(LocalDate.of(2025,1,2));
        req.setStatus(LeaveStatus.REJECTED);
        employee.setLeaveRequests(List.of(req));
        when(employeeRepository.findByEmployeeNumber("E001")).thenReturn(Optional.of(employee));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.approveLeaveRequest("E001", req.getStartDate(), "M001"));
        assertEquals("Leave request already rejected", ex.getMessage());
    }

    @Test
    void approveLeaveRequest_managerNotFound_throws() {
        LeaveRequest req = new LeaveRequest();
        req.setStartDate(LocalDate.of(2025,1,3));
        req.setStatus(LeaveStatus.PENDING);
        req.setTotalDays(2);
        employee.setLeaveRequests(List.of(req));
        when(employeeRepository.findByEmployeeNumber("E001")).thenReturn(Optional.of(employee));
        when(employeeRepository.findByEmployeeNumber("M001")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.approveLeaveRequest("E001", req.getStartDate(), "M001"));
        assertTrue(ex.getMessage().startsWith("Manager not found:"));
    }

    @Test
    void approveLeaveRequest_totalDaysNull_throws() {
        LeaveRequest req = new LeaveRequest();
        req.setStartDate(LocalDate.of(2025,1,3));
        req.setStatus(LeaveStatus.PENDING);
        req.setTotalDays(null);
        employee.setLeaveRequests(List.of(req));
        when(employeeRepository.findByEmployeeNumber("E001")).thenReturn(Optional.of(employee));
        when(employeeRepository.findByEmployeeNumber("M001")).thenReturn(Optional.of(new Employee()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.approveLeaveRequest("E001", req.getStartDate(), "M001"));
        assertEquals("Request totalDays is null", ex.getMessage());
    }

    @Test
    void approveLeaveRequest_insufficientBalance_throws() {
        LeaveRequest req = new LeaveRequest();
        req.setStartDate(LocalDate.of(2025,1,3));
        req.setStatus(LeaveStatus.PENDING);
        req.setTotalDays(20);
        employee.setLeaveBalance(5);
        employee.setLeaveRequests(List.of(req));
        when(employeeRepository.findByEmployeeNumber("E001")).thenReturn(Optional.of(employee));
        when(employeeRepository.findByEmployeeNumber("M001")).thenReturn(Optional.of(new Employee()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.approveLeaveRequest("E001", req.getStartDate(), "M001"));
        assertEquals("Yetersiz bakiye!", ex.getMessage());
    }

    @Test
    void approveLeaveRequest_success_updatesStatusSavesAndDecrementsBalance() {
        LeaveRequest req = new LeaveRequest();
        req.setStartDate(LocalDate.of(2025,1,3));
        req.setStatus(LeaveStatus.PENDING);
        req.setTotalDays(3);
        employee.setLeaveBalance(7);
        employee.setLeaveRequests(List.of(req));

        Employee manager = new Employee();
        manager.setEmployeeNumber("M001");

        when(employeeRepository.findByEmployeeNumber("E001")).thenReturn(Optional.of(employee));
        when(employeeRepository.findByEmployeeNumber("M001")).thenReturn(Optional.of(manager));

        service.approveLeaveRequest("E001", req.getStartDate(), "M001");

        assertEquals(LeaveStatus.APPROVED, req.getStatus());
        assertEquals(manager, req.getApprovedBy());
        assertNotNull(req.getApprovedAt());
        assertEquals(4, employee.getLeaveBalance());
        verify(leaveRequestRepository).save(req);
        verify(employeeRepository).save(employee);
    }

    // ---- rejectLeaveRequestByEmployeeNumber ----
    @Test
    void rejectLeaveRequest_employeeNotFound_throws() {
        when(employeeRepository.findByEmployeeNumber("E404")).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.rejectLeaveRequestByEmployeeNumber("E404", "M001", "n"));
        assertTrue(ex.getMessage().startsWith("Employee not found:"));
    }

    @Test
    void rejectLeaveRequest_managerNotFound_throws() {
        when(employeeRepository.findByEmployeeNumber("E001")).thenReturn(Optional.of(employee));
        when(employeeRepository.findByEmployeeNumber("M404")).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.rejectLeaveRequestByEmployeeNumber("E001", "M404", "n"));
        assertTrue(ex.getMessage().startsWith("Manager not found:"));
    }

    @Test
    void rejectLeaveRequest_noPendingRequest_throws() {
        // No PENDING in list
        LeaveRequest r = new LeaveRequest(); r.setStatus(LeaveStatus.APPROVED);
        employee.setLeaveRequests(List.of(r));
        when(employeeRepository.findByEmployeeNumber("E001")).thenReturn(Optional.of(employee));
        when(employeeRepository.findByEmployeeNumber("M001")).thenReturn(Optional.of(new Employee()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.rejectLeaveRequestByEmployeeNumber("E001", "M001", "note"));
        assertTrue(ex.getMessage().startsWith("Pending leave request not found for employee:"));
    }

    @Test
    void rejectLeaveRequest_success_setsRejectedAndSaves() {
        LeaveRequest pending = new LeaveRequest();
        pending.setStatus(LeaveStatus.PENDING);
        employee.setLeaveRequests(List.of(pending));
        when(employeeRepository.findByEmployeeNumber("E001")).thenReturn(Optional.of(employee));
        Employee manager = new Employee(); manager.setEmployeeNumber("M001");
        when(employeeRepository.findByEmployeeNumber("M001")).thenReturn(Optional.of(manager));

        service.rejectLeaveRequestByEmployeeNumber("E001", "M001", "Too many requests");

        assertEquals(LeaveStatus.REJECTED, pending.getStatus());
        assertEquals(manager, pending.getApprovedBy());
        assertNotNull(pending.getApprovedAt());
        assertEquals("Too many requests", pending.getManagerNote());
        verify(leaveRequestRepository).save(pending);
    }
}

