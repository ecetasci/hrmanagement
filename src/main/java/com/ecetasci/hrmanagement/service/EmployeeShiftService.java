package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.EmployeeShift;
import com.ecetasci.hrmanagement.entity.Shift;
import com.ecetasci.hrmanagement.exceptions.BusinessException;
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import com.ecetasci.hrmanagement.repository.EmployeeShiftRepository;
import com.ecetasci.hrmanagement.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeShiftService {

    private final EmployeeShiftRepository employeeShiftRepository;
    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;

    // 1. Çalışana vardiya atama
    public EmployeeShift assignShift(Long employeeId, Long shiftId, LocalDate assignedDate) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found"));

        // 1️⃣ Çalışan aynı gün başka bir vardiyada mı?
        boolean exists = employeeShiftRepository.existsByEmployeeIdAndAssignedDate(employeeId, assignedDate);
        if (exists) {
            throw new BusinessException("Çalışan bu tarihte zaten bir vardiyada!");
        }

        // 2️⃣ Şirket uyumu kontrolü
        if (!employee.getCompany().getId().equals(shift.getCompany().getId())) {
            throw new BusinessException("Çalışanın şirketi ile vardiya şirketi aynı değil!");
        }
//localTimela LocaldATEtİME FARK

        EmployeeShift employeeShift = EmployeeShift.builder()
                .employee(employee)
                .shift(shift)
                .assignedDate(assignedDate)
                .startTime(shift.getStartTime())
                .endTime(shift.getEndTime())
                .build();

        return employeeShiftRepository.save(employeeShift);
    }

    // 2. Çalışanın tüm vardiyaları
    public List<EmployeeShift> getShiftsByEmployee(Long employeeId) {
        return employeeShiftRepository.findByEmployeeId(employeeId);
    }

    // 3. Çalışanın vardiyasını sil
    public void removeShift(Long employeeShiftId) {
        if (!employeeShiftRepository.existsById(employeeShiftId)) {
            throw new ResourceNotFoundException("Employee shift not found");
        }
        employeeShiftRepository.deleteById(employeeShiftId);
    }
}

