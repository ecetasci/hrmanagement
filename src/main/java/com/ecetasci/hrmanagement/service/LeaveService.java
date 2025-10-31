package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.LeaveRequestDto;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.LeaveRequest;
import com.ecetasci.hrmanagement.enums.LeaveStatus;
import com.ecetasci.hrmanagement.mapper.LeaveMapper;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import com.ecetasci.hrmanagement.repository.LeaveTypeRepository;
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.ecetasci.hrmanagement.utility.HolidayUtil.calculateWorkingDays;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final EmployeeRepository employeeRepository;
    private final LeaveMapper leaveMapper;
    private final LeaveTypeRepository leaveTypeRepository;
    private final com.ecetasci.hrmanagement.repository.LeaveRequestRepository leaveRequestRepository;


    @Transactional
    public LeaveRequest leaveRequestCreate(LeaveRequestDto leaveRequestDto) {

        // Map DTO -> entity
        LeaveRequest entity = leaveMapper.toEntity(leaveRequestDto);

        String empNum = leaveRequestDto.employeeNumber() != null ? leaveRequestDto.employeeNumber().trim() : null;
        // Try exact lookup first (keeps existing tests working), then fallback to case-insensitive lookup
        Employee employee = employeeRepository.findByEmployeeNumber(empNum)
                .or(() -> employeeRepository.findEmployeeByEmployeeNumberIgnoreCase(empNum))
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + empNum));
        var leaveType = leaveTypeRepository.findById(leaveRequestDto.leaveTypeId())
                .orElseThrow(() -> new IllegalArgumentException("LeaveType not found: " + leaveRequestDto.leaveTypeId()));

        // DTO tarihlerinin varlığını ve sırasını doğrula
        LocalDate startDate = leaveRequestDto.startDate();
        LocalDate endDate = leaveRequestDto.endDate();
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate ve endDate boş olamaz");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate, startDate'den önce olamaz");
        }

        // Çakışma kontrolü (PENDING veya APPROVED ile)
        boolean hasOverlap = employee.getLeaveRequests().stream()
                .anyMatch(r ->
                        (r.getStatus() != LeaveStatus.REJECTED) &&
                                !(endDate.isBefore(r.getStartDate()) || startDate.isAfter(r.getEndDate()))
                );

        if (hasOverlap) {
            throw new IllegalStateException("Bu tarih aralığında zaten izin talebiniz var!");
        }

        int totalDays = calculateWorkingDays(startDate, endDate);

        if (employee.getLeaveBalance() < totalDays) {
            throw new IllegalStateException("Yetersiz izin bakiyesi!");
        }

        // Entity'ye eksik atamaları yap
        entity.setEmployee(employee);
        entity.setLeaveType(leaveType);
        entity.setStartDate(startDate);
        entity.setEndDate(endDate);
        entity.setTotalDays(totalDays);
        entity.setStatus(LeaveStatus.PENDING);

        return leaveRequestRepository.save(entity);
    }

    @Transactional
    public void approveLeaveRequest(String employeeNumber, LocalDate startDate, String managerEmployeeNumber) {
        String empNum = employeeNumber != null ? employeeNumber.trim() : null;
        Employee employee = employeeRepository.findByEmployeeNumber(empNum)
                .or(() -> employeeRepository.findEmployeeByEmployeeNumberIgnoreCase(empNum))
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + empNum));

        LeaveRequest request = employee.getLeaveRequests().stream()
                .filter(r -> startDate.equals(r.getStartDate()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found for startDate: " + startDate));

        if (request.getStatus() == LeaveStatus.APPROVED) {
            throw new IllegalStateException("Leave request already approved");
        }
        if (request.getStatus() == LeaveStatus.REJECTED) {
            throw new IllegalStateException("Leave request already rejected");
        }

        String mgrNum = managerEmployeeNumber != null ? managerEmployeeNumber.trim() : null;
        Employee manager = employeeRepository.findByEmployeeNumber(mgrNum)
                .or(() -> employeeRepository.findEmployeeByEmployeeNumberIgnoreCase(mgrNum))
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + mgrNum));

        Integer totalDays = request.getTotalDays();
        if (totalDays == null) {
            throw new IllegalStateException("Request totalDays is null");
        }
        if (employee.getLeaveBalance() < totalDays) {
            throw new IllegalStateException("Yetersiz bakiye!");
        }

        // Opsiyonel: manager'ın gerçekten aynı şirkette olup olmadığını employeeNumber stringleri üzerinden doğrula
        // Eğer gerekli ise şunu kullanabiliriz (isteğe bağlı):
        // if (manager.getCompany() == null || employee.getCompany() == null || !Objects.equals(manager.getCompany().getId(), employee.getCompany().getId())) {
        //     throw new IllegalStateException("Manager and employee are not in the same company");
        // }

        request.setStatus(LeaveStatus.APPROVED);
        request.setApprovedBy(manager);
        request.setApprovedAt(LocalDateTime.now());

        employee.setLeaveBalance(employee.getLeaveBalance() - totalDays);

        // Kaydet: hem request hem employee'yi açıkça kaydet
        leaveRequestRepository.save(request);
        employeeRepository.save(employee);
    }

    @Transactional
    public void rejectLeaveRequestByEmployeeNumber(String employeeNumber, String managerEmployeeNumber, String managerNote) {
        String empNum = employeeNumber != null ? employeeNumber.trim() : null;
        Employee employee = employeeRepository.findByEmployeeNumber(empNum)
                .or(() -> employeeRepository.findEmployeeByEmployeeNumberIgnoreCase(empNum))
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + empNum));

        String mgrNum = managerEmployeeNumber != null ? managerEmployeeNumber.trim() : null;
        Employee managerEmployee = employeeRepository.findByEmployeeNumber(mgrNum)
                .or(() -> employeeRepository.findEmployeeByEmployeeNumberIgnoreCase(mgrNum))
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + mgrNum));

        LeaveRequest request = employee.getLeaveRequests().stream()
                .filter(r -> r.getStatus() == LeaveStatus.PENDING) // öncelikle beklemedeki talebi hedefle
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Pending leave request not found for employee: " + employeeNumber));

        if (request.getStatus() == LeaveStatus.APPROVED) {
            throw new IllegalStateException("Leave request already approved");
        }
        if (request.getStatus() == LeaveStatus.REJECTED) {
            throw new IllegalStateException("Leave request already rejected");
        }

        request.setStatus(LeaveStatus.REJECTED);
        request.setApprovedBy(managerEmployee);
        request.setApprovedAt(LocalDateTime.now());
        request.setManagerNote(managerNote);

        leaveRequestRepository.save(request);
    }

}
