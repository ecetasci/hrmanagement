package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.EmployeeRequestDto;
import com.ecetasci.hrmanagement.dto.response.EmployeeResponseDto;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.User;
import com.ecetasci.hrmanagement.enums.UserStatus;
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;
import com.ecetasci.hrmanagement.repository.CompanyRepository;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import com.ecetasci.hrmanagement.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ExpenseRepository expenseRepository;

    @InjectMocks
    private EmployeeService service;

    private Company company;

    @BeforeEach
    void setup() {
        company = new Company();
        company.setId(1L);
        company.setCompanyName("ACME");
    }

    @Test
    void getAllEmployees_returnsMappedPage() {
        Employee e1 = Employee.builder()
                .id(10L)
                .employeeNumber("E001")
                .name("John")
                .email("j@x.com")
                .position("Dev")
                .department("IT")
                .build();
        Employee e2 = Employee.builder()
                .id(11L)
                .employeeNumber("E002")
                .name("Jane")
                .email("j2@x.com")
                .position("QA")
                .department("QA")
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        when(employeeRepository.findAllByCompanyId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(e1, e2), pageable, 2));

        Page<EmployeeResponseDto> page = service.getAllEmployees(1L, pageable);

        assertEquals(2, page.getTotalElements());
        assertEquals("E001", page.getContent().get(0).employeeNumber());
        assertEquals("Jane", page.getContent().get(1).name());
        verify(employeeRepository).findAllByCompanyId(1L, pageable);
    }

    @Test
    void createEmployee_companyNotFound_throws() {
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());
        EmployeeRequestDto dto = defaultRequest();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createEmployee(1L, dto));
        assertEquals("Company not found", ex.getMessage());
        verify(employeeRepository, never()).save(any());
        verify(emailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void createEmployee_success_savesAndSendsEmail_andReturnsDto() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(passwordEncoder.encode("rawpass")).thenReturn("ENCODED");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee emp = inv.getArgument(0);
            emp.setId(100L);
            return emp;
        });
        EmployeeRequestDto dto = defaultRequest();

        EmployeeResponseDto res = service.createEmployee(1L, dto);

        assertEquals(100L, res.id());
        assertNotNull(res.employeeNumber());
        assertEquals(8, res.employeeNumber().length()); // UUID substring(0,8)
        assertEquals(dto.name(), res.name());
        assertEquals(dto.email(), res.email());
        assertEquals(dto.position(), res.position());
        assertEquals(dto.department(), res.department());

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        Employee saved = captor.getValue();
        assertEquals(company, saved.getCompany());
        assertEquals("ENCODED", saved.getPassword());

        verify(emailService).send(eq(dto.email()), startsWith("Welcome "+dto.name()), eq("Hoşgeldiniz"));
    }

    @Test
    void updateEmployee_notFound_throws() {
        when(employeeRepository.findById(5L)).thenReturn(Optional.empty());
        EmployeeRequestDto dto = defaultRequest();

        RuntimeException ex = assertThrows(ResourceNotFoundException.class, () -> service.updateEmployee(5L, dto));
        assertEquals("Employee not found", ex.getMessage());
    }

    @Test
    void updateEmployee_success_updatesFields_andReturnsDto() {
        Employee existing = new Employee();
        existing.setId(7L);
        existing.setEmployeeNumber("E777");
        existing.setPassword("OLD");
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeRequestDto dto = new EmployeeRequestDto(
                "New Name", "new@example.com", "newpass",
                LocalDate.of(1990,1,1), LocalDate.of(2020,1,1),
                "Lead", "R&D", BigDecimal.valueOf(9999),
                "+905001112233", "Addr", "EC"
        );

        EmployeeResponseDto res = service.updateEmployee(7L, dto);

        assertEquals(7L, res.id());
        assertEquals("E777", res.employeeNumber());
        assertEquals("New Name", res.name());
        assertEquals("new@example.com", res.email());
        assertEquals("Lead", res.position());
        assertEquals("R&D", res.department());

        // password değişmemeli
        assertEquals("OLD", existing.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
        verify(employeeRepository).save(existing);
    }

    @Test
    void deleteEmployee_callsRepository() {
        // Service throws ResourceNotFoundException when employee does not exist (existsById returns false on mock)
        RuntimeException ex = assertThrows(ResourceNotFoundException.class, () -> service.deleteEmployee(9L));
        assertEquals("Employee not found", ex.getMessage());
    }

    @Test
    void activateEmployee_notFound_throws() {
        when(employeeRepository.findById(3L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(ResourceNotFoundException.class, () -> service.activateEmployee(3L, true));
        assertEquals("Employee not found", ex.getMessage());
    }

    @Test
    void activateEmployee_true_setsStatusActive_andSendsActivatedEmail() {
        Employee emp = new Employee();
        emp.setId(1L);
        emp.setEmail("e@x.com");
        User user = new User();
        user.setUserStatus(UserStatus.PENDING_ADMIN_APPROVAL);
        emp.setUser(user);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        service.activateEmployee(1L, true);

        assertEquals(UserStatus.ACTIVE, emp.getUser().getUserStatus());
        verify(employeeRepository).save(emp);
        verify(emailService).send(eq("e@x.com"), eq("aktivasyon"), contains("activated"));
    }

    @Test
    void activateEmployee_false_setsStatusActive_andSendsDeactivatedEmail() {
        Employee emp = new Employee();
        emp.setId(2L);
        emp.setEmail("e2@x.com");
        User user = new User();
        user.setUserStatus(UserStatus.PENDING_ADMIN_APPROVAL);
        emp.setUser(user);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(emp));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        service.activateEmployee(2L, false);

        assertEquals(UserStatus.ACTIVE, emp.getUser().getUserStatus());
        verify(employeeRepository).save(emp);
        verify(emailService).send(eq("e2@x.com"), eq("aktivasyon"), contains("deactivated"));
    }

    @Test
    void save_delegatesToRepository() {
        Employee emp = new Employee();
        when(employeeRepository.save(emp)).thenReturn(emp);
        Employee result = service.save(emp);
        assertEquals(emp, result);
        verify(employeeRepository).save(emp);
    }

    private EmployeeRequestDto defaultRequest() {
        return new EmployeeRequestDto(
                "John Doe", "john@example.com", "rawpass",
                LocalDate.of(1995,5,5), LocalDate.of(2020,1,1),
                "Developer", "IT", BigDecimal.valueOf(5000),
                "+905001234567", "Address", "ICE"
        );
    }
}
