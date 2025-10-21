package com.ecetasci.hrmanagement.repository;

import com.ecetasci.hrmanagement.entity.Expense;
import com.ecetasci.hrmanagement.enums.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // Çalışana ait tüm masraflar
    List<Expense> findByEmployee_Id(Long employeeId);

    // Şirket yöneticisi için tüm masraflar
    List<Expense> findByEmployee_Company_Id(Long companyId);

    List<Expense> findByEmployee_Company_IdAndStatus(Long companyId, ExpenseStatus status);
    List<Expense> findTop3ByEmployee_IdOrderByExpenseDateDesc(Long employeeId);
}
