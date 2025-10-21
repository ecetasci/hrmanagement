package com.ecetasci.hrmanagement.repository;

import com.ecetasci.hrmanagement.entity.Break;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BreakRepository extends JpaRepository<Break, Long> {
    List<Break> findByShiftId(Long shiftId);
}