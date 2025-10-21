package com.ecetasci.hrmanagement.repository;

import com.ecetasci.hrmanagement.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    boolean existsBySerialNumber(String serialNumber);
}
