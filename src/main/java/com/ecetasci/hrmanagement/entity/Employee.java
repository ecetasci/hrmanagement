package com.ecetasci.hrmanagement.entity;

import com.ecetasci.hrmanagement.enums.Role;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String employeeNumber;

    @Column
    private LocalDate birthDate;

    @Column
    private LocalDate hireDate;

    @Column
    private String position;

    @Column
    private String department;

    @Column
    private BigDecimal salary;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 255)
    @JsonManagedReference("company-subscriptions")
    private String address;

    @Column(length = 150)
    @JsonManagedReference("company-employees")
    private String emergencyContact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.EMPLOYEE;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private Integer leaveBalance = 15;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<LeaveRequest> leaveRequests;


    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<Expense> expenses;


}
