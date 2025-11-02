package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.EmployeeRequestDto;
import com.ecetasci.hrmanagement.dto.response.EmployeeResponseDto;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.enums.UserStatus;
import com.ecetasci.hrmanagement.repository.CompanyRepository;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import com.ecetasci.hrmanagement.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository; // şirket izolasyonu için
    private final EmailService emailService; // email bildirimleri için
    private final PasswordEncoder passwordEncoder;
    private final ExpenseRepository expenseRepository;

    public Page<EmployeeResponseDto> getAllEmployees(Long companyId, Pageable pageable) {
        return employeeRepository.findAllByCompanyId(companyId, pageable)
                .map(emp -> new EmployeeResponseDto(
                        emp.getId(),
                        emp.getEmployeeNumber(),
                        emp.getName(),

                        emp.getEmail(),
                        emp.getPosition(),
                        emp.getDepartment()
                ));
    }

//Managerdan bağımsız employee oluşturma seçeneği olması için yazıldı,
    public EmployeeResponseDto createEmployee(Long companyId, EmployeeRequestDto dto) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        // Otomatik personel numarası üretme
        String employeeNumber = UUID.randomUUID().toString().substring(0,8);

        Employee employee = Employee.builder()
                .name(dto.name())

                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .birthDate(dto.birthDate())
                .hireDate(dto.hireDate())
                .position(dto.position())
                .department(dto.department())
                .salary(dto.salary())
                .phoneNumber(dto.phoneNumber())
                .address(dto.address())
                .emergencyContact(dto.emergencyContact())
                .company(company)
                .employeeNumber(employeeNumber)
                .build();

        Employee saved = employeeRepository.save(employee);

        // Email bildirimi
        emailService.send(saved.getEmail(), "Welcome " + saved.getName(), "Hoşgeldiniz");

        return new EmployeeResponseDto(
                saved.getId(), saved.getEmployeeNumber(),
                saved.getName(),
                saved.getEmail(), saved.getPosition(),
                saved.getDepartment()
        );
    }

    public String generateEmployeeNumber() {
        // Prefix: tek büyük harf, ardından 6 haneli sıfır dolgulu sayı => A123456
        for (int attempt = 0; attempt < 10; attempt++) {
            char prefix = (char) ('A' + ThreadLocalRandom.current().nextInt(26));
            int number = ThreadLocalRandom.current().nextInt(0, 1_000_000);
            String candidate = String.format("%c%06d", prefix, number);

            // repository'de var mı kontrol et (findByEmployeeNumber var varsayımıyla)
            if (employeeRepository.findEmployeeByEmployeeNumber(candidate).isEmpty()) {
                return candidate;
            }
        }

        // Nadiren çakışma olursa güvenli fallback
        return "EMP" + System.currentTimeMillis();
    }

    public EmployeeResponseDto updateEmployee(Long id, EmployeeRequestDto dto) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        emp.setName(dto.name());

        emp.setEmail(dto.email());
        emp.setPosition(dto.position());
        emp.setDepartment(dto.department());
        emp.setSalary(dto.salary());
        emp.setPhoneNumber(dto.phoneNumber());
        emp.setAddress(dto.address());
        emp.setEmergencyContact(dto.emergencyContact());

        Employee updated = employeeRepository.save(emp);
        return new EmployeeResponseDto(
                updated.getId(), updated.getEmployeeNumber(),
                updated.getName(),
                updated.getEmail(), updated.getPosition(),
                updated.getDepartment()
        );
    }

    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee not found");
        }
        employeeRepository.deleteById(id);
    }

    public void activateEmployee(Long id, boolean activate) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        emp.getUser().setUserStatus(UserStatus.ACTIVE);
        employeeRepository.save(emp);

        String status = activate ? "activated" : "deactivated";
        emailService.send(emp.getEmail(),"aktivasyon" ,"Your account has been " + status);
    }


    public Employee save(Employee employee) {
        return employeeRepository.save(employee);
    }
}
