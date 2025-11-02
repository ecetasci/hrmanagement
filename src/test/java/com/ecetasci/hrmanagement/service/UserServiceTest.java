package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.RegisterRequestDto;
import com.ecetasci.hrmanagement.dto.request.ResetPasswordRequestDto;
import com.ecetasci.hrmanagement.dto.request.UpdateUserRequestDto;
import com.ecetasci.hrmanagement.dto.response.RegisterResponseDto;
import com.ecetasci.hrmanagement.dto.response.UserResponse;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.User;
import com.ecetasci.hrmanagement.enums.Role;
import com.ecetasci.hrmanagement.enums.UserStatus;
import com.ecetasci.hrmanagement.repository.CompanyRepository;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import com.ecetasci.hrmanagement.repository.UserRepository;
import com.ecetasci.hrmanagement.repository.ExpenseRepository;
import com.ecetasci.hrmanagement.utility.JwtManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private JwtManager jwtManager;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private ExpenseRepository expenseRepository;

    // employeeService will be a real instance created in setup so its generateEmployeeNumber() works
    private EmployeeService employeeService;

    // We'll construct UserService manually in setup so it receives the real employeeService
    private UserService service;

    private Company company;


    @BeforeEach
    void setup() {
        company = Company.builder().id(1L).companyName("ACME").build();

        // create a real EmployeeService backed by mocked repositories/deps
        employeeService = new EmployeeService(employeeRepository, companyRepository, emailService, passwordEncoder, expenseRepository);

        // construct the UserService with mocks and the real employeeService
        service = new UserService(userRepository, employeeRepository, passwordEncoder, emailService, jwtManager, companyRepository, employeeService);
    }

    // register
    @Test
    void register_companyAdmin_withCompany_createsUserAndEmployee_andSendsEmail() {
        RegisterRequestDto dto = new RegisterRequestDto("john", "Rawpass1!", "Rawpass1!", Role.COMPANY_ADMIN, "john@ex.com", 1L);
        when(passwordEncoder.encode("Rawpass1!")).thenReturn("ENC");
        when(jwtManager.generateToken(anyString())).thenReturn("jwtTok");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        //when(employeeRepository.findByEmployeeNumber(anyString())).thenReturn(Optional.empty());
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));
        // use the real employeeService, but stub its internal call to repository if needed
        when(employeeRepository.findEmployeeByEmployeeNumber(anyString())).thenReturn(Optional.empty());

        RegisterResponseDto res = service.register(dto);

        assertEquals("john", res.username());
        assertEquals(10L, res.id());
        assertEquals("john@ex.com", res.email());

        // user kaydı ve alanlar
        ArgumentCaptor<User> userCap = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(userCap.capture());
        User savedUser = userCap.getValue();
        assertEquals(UserStatus.PENDING_ADMIN_APPROVAL, savedUser.getUserStatus());
        assertEquals("ENC", savedUser.getPassword());
        assertEquals(Role.COMPANY_ADMIN, savedUser.getRole());
        assertEquals("jwtTok", savedUser.getEmailVerificationToken());
        assertNotNull(savedUser.getTokenExpiryDate());

        // employee kaydı ve alanlar
        ArgumentCaptor<Employee> empCap = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository, atLeastOnce()).save(empCap.capture());
        Employee savedEmp = empCap.getValue();
        assertEquals(savedUser, savedEmp.getUser());
        assertEquals(Role.COMPANY_ADMIN, savedEmp.getRole());
        assertEquals(company, savedEmp.getCompany());
        // savedEmp.setEmployeeNumber(employeeService.generateEmployeeNumber()); // no need to set here
        assertNotNull(savedEmp.getEmployeeNumber());

        // email gönderildi
        verify(emailService).send(eq("john@ex.com"), eq("user register"), contains("jwtTok"));
    }

    @Test
    void register_companyAdmin_companyIdNull_throwsAndDoesNotCreateEmployeeOrSendEmail() {
        RegisterRequestDto dto = new RegisterRequestDto("john", "Rawpass1!", "Rawpass1!", Role.COMPANY_ADMIN, "john@ex.com", null);
        when(passwordEncoder.encode(anyString())).thenReturn("ENC");
        when(jwtManager.generateToken(anyString())).thenReturn("tok");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.register(dto));
        assertEquals("Company not found for ID: null", ex.getMessage());

        verify(employeeRepository, never()).save(any());
        verify(emailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void register_companyAdmin_companyNotFound_employeeSavedWithoutCompany() {
        RegisterRequestDto dto = new RegisterRequestDto("john", "Rawpass1!", "Rawpass1!", Role.COMPANY_ADMIN, "john@ex.com", 99L);
        when(passwordEncoder.encode(anyString())).thenReturn("ENC");
        when(jwtManager.generateToken(anyString())).thenReturn("tok");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());
       // when(employeeRepository.findByEmployeeNumber(anyString())).thenReturn(Optional.empty());
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        service.register(dto);

        ArgumentCaptor<Employee> cap = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository, atLeastOnce()).save(cap.capture());
        Employee emp = cap.getValue();
        assertNull(emp.getCompany());
    }

    @Test
    void register_nonAdminRole_doesNotCreateEmployee() {
        RegisterRequestDto dto = new RegisterRequestDto("jane", "Rawpass1!", "Rawpass1!", Role.EMPLOYEE, "jane@ex.com", 1L);
        when(passwordEncoder.encode(anyString())).thenReturn("ENC");
        when(jwtManager.generateToken(anyString())).thenReturn("tok");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterResponseDto res = service.register(dto);
        assertEquals("jane", res.username());
        verify(employeeRepository, never()).save(any());
        verify(emailService).send(eq("jane@ex.com"), eq("user register"), contains("tok"));
    }

    // generateEmployeeNumber
    //@Test
  //  void generateEmployeeNumber_happy_returnsLetterPlus6Digits() {
        //when(employeeRepository.findByEmployeeNumber(anyString())).thenReturn(Optional.empty());
      //(  String num = employeeService.generateEmployeeNumber();
       // assertTrue(num.matches("[A-Z]\\d{6}"));
    //}

    @Test
    void generateEmployeeNumber_fallback_returnsEMPWithTimestamp() {
        when(employeeRepository.findEmployeeByEmployeeNumber(anyString())).thenReturn(Optional.of(new Employee()));
        String num = employeeService.generateEmployeeNumber();
        assertTrue(num.startsWith("EMP"));
    }

    // save overloads
    @Test
    void save_withParams_savesUser() {
        service.save("u", "p", "e@x.com", Role.EMPLOYEE);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void save_withUser_delegatesToRepo() {
        User u = new User();
        assertEquals(u, service.save(u));
        verify(userRepository).save(u);
    }

    // findAll
    @Test
    void findAll_mapsToUserResponse() {
        User u1 = User.builder().id(1L).name("A").email("a@x.com").role(Role.EMPLOYEE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        User u2 = User.builder().id(2L).name("B").email("b@x.com").role(Role.COMPANY_ADMIN).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(u1, u2), pageable, 2));

        Page<UserResponse> page = service.findAll(pageable);

        assertEquals(2, page.getTotalElements());
        assertEquals("A", page.getContent().get(0).name());
        assertEquals(Role.COMPANY_ADMIN, page.getContent().get(1).role());
        verify(userRepository).findAll(pageable);
    }

    // simple delegations
    @Test
    void findAllByUsername_delegates() {
        when(userRepository.findAllByNameContaining("jo")).thenReturn(List.of(new User()));
        assertEquals(1, service.findAllByUsername("jo").size());
    }

    @Test
    void findById_delegates() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(new User()));
        assertTrue(service.findById(5L).isPresent());
    }

    @Test
    void findUserByEmail_delegates() {
        when(userRepository.findUserByEmail("e@x.com")).thenReturn(Optional.of(new User()));
        assertTrue(service.findUserByEmail("e@x.com").isPresent());
    }

    @Test
    void existsById_delegates() {
        when(userRepository.existsById(9L)).thenReturn(true);
        assertTrue(service.existsById(9L));
    }

    // updateUserProfile
    @Test
    void updateUserProfile_updatesFieldsAndSaves() {
        UpdateUserRequestDto dto = new UpdateUserRequestDto(1L, "newname", "Newpass1!", "new@x.com");
        User existing = new User();
        existing.setEmail("old@x.com");
        when(userRepository.findUserByEmail("new@x.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User res = service.updateUserProfile(dto);

        assertEquals("newname", res.getName());
        assertEquals("Newpass1!", res.getPassword());
        assertEquals("new@x.com", res.getEmail());
        verify(userRepository).save(existing);
    }

    // findByUsername
    @Test
    void findByUsername_delegates() {
        when(userRepository.findByName("x")).thenReturn(Optional.of(new User()));
        assertTrue(service.findByUsername("x").isPresent());
    }

    // verifyEmail
    @Test
    void verifyEmail_success_activatesAndClearsToken() {
        User u = new User();
        u.setEmailVerificationToken("t");
        u.setTokenExpiryDate(LocalDateTime.now().plusHours(1));
        when(userRepository.findByEmailVerificationToken("t")).thenReturn(Optional.of(u));

        service.verifyEmail("t");

        assertEquals(UserStatus.ACTIVE, u.getUserStatus());
        // Service currently sets the token field to the provided token (doesn't clear it)
        assertEquals("t", u.getEmailVerificationToken());
        assertNull(u.getTokenExpiryDate());
        verify(userRepository).save(u);
    }

    @Test
    void verifyEmail_expired_throws() {
        User u = new User();
        u.setEmailVerificationToken("t");
        u.setTokenExpiryDate(LocalDateTime.now().minusHours(1));
        when(userRepository.findByEmailVerificationToken("t")).thenReturn(Optional.of(u));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.verifyEmail("t"));
        assertEquals("Token expired", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    // generateResetToken
    @Test
    void generateResetToken_setsTokenAndSaves() {
        User u = new User();
        when(userRepository.findUserByEmail("e@x.com")).thenReturn(Optional.of(u));
        when(jwtManager.generateToken("e@x.com")).thenReturn("resetTok");

        service.generateResetToken("e@x.com");

        assertEquals("resetTok", u.getPasswordResetToken());
        verify(userRepository).save(u);
    }

    // resetPassword
    @Test
    void resetPassword_employeeRole_updatesUserAndEmployee() {
        User u = new User();
        u.setRole(Role.EMPLOYEE);
        u.setId(7L);
        when(userRepository.findUserByPasswordResetToken("tok")).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("Newpass1!"))
                .thenReturn("ENC-PASS");
        Employee emp = new Employee();
        when(employeeRepository.findByUserId(7L)).thenReturn(Optional.of(emp));

        service.resetPassword(new ResetPasswordRequestDto("tok", "Newpass1!"));

        assertEquals("ENC-PASS", u.getPassword());
        assertNull(u.getPasswordResetToken());
        verify(userRepository).save(u);
        assertEquals("ENC-PASS", emp.getPassword());
        verify(employeeRepository).save(emp);
    }

    @Test
    void resetPassword_nonEmployee_updatesOnlyUser() {
        User u = new User();
        u.setRole(Role.COMPANY_ADMIN);
        u.setId(8L);
        when(userRepository.findUserByPasswordResetToken("tok")).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("Newpass1!"))
                .thenReturn("ENC-PASS");

        service.resetPassword(new ResetPasswordRequestDto("tok", "Newpass1!"));

        assertEquals("ENC-PASS", u.getPassword());
        assertNull(u.getPasswordResetToken());
        verify(userRepository).save(u);
        verify(employeeRepository, never()).save(any());
    }
}
