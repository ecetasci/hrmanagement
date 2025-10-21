package com.ecetasci.hrmanagement.repository;

import com.ecetasci.hrmanagement.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    List<LeaveType> findAllByCompanyId(Long companyId);
}
