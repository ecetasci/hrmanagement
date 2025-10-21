package com.ecetasci.hrmanagement.controller;


import com.ecetasci.hrmanagement.dto.request.RegisterRequestDto;
import com.ecetasci.hrmanagement.dto.request.UpdateUserRequestDto;
import com.ecetasci.hrmanagement.dto.response.BaseResponse;
import com.ecetasci.hrmanagement.dto.response.LoginResponseDto;
import com.ecetasci.hrmanagement.dto.response.RegisterResponseDto;
import com.ecetasci.hrmanagement.dto.response.UserResponse;
import com.ecetasci.hrmanagement.entity.User;
import com.ecetasci.hrmanagement.enums.UserStatus;
import com.ecetasci.hrmanagement.repository.UserRepository;
import com.ecetasci.hrmanagement.service.EmailService;
import com.ecetasci.hrmanagement.service.UserService;
import com.ecetasci.hrmanagement.utility.JwtManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@RestController
@RequiredArgsConstructor
@RequestMapping("api/user")
public class UserController {
    private final UserService userService;
    private final EmailService emailService;


    @GetMapping("/find-all")
    public ResponseEntity<BaseResponse<List<UserResponse>>> findAll() {

        return ResponseEntity.ok(BaseResponse.<List<UserResponse>>builder()
                .success(true)
                .code(200)
                .message("user-list")
                .data(userService.findAll()).build());

    }


    @GetMapping("/find-by-username")
    public List<User> findAllByUsername(@RequestParam String username) {
        return userService.findAllByUsername(username);
    }


    @GetMapping("/find-by-id" + "/{id}")
    public ResponseEntity<User> findById(@PathVariable Long id) {
        if (id == null || id < 0) {
            return ResponseEntity.badRequest().body(null);
        }
        Optional<User> user = userService.findById(id);
        return user.isPresent() ? ResponseEntity.ok(user.get()) : ResponseEntity.notFound().build();
    }

    //SİTE ADMİN
    @PostMapping("/register")
    public ResponseEntity<BaseResponse<RegisterResponseDto>> register(@RequestBody @Valid RegisterRequestDto dto) {

        System.out.println("register metodu tetiklendi.");
        RegisterResponseDto registeredUser = userService.register(dto);
       // emailService.send(dto.email(), "kayıt", "user kaydedildi");
        return ResponseEntity.ok(
                BaseResponse.<RegisterResponseDto>builder()
                        .success(true)
                        .code(200)
                        .message("Register işlemi başarıyla tamamlandı.")
                        .data(registeredUser)
                        .build()
        );
    }


    @PutMapping("/update-user-profile")
    public ResponseEntity<BaseResponse<User>> updateUserProfile(
            @RequestBody @Valid UpdateUserRequestDto dto) {
        User updated = userService.updateUserProfile(dto);
        //emailService.send(dto.email(), "update", "user güncellendi");
        return ResponseEntity.ok(
                BaseResponse.<User>builder()
                        .success(true)
                        .code(200)
                        .message("Kullanıcı başarıyla güncellendi.")
                        .data(updated)
                        .build()
        );
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam String token) {
        userService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully");
    }


}