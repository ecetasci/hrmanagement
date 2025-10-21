package com.ecetasci.hrmanagement.repository;

import com.ecetasci.hrmanagement.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeNumber(String employeeNumber);

    Page<Employee> findAllByCompanyId(Long companyId, Pageable pageable);

    Optional<List<Employee>> findEmployeesByCompanyCompanyName(String companyName);

    Optional<Employee> findEmployeeById(Long id);

    Boolean existsEmployeeById(Long id);

    Optional<Employee> findEmployeeByUserId(Long id);

    Optional<Employee> findByUserId(Long id);

    List<Employee> findByCompany_Id(Long companyId);
}
