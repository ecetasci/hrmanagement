package com.ecetasci.hrmanagement.repository;

import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.LeaveRequest;
import com.ecetasci.hrmanagement.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest,Long> {
    List<LeaveRequest> findByEmployee_Company_IdAndStatus(Long companyId, LeaveStatus status);

    List<LeaveRequest> findByEmployee_Id(Long employeeId);
}
