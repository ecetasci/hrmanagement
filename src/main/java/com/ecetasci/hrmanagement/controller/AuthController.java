package com.ecetasci.hrmanagement.controller;

import com.ecetasci.hrmanagement.dto.request.ResetPasswordRequestDto;
import com.ecetasci.hrmanagement.dto.response.BaseResponse;
import com.ecetasci.hrmanagement.dto.request.LoginRequestDto;
import com.ecetasci.hrmanagement.dto.request.RegisterCompanyManagerRequestDto;
import com.ecetasci.hrmanagement.dto.response.LoginResponseDto;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.User;
import com.ecetasci.hrmanagement.enums.UserStatus;
import com.ecetasci.hrmanagement.security.UserPrincipal;
import com.ecetasci.hrmanagement.service.*;
import com.ecetasci.hrmanagement.utility.JwtManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.ecetasci.hrmanagement.enums.Role.COMPANY_ADMIN;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth")
public class AuthController {
    private final UserService userService;
    private final CompanyService companyService;
    private final EmployeeService employeeService;
    private final SiteAdminService siteAdminService;
    private final CompanyManagerService companyManagerService;
    private final AuthenticationManager authenticationManager;
    private final JwtManager jwtManager;
    private final PasswordEncoder passwordEncoder;


    //manager da bir employee
    @PostMapping("/company-manager-register")
    @Transactional
    public ResponseEntity<BaseResponse<User>> register(@RequestBody @Valid RegisterCompanyManagerRequestDto dto) {

        if ((userService.findUserByEmail(dto.email())).isPresent()) {
            throw new RuntimeException("Bu kullanıcı zaten kayıtlıdır");
        }
            User user = new User();
            user.setName(dto.name());
            user.setEmail(dto.email());
            user.setPassword(passwordEncoder.encode(dto.password())); // şifre encode
            user.setRole(COMPANY_ADMIN);
            user.setPasswordResetToken(jwtManager.generateToken(dto.name()));
            user.setCreatedAt(LocalDateTime.now());
            user.setEmailVerificationToken(jwtManager.generateToken(dto.name()));
            user.setUserStatus(UserStatus.PENDING_EMAIL_VERIFICATION);


            User saved = userService.save(user);

            Employee employee =new Employee();
            employee.setUser(saved);
            employee.setName(dto.name());
            employee.setEmail(dto.email());
            employee.setPassword(saved.getPassword());
            employee.setRole(saved.getRole());
            employee.setCompany(companyService.findById(dto.companyId()));
            employee.setCreatedAt(saved.getCreatedAt());
            //TO DO number generate için bir methıd yazarım
            employee.setEmployeeNumber("testnumber");

            employeeService.save(employee);

            return ResponseEntity.ok(BaseResponse.<User>builder()
                    .success(true)
                    .code(201)
                    .message("Registration successful")
                    .data(saved)
                    .build());
        //to do response dto yaz  parolayı dönmesin
    }

    @PostMapping("/verify-email")
    public ResponseEntity<BaseResponse<String>> verifyEmail(@RequestParam String token) {
        try {
            String username = jwtManager.extractUsername(token);
            if (username == null) {
                return ResponseEntity.badRequest().body(BaseResponse.<String>builder()
                        .success(false)
                        .code(400)
                        .message("Invalid token")
                        .build());
            }

            UserDetails userDetails = new UserPrincipal(
                    userService.findByUsername(username).orElseThrow()
            );

            if (jwtManager.verifyToken(token, userDetails)) {
                User user=userService.findByUsername(username).orElseThrow();
                user.setUserStatus(UserStatus.ACTIVE);
                userService.save(user);
                         return ResponseEntity.ok(BaseResponse.<String>builder()
                        .success(true)
                        .code(200)
                        .message("Email verified successfully")
                        .data("Verified user: " + username)
                        .build());
            } else {
                return ResponseEntity.badRequest().body(BaseResponse.<String>builder()
                        .success(false)
                        .code(400)
                        .message("Token verification failed")
                        .build());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(BaseResponse.<String>builder()
                    .success(false)
                    .code(400)
                    .message("Verification failed: " + e.getMessage())
                    .build());
        }
    }

    //logout olmadan tekrar çalıştılıbailiyor nasıl düzeltiriz
    @PostMapping("/manager-login")
    public ResponseEntity<BaseResponse<LoginResponseDto>> login(@RequestBody @Valid LoginRequestDto dto) {
        User user = userService.findUserByEmail(dto.email()).orElseThrow();
        if (!(user.getUserStatus().equals(UserStatus.ACTIVE))) {
            throw new RuntimeException("Email not verified");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(dto.name(),
                    dto.password()));

           String token = jwtManager.generateToken(dto.name());

            LoginResponseDto loginResponseDto=new LoginResponseDto(token,user.getName(), user.getEmail(), user.getRole());

            return ResponseEntity.ok().body(BaseResponse.<LoginResponseDto>builder()
                    .success(true)
                    .code(200)
                    .message("Login Successful")
                    .data(loginResponseDto)
                    .build());
        }
        catch (Exception e) {
            System.out.println("Login failed:"+e.getMessage());
            return ResponseEntity.badRequest().body(BaseResponse.<LoginResponseDto>builder()
                    .success(false)
                    .code(400)
                    .message("Invalid username or password")
                    .build());
        }
    }






    @PostMapping("/forgot-password")
    public ResponseEntity<BaseResponse<String>> forgotPassword(@RequestParam String email) {
        userService.generateResetToken(email);

        return ResponseEntity.ok(BaseResponse.<String>builder()
                .success(true)
                .code(200)
                .message("Password reset link sent to your email")
                .build());
    }


    @PostMapping("/reset-password")
    public ResponseEntity<BaseResponse<String>> resetPassword(@RequestBody @Valid ResetPasswordRequestDto dto) {
        try {
            userService.resetPassword(dto);
            return ResponseEntity.ok(BaseResponse.<String>builder()
                    .success(true)
                    .code(200)
                    .message("Password reset successful")
                    .data("Password updated")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(BaseResponse.<String>builder()
                    .success(false)
                    .code(400)
                    .message("Password reset failed: " + e.getMessage())
                    .build());
        }
    }


    //● POST /api/auth/logout - Çıkış yapma //login sırasında tutulan tokenla
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<String>> logout(@RequestBody String token) {

            try {
            jwtManager.invalidateToken(token);
            return ResponseEntity.ok(BaseResponse.<String>builder()
                    .success(true)
                    .code(200)
                    .message("Logout successful")
                    .data("Token invalidated")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(BaseResponse.<String>builder()
                    .success(false)
                    .code(400)
                    .message("Logout failed: " + e.getMessage())
                    .build());
        }
    }



}
