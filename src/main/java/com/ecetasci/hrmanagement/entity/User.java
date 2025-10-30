package com.ecetasci.hrmanagement.entity;

import com.ecetasci.hrmanagement.enums.Role;
import com.ecetasci.hrmanagement.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column
    private UserStatus userStatus;

    @Column(nullable = false)
    private boolean isFirstAdmin = false;

    private String emailVerificationToken;

    private String passwordResetToken;


    private LocalDateTime tokenExpiryDate;


    private LocalDateTime createdAt;


    private LocalDateTime updatedAt;


}

