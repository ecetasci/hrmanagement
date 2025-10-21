package com.ecetasci.hrmanagement.repository;

import com.ecetasci.hrmanagement.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    List<Shift> findByCompanyId(Long companyId);
}