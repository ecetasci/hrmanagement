package com.ecetasci.hrmanagement.entity;

import com.ecetasci.hrmanagement.enums.AssetType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String brand;
    private String model;
    private String serialNumber;
    private BigDecimal value;
    @Enumerated(EnumType.STRING)
    private AssetType type;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @OneToMany(mappedBy = "asset")
    private List<EmployeeAsset> employeeAssets = new ArrayList<>();
}