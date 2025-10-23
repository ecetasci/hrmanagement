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

import static com.ecetasci.hrmanagement.enums.Role.COMPANY_ADMIN;

/**
 * AuthController — kimlik ve yetkilendirme işlemleri.
 *
 * Bu controller aşağıdaki işlevleri sağlar:
 * - Şirket yönetici kaydı
 * - E-posta doğrulama (verify-email)
 * - Giriş (login)
 * - Parola sıfırlama isteği ve sıfırlama (forgot-password, reset-password)
 * - Logout
 *
 * Not: Metot parametreleri DTO olarak alınır; dönen cevaplar proje genelindeki `BaseResponse` sarmalayıcısı ile dönülür.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth")
public class AuthController {
    private final UserService userService;
    private final CompanyService companyService;
    private final EmployeeService employeeService;
    private final AuthenticationManager authenticationManager;
    private final JwtManager jwtManager;
    private final PasswordEncoder passwordEncoder;


    /**
     * Şirket yönetici kaydı yapar.
     *
     * @param dto Kayıt için gerekli alanları taşıyan DTO (isim, email, password, companyId vb.)
     * @return Kayıt sonucu ve oluşturulan User nesnesi içeren BaseResponse
     */
    @PostMapping("/company-manager-register")
    @Transactional
    public ResponseEntity<BaseResponse<User>> register(@RequestBody @Valid RegisterCompanyManagerRequestDto dto) {

        if ((userService.findUserByEmail(dto.email())).isPresent()) {
            return ResponseEntity.badRequest().body(BaseResponse.<User>builder()
                    .success(false)
                    .code(400)
                    .message("Bu kullanıcı zaten kayıtlıdır")
                    .build());
        }
            User user = new User();
            user.setName(dto.name());
            user.setEmail(dto.email());
            user.setPassword(passwordEncoder.encode(dto.password())); // şifre encode
            user.setRole(COMPANY_ADMIN);
            user.setPasswordResetToken(jwtManager.generateToken(dto.email()));
            user.setCreatedAt(LocalDateTime.now());
            user.setEmailVerificationToken(jwtManager.generateToken(dto.email()));
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

            employee.setEmployeeNumber(employeeService.generateEmployeeNumber());

            employeeService.save(employee);

            return ResponseEntity.ok(BaseResponse.<User>builder()
                    .success(true)
                    .code(201)
                    .message("Registration successful")
                    .data(saved)
                    .build());
        //to do response dto yaz  parolayı dönmesin
    }

    /**
     * E-posta doğrulama token'ını alır ve kullanıcıyı aktif hale getirir.
     *
     * @param token E-posta doğrulama amacı ile oluşturulmuş JWT token
     * @return Başarı/başarısızlık bilgisini içeren BaseResponse
     */
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

    /**
     * Kullanıcı girişi yapar.
     *
     * @param dto Login bilgilerini içeren DTO (email, password)
     * @return Giriş başarılıysa JWT token ve kullanıcı bilgilerini içeren BaseResponse
     */
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponseDto>> login(@RequestBody @Valid LoginRequestDto dto) {
        var optUser = userService.findUserByEmail(dto.email());
        if (optUser.isEmpty()) {
            return ResponseEntity.status(404).body(BaseResponse.<LoginResponseDto>builder()
                    .success(false)
                    .code(404)
                    .message("User not found")
                    .build());
        }
        User user = optUser.get();
        if (!UserStatus.ACTIVE.equals(user.getUserStatus())) {
            return ResponseEntity.status(403).body(BaseResponse.<LoginResponseDto>builder()
                    .success(false)
                    .code(403)
                    .message("Email not verified")
                    .build());
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(dto.email(),
                    dto.password()));
           String token = jwtManager.generateToken(dto.email());
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

    /**
     * Parola sıfırlama linki oluşturur ve e-posta gönderir.
     *
     * @param email Parola sıfırlama isteği yapılan kullanıcının email adresi
     * @return İşlem sonucu mesajı içeren BaseResponse
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<BaseResponse<String>> forgotPassword(@RequestParam String email) {
        userService.generateResetToken(email);

        return ResponseEntity.ok(BaseResponse.<String>builder()
                .success(true)
                .code(200)
                .message("Password reset link sent to your email")
                .build());
    }

    /**
     * Yeni parolayı set eder. Gelen DTO içinde token ve yeni parola bulunur.
     *
     * @param dto ResetPasswordRequestDto: token ve yeni parolayı içerir
     * @return İşlem sonucu mesajı içeren BaseResponse
     */
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

    /**
     * Oturumu kapatır (token geçersizleştirme).
     *
     * @param token İptal edilmek istenen JWT token
     * @return İşlem sonucu mesajı içeren BaseResponse
     */
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
