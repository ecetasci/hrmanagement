package com.ecetasci.hrmanagement.repository;

import com.ecetasci.hrmanagement.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    boolean existsBySerialNumber(String serialNumber);

    // Find assets by company id
    List<Asset> findAllByCompanyId(Long companyId);
}
