package com.ecetasci.hrmanagement.repository;

import com.ecetasci.hrmanagement.entity.Asset;
import com.ecetasci.hrmanagement.entity.EmployeeAsset;
import com.ecetasci.hrmanagement.enums.EmployeeAssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeAssetRepository extends JpaRepository<EmployeeAsset, Long> {
    boolean existsByAssetAndStatusIn(Asset asset, List<EmployeeAssetStatus> statuses);
    List<EmployeeAsset> findByEmployeeId(Long employeeId);
}
