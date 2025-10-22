package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.ExpenseCreateRequest;
import com.ecetasci.hrmanagement.dto.response.ExpenseResponseDto;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.Expense;
import com.ecetasci.hrmanagement.enums.ExpenseStatus;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import com.ecetasci.hrmanagement.repository.ExpenseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final EmployeeRepository employeeRepository;


    public List<ExpenseResponseDto> getEmployeeExpenses(Long employeeId) {
        return expenseRepository.findByEmployee_Id(employeeId).stream()
                .map(expense -> new ExpenseResponseDto(
                        expense.getId(),
                        expense.getDescription(),
                        expense.getAmount(),
                        expense.getExpenseDate(),
                        expense.getStatus()
                ))
                .toList();
    }


    public ExpenseResponseDto createExpense(Long employeeId, ExpenseCreateRequest dto) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Expense expense = Expense.builder()
                .expenseDate(dto.expenseDate())
                .amount(dto.amount())
                .description(dto.description())
                .employee(employee)
                .status(ExpenseStatus.PENDING)
                .build();

        Expense saved = expenseRepository.save(expense);

        return new ExpenseResponseDto(
                saved.getId(),
                saved.getDescription(),
                saved.getAmount(),
                saved.getExpenseDate(),
                saved.getStatus()
        );

    }

    public List<ExpenseResponseDto> getCompanyExpenses(Long companyId) {
        return expenseRepository.findByEmployee_Company_Id(companyId).stream()
                .map(expense -> new ExpenseResponseDto(
                        expense.getId(),
                        expense.getDescription(),
                        expense.getAmount(),
                        expense.getExpenseDate(),
                        expense.getStatus()
                ))
                .toList();
    }

    @Transactional
    public void approveExpense(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        expense.setStatus(ExpenseStatus.APPROVED);
        expense.setWillAdd(true);
        expenseRepository.save(expense);
    }

    @Transactional
    public void rejectExpense(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        expense.setStatus(ExpenseStatus.REJECTED);
        expense.setWillAdd(false);
        expenseRepository.save(expense);
    }

    public ExpenseResponseDto updateRejectedExpense(Long expenseId, ExpenseCreateRequest dto) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (!ExpenseStatus.REJECTED.equals(expense.getStatus())) {
            throw new RuntimeException("Only rejected expenses can be updated!");
        }

        expense.setExpenseDate(dto.expenseDate());
        expense.setAmount(dto.amount());
        expense.setDescription(dto.description());
        expense.setStatus(ExpenseStatus.PENDING);
        Expense saved = expenseRepository.save(expense);

        return new ExpenseResponseDto(saved.getId(), saved.getDescription(), saved.getAmount(), saved.getExpenseDate(), saved.getStatus());
    }
}
