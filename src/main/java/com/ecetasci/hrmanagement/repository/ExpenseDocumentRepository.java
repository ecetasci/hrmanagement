package com.ecetasci.hrmanagement.repository;

import com.ecetasci.hrmanagement.entity.ExpenseDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseDocumentRepository extends JpaRepository<ExpenseDocument, Long> {
    List<ExpenseDocument> findAllByExpense_Id(Long expenseId);
}
