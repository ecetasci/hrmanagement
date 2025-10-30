package com.ecetasci.hrmanagement.entity;

import com.ecetasci.hrmanagement.enums.CompanyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "company")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String companyName;

    @Column(nullable = false, length = 150)
    private String companyEmail;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 255)
    private String address;

    @Column(length = 50)
    private String taxNumber;

    @Column(length = 150)
    private String website;

    private Integer employeeCount;

    private LocalDate foundedDate;

    private String logoUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "company", cascade = {CascadeType.ALL})
    private List<CompanySubscription> subscriptions;

    @OneToMany(mappedBy = "company", cascade = {CascadeType.ALL})
    private List<Employee> employees;

    //@Enumerated(EnumType.STRING)
    //private UserStatus userStatus;

    @OneToOne(mappedBy = "company", cascade = CascadeType.ALL)
    private CompanyReview companyReview;

    @Column
    @Enumerated(EnumType.STRING)
    private CompanyStatus companyStatus;


    @Column
    private boolean isActive;


}
