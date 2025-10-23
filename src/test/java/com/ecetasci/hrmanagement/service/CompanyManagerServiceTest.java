package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.RegisterEmployeeRequestDto;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.User;
import com.ecetasci.hrmanagement.enums.Role;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import com.ecetasci.hrmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyManagerServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private CompanyService companyService;
    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private CompanyManagerService service;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setCompanyName("ACME");
    }

    @Test
    void createEmployee_success() {
        RegisterEmployeeRequestDto dto = new RegisterEmployeeRequestDto(
                "John Doe", 1L, "rawPass", Role.EMPLOYEE, "john@example.com", "Developer", "IT"
        );

        when(passwordEncoder.encode("rawPass")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(companyService.findById(1L)).thenReturn(company);
        when(employeeService.generateEmployeeNumber()).thenReturn("E123456");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId(20L);
            return e;
        });

        Employee saved = service.createEmployee(dto);

        assertNotNull(saved.getId());
        assertEquals("John Doe", saved.getName());
        assertEquals("john@example.com", saved.getEmail());
        assertEquals(Role.EMPLOYEE, saved.getRole());
        assertEquals("E123456", saved.getEmployeeNumber());
        assertEquals(company, saved.getCompany());
        assertNotNull(saved.getUser());
        assertEquals("ENCODED", saved.getPassword());

        // User da encoded şifre ile kaydedilmiş olmalı
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("ENCODED", savedUser.getPassword());
        assertEquals(Role.EMPLOYEE, savedUser.getRole());
        assertEquals("john@example.com", savedUser.getEmail());
    }

    @Test
    void findAllByCompanyId_returnsPage() {
        Employee e1 = new Employee(); e1.setId(1L); e1.setName("A");
        Employee e2 = new Employee(); e2.setId(2L); e2.setName("B");
        Page<Employee> page = new PageImpl<>(List.of(e1, e2));
        Pageable pageable = Pageable.unpaged();
        when(employeeRepository.findAllByCompanyId(1L, pageable)).thenReturn(page);

        Page<Employee> result = service.findAllByCompanyId(1L, pageable);

        assertEquals(2, result.getContent().size());
        verify(employeeRepository).findAllByCompanyId(1L, pageable);
    }

    @Test
    void updateEmployee_notFound_throws() {
        RegisterEmployeeRequestDto dto = new RegisterEmployeeRequestDto(
                "NewName", 1L, "newPass", Role.EMPLOYEE, "n@example.com", "pos", "dep"
        );
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateEmployee(99L, dto));
        assertEquals("Employee not found", ex.getMessage());
    }

    @Test
    void updateEmployee_withoutPassword_updatesNameOnly() {
        // mevcut employee + user
        User user = new User(); user.setId(7L); user.setPassword("OLD");
        Employee emp = new Employee(); emp.setId(5L); emp.setName("Old"); emp.setPassword("OLD"); emp.setUser(user);

        RegisterEmployeeRequestDto dto = new RegisterEmployeeRequestDto(
                "NewName", 1L, "", Role.EMPLOYEE, "n@example.com", "pos", "dep"
        );

        when(employeeRepository.findById(5L)).thenReturn(Optional.of(emp));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee updated = service.updateEmployee(5L, dto);

        assertEquals("NewName", updated.getName());
        assertEquals("OLD", updated.getPassword()); // değişmemeli
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateEmployee_withPassword_updatesBothUserAndEmployee() {
        User user = new User(); user.setId(7L); user.setPassword("OLD");
        Employee emp = new Employee(); emp.setId(5L); emp.setName("Old"); emp.setPassword("OLD"); emp.setUser(user);

        RegisterEmployeeRequestDto dto = new RegisterEmployeeRequestDto(
                "NewName", 1L, "newRaw", Role.EMPLOYEE, "n@example.com", "pos", "dep"
        );

        when(employeeRepository.findById(5L)).thenReturn(Optional.of(emp));
        when(passwordEncoder.encode("newRaw")).thenReturn("ENC2");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee updated = service.updateEmployee(5L, dto);

        assertEquals("NewName", updated.getName());
        assertEquals("ENC2", updated.getPassword());
        assertEquals("ENC2", updated.getUser().getPassword());
        verify(userRepository, times(1)).save(updated.getUser());
    }

    @Test
    void deleteEmployee_notFound_throws() {
        when(employeeRepository.findById(42L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.deleteEmployee(42L));
        assertEquals("Employee not found", ex.getMessage());
    }

    @Test
    void deleteEmployee_withLinkedUser_deletesUserThenEmployee() {
        User user = new User(); user.setId(3L);
        Employee emp = new Employee(); emp.setId(2L); emp.setUser(user);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(emp));

        service.deleteEmployee(2L);

        verify(userRepository).delete(user);
        verify(employeeRepository).delete(emp);
    }

    @Test
    void deleteEmployee_withoutLinkedUser_deletesEmployeeOnly() {
        Employee emp = new Employee(); emp.setId(2L); emp.setUser(null);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(emp));

        service.deleteEmployee(2L);

        verify(userRepository, never()).delete(any());
        verify(employeeRepository).delete(emp);
    }

    @Test
    void setEmployeeActiveStatus_notFound_throws() {
        when(employeeRepository.findById(9L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.setEmployeeActiveStatus(9L, true));
        assertEquals("Employee not found", ex.getMessage());
    }

    @Test
    void setEmployeeActiveStatus_active_sendsEmail() {
        Employee emp = new Employee();
        emp.setId(1L);
        emp.setName("Jane");
        emp.setEmail("jane@example.com");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee result = service.setEmployeeActiveStatus(1L, true);

        assertEquals(emp, result);
        verify(emailService).send(eq("jane@example.com"), eq("Hesap Durumu Güncellendi"), contains("aktifleştirildi"));
        verify(employeeRepository).save(emp);
    }

    @Test
    void setEmployeeActiveStatus_inactive_sendsEmail() {
        Employee emp = new Employee();
        emp.setId(1L);
        emp.setName("Jane");
        emp.setEmail("jane@example.com");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setEmployeeActiveStatus(1L, false);

        verify(emailService).send(eq("jane@example.com"), eq("Hesap Durumu Güncellendi"), contains("pasifleştirildi"));
    }
}

