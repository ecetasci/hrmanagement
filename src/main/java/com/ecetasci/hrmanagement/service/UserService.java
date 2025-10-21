package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.RegisterRequestDto;
import com.ecetasci.hrmanagement.dto.request.ResetPasswordRequestDto;
import com.ecetasci.hrmanagement.dto.request.UpdateUserRequestDto;
import com.ecetasci.hrmanagement.dto.response.RegisterResponseDto;
import com.ecetasci.hrmanagement.dto.response.UserResponse;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.User;
import com.ecetasci.hrmanagement.enums.Role;
import com.ecetasci.hrmanagement.enums.UserStatus;
import com.ecetasci.hrmanagement.repository.CompanyRepository;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import com.ecetasci.hrmanagement.repository.UserRepository;
import com.ecetasci.hrmanagement.utility.JwtManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtManager jwtManager;
    private final CompanyRepository companyRepository;

    public User findUserPasswordResetToken(String token) {
        User user = userRepository.findUserByPasswordResetToken(token).orElseThrow();
        return user;
    }

    @Transactional
    public RegisterResponseDto register(@Valid RegisterRequestDto dto) {
        User savedUser = userRepository.save(User.builder()
                .name(dto.username())
                .password(passwordEncoder.encode(dto.password()))
                .email(dto.email())
                .role(dto.role())
                .userStatus(UserStatus.PENDING_ADMIN_APPROVAL)
                .emailVerificationToken(jwtManager.generateToken(dto.username()))
                .tokenExpiryDate(LocalDateTime.now().plusHours(48))
                .build());

        // Eğer rol Company Admin ise aynı zamanda bir Employee oluştur
        if (dto.role() == Role.COMPANY_ADMIN) {
            Employee employee = new Employee();
            employee.setUser(savedUser);
            employee.setName(savedUser.getName());
            employee.setEmail(savedUser.getEmail());
            employee.setPassword(savedUser.getPassword()); // encoded şifre
            employee.setEmployeeNumber(generateEmployeeNumber());
            employee.setRole(Role.COMPANY_ADMIN);
            if(dto.companyId()!=null){
                companyRepository.findById( dto.companyId()).ifPresent(company -> {
                    employee.setCompany(company);
                    employeeRepository.save(employee);
            }
            );
            }
            else{
                throw new RuntimeException("Company not found for ID: " + dto.companyId());
            }
            employeeRepository.save(employee);

        }

        emailService.send(savedUser.getEmail(), "user register",
                "registering completed, please verify your email with token: " + savedUser.getEmailVerificationToken());

        return new RegisterResponseDto(savedUser.getName(), savedUser.getId(), savedUser.getEmail());
    }


    private String generateEmployeeNumber() {
        // Prefix: tek büyük harf, ardından 6 haneli sıfır dolgulu sayı => A123456
        for (int attempt = 0; attempt < 10; attempt++) {
            char prefix = (char) ('A' + ThreadLocalRandom.current().nextInt(26));
            int number = ThreadLocalRandom.current().nextInt(0, 1_000_000);
            String candidate = String.format("%c%06d", prefix, number);

            // repository'de var mı kontrol et (findByEmployeeNumber var varsayımıyla)
            if (employeeRepository.findByEmployeeNumber(candidate).isEmpty()) {
                return candidate;
            }
        }

        // Nadiren çakışma olursa güvenli fallback
        return "EMP" + System.currentTimeMillis();
    }

    public void save(String username, String password, String email, Role role) {
        User user = User.builder().name(username).password(password).email(email).role(role).build();
        userRepository.save(user);
    }

    public User save(User user) {
        userRepository.save(user);
        return user;
    }

    //pageablea çevir
    public List<UserResponse> findAll() {
        List<User> users = userRepository.findAll();
        System.out.println(users.get(1).getCreatedAt());
        return users.stream().map(user -> new UserResponse(user.getId(), user.getName(), user.getCreatedAt(), user.getUpdatedAt()
                , user.getEmail(), user.getRole())).toList();
    }


    public List<User> findAllByUsername(String username) {
        return userRepository.findAllByNameContaining(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }


    public boolean existsById(@NotBlank Long userId) {
        return userRepository.existsById(userId);
    }


    public User updateUserProfile(@Valid UpdateUserRequestDto dto) {

        User user = userRepository.findUserByEmail(dto.email()).orElseThrow();
        user.setName(dto.username());
        user.setPassword(dto.password());
        user.setEmail(dto.email());
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByName(username);
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        user.setUserStatus(UserStatus.ACTIVE);
        user.setEmailVerificationToken(null);
        user.setTokenExpiryDate(null);

        userRepository.save(user);
    }

    public void generateResetToken(String email) {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtManager.generateToken(email);
        user.setPasswordResetToken(token);
        userRepository.save(user);
        System.out.println(token);

        // mailService.send(...) ile mail atılır
    }


    public void resetPassword(ResetPasswordRequestDto dto) {
        User user = userRepository.findUserByPasswordResetToken(dto.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

        String encodedPassword = passwordEncoder.encode(dto.getNewPassword());

        // User update
        user.setPassword(encodedPassword);
        user.setPasswordResetToken(null);
        userRepository.save(user);

        // Eğer Employee ise, Employee tablosunu da güncelle
        // Manager da user employee olduğundan ayrı method yazmadım.
        if (user.getRole() == Role.EMPLOYEE) {
            employeeRepository.findByUserId(user.getId()).ifPresent(employee -> {
                employee.setPassword(encodedPassword);
                employeeRepository.save(employee);
            });
        }
    }

}