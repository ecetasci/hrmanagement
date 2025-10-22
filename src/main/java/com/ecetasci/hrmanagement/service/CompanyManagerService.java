package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.RegisterEmployeeRequestDto;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.User;
import com.ecetasci.hrmanagement.enums.Role;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import com.ecetasci.hrmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyManagerService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CompanyService companyService;
    private final UserService userService;

    // Personel ekleme
    public Employee createEmployee(RegisterEmployeeRequestDto dto) {
        // 1. Önce User oluştur
        User user = new User();
        user.setName(dto.name());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(Role.EMPLOYEE);
        user.setEmail(dto.email());
        //user.setActive(false);

        User savedUser = userRepository.save(user);

        // 2. Sonra Employee oluştur ve User'ı bağla
        Employee employee = new Employee();
        employee.setName(dto.name());
        employee.setPassword(savedUser.getPassword()); // aynı şifre
        employee.setRole(Role.EMPLOYEE);
        //employee.setActive(false);
        employee.setCompany(companyService.findById(dto.companyId()));
        employee.setEmployeeNumber(userService.generateEmployeeNumber());
        employee.setDepartment(dto.department());
        employee.setPosition(dto.position());
        employee.setEmail(dto.email());
        employee.setUser(savedUser); // ilişkilendirme

        return employeeRepository.save(employee);
    }


    // Listeleme (şirket bazlı izolasyon)
    public Page<Employee> findAllByCompanyId(Long companyId, Pageable pageable) {
        return employeeRepository.findAllByCompanyId(companyId, pageable);
    }


    // Güncelleme
    public Employee updateEmployee(Long id, RegisterEmployeeRequestDto dto) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        emp.setName(dto.name());

        if (dto.password() != null && !dto.password().isEmpty()) {
            String encoded = passwordEncoder.encode(dto.password());
            emp.setPassword(encoded);

            // User tarafını da güncelle
            User user = emp.getUser();
            user.setPassword(encoded);
            userRepository.save(user);
        }

        return employeeRepository.save(emp);
    }


    public void deleteEmployee(Long id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // önce User’ı da silmek istersen:
        if (emp.getUser() != null) {
            userRepository.delete(emp.getUser());
        }

        employeeRepository.delete(emp);
    }


    // Aktifleştirme/Pasifleştirme + email bildirimi
    public Employee setEmployeeActiveStatus(Long id, boolean active) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
       // emp.setActive(active);
        employeeRepository.save(emp);

        // Email bildirimi
        String status = active ? "aktifleştirildi" : "pasifleştirildi";
        emailService.send(emp.getEmail(),
                "Hesap Durumu Güncellendi",
                "Sayın " + emp.getName() + ", hesabınız " + status + ".");

        return emp;
    }


}

