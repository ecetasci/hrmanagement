package com.ecetasci.hrmanagement.entity;

import com.ecetasci.hrmanagement.enums.EmployeeAssetStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class EmployeeAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "asset_id")
    private Asset asset;


    private LocalDate assignedDate;

    @Enumerated(EnumType.STRING)
    private EmployeeAssetStatus status;

    private String employeeNote;
}