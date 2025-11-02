package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.RegisterEmployeeRequestDto;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.User;
import com.ecetasci.hrmanagement.enums.Role;
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import com.ecetasci.hrmanagement.repository.UserRepository;
import com.ecetasci.hrmanagement.utility.JwtManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Mock
    private JwtManager jwtManager;

    @InjectMocks
    private CompanyManagerService companyManagerService;

    @Captor
    private ArgumentCaptor<Employee> employeeCaptor;

    private AutoCloseable mocksCloseable;

    @BeforeEach
    void setUp() {
        mocksCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocksCloseable != null) {
            mocksCloseable.close();
        }
    }

    @Test
    void createEmployee_savesEmployeeAndSendsEmail() {
        // Arrange
        RegisterEmployeeRequestDto dto = new RegisterEmployeeRequestDto(
                "John Doe",
                1L,
                "password123",
                Role.EMPLOYEE,
                "john@example.com",
                "Developer",
                "IT",
                LocalDate.of(1990,1,1),
                LocalDate.of(2020,1,1),
                BigDecimal.valueOf(5000),
                "555-1234",
                "Somewhere",
                "Jane:555-0000"
        );

        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(jwtManager.generateToken(anyString())).thenReturn("token123");

        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setEmail(dto.email());
        savedUser.setPassword("encodedPass");
        savedUser.setEmailVerificationToken("token123");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Company company = new Company();
        company.setId(1L);
        when(companyService.findById(1L)).thenReturn(company);
        when(employeeService.generateEmployeeNumber()).thenReturn("EMP-001");

        Employee savedEmployee = new Employee();
        savedEmployee.setId(100L);
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);

        // Act
        Employee result = companyManagerService.createEmployee(dto);

        // Assert
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
        verify(employeeRepository).save(employeeCaptor.capture());
        Employee captured = employeeCaptor.getValue();
        assertEquals("John Doe", captured.getName());
        assertEquals("encodedPass", captured.getPassword());
        assertEquals(dto.email(), captured.getEmail());
        verify(emailService).sendVerificationEmail(eq(dto.email()), eq("token123"));
    }

    @Test
    void updateEmployee_whenNotFound_throwsResourceNotFound() {
        // Arrange
        when(employeeRepository.findById(5L)).thenReturn(Optional.empty());
        RegisterEmployeeRequestDto dto = new RegisterEmployeeRequestDto(
                "Name",
                1L,
                "newpass",
                Role.EMPLOYEE,
                "a@b.com",
                "Pos",
                "Dept",
                LocalDate.now(),
                LocalDate.now(),
                BigDecimal.ZERO,
                "",
                "",
                ""
        );

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> companyManagerService.updateEmployee(5L, dto));
    }

    @Test
    void findAllByCompanyId_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Employee e1 = new Employee();
        Employee e2 = new Employee();
        Page<Employee> page = new PageImpl<>(java.util.List.of(e1, e2));
        when(employeeRepository.findAllByCompanyId(2L, pageable)).thenReturn(page);

        Page<Employee> result = companyManagerService.findAllByCompanyId(2L, pageable);
        assertEquals(2, result.getContent().size());
        verify(employeeRepository).findAllByCompanyId(2L, pageable);
    }

}
