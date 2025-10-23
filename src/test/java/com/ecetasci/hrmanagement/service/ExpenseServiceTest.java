package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.ExpenseCreateRequest;
import com.ecetasci.hrmanagement.dto.response.ExpenseResponseDto;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.Expense;
import com.ecetasci.hrmanagement.enums.ExpenseStatus;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import com.ecetasci.hrmanagement.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private EmployeeRepository employeeRepository;

    @InjectMocks
    private ExpenseService service;

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = Employee.builder().id(1L).name("John").email("john@example.com").password("p").build();
    }

    @Test
    void getEmployeeExpenses_returnsMappedDtos() {
        Expense e1 = Expense.builder()
                .id(10L).description("Taxi").amount(BigDecimal.TEN)
                .expenseDate(LocalDate.of(2025,1,2)).status(ExpenseStatus.PENDING).build();
        Expense e2 = Expense.builder()
                .id(11L).description("Meal").amount(BigDecimal.ONE)
                .expenseDate(LocalDate.of(2025,1,3)).status(ExpenseStatus.APPROVED).build();
        when(expenseRepository.findByEmployee_Id(1L)).thenReturn(List.of(e1, e2));

        List<ExpenseResponseDto> list = service.getEmployeeExpenses(1L);

        assertEquals(2, list.size());
        assertEquals(10L, list.get(0).id());
        assertEquals("Meal", list.get(1).description());
        assertEquals(ExpenseStatus.APPROVED, list.get(1).status());
        verify(expenseRepository).findByEmployee_Id(1L);
    }

    @Test
    void getCompanyExpenses_returnsMappedDtos() {
        Expense e1 = Expense.builder().id(20L).description("Laptop bag").amount(BigDecimal.valueOf(50)).expenseDate(LocalDate.now()).status(ExpenseStatus.PENDING).build();
        when(expenseRepository.findByEmployee_Company_Id(7L)).thenReturn(List.of(e1));

        List<ExpenseResponseDto> list = service.getCompanyExpenses(7L);

        assertEquals(1, list.size());
        assertEquals(20L, list.get(0).id());
        assertEquals("Laptop bag", list.get(0).description());
        verify(expenseRepository).findByEmployee_Company_Id(7L);
    }

    @Test
    void createExpense_employeeNotFound_throws() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());
        ExpenseCreateRequest dto = new ExpenseCreateRequest("Taxi", BigDecimal.TEN, LocalDate.now());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createExpense(1L, dto));
        assertEquals("Employee not found", ex.getMessage());
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void createExpense_success_persistsPendingAndReturnsDto() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
            Expense saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });
        LocalDate date = LocalDate.of(2025, 1, 5);
        ExpenseCreateRequest dto = new ExpenseCreateRequest("Taxi", BigDecimal.TEN, date);

        ExpenseResponseDto res = service.createExpense(1L, dto);

        assertEquals(100L, res.id());
        assertEquals("Taxi", res.description());
        assertEquals(BigDecimal.TEN, res.amount());
        assertEquals(date, res.expenseDate());
        assertEquals(ExpenseStatus.PENDING, res.status());

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        Expense toSave = captor.getValue();
        assertEquals(employee, toSave.getEmployee());
        assertEquals(ExpenseStatus.PENDING, toSave.getStatus());
    }

    @Test
    void approveExpense_notFound_throws() {
        when(expenseRepository.findById(9L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.approveExpense(9L));
        assertEquals("Expense not found", ex.getMessage());
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void approveExpense_setsApprovedAndWillAddTrue_andPersists() {
        Expense exp = Expense.builder().id(5L).status(ExpenseStatus.PENDING).willAdd(false).build();
        when(expenseRepository.findById(5L)).thenReturn(Optional.of(exp));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        service.approveExpense(5L);

        assertEquals(ExpenseStatus.APPROVED, exp.getStatus());
        assertTrue(Boolean.TRUE.equals(exp.getWillAdd()));
        verify(expenseRepository).save(exp);
    }

    @Test
    void rejectExpense_notFound_throws() {
        when(expenseRepository.findById(8L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.rejectExpense(8L));
        assertEquals("Expense not found", ex.getMessage());
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void rejectExpense_setsRejectedAndWillAddFalse_andPersists() {
        Expense exp = Expense.builder().id(6L).status(ExpenseStatus.PENDING).willAdd(true).build();
        when(expenseRepository.findById(6L)).thenReturn(Optional.of(exp));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        service.rejectExpense(6L);

        assertEquals(ExpenseStatus.REJECTED, exp.getStatus());
        assertFalse(Boolean.TRUE.equals(exp.getWillAdd()));
        verify(expenseRepository).save(exp);
    }

    @Test
    void updateRejectedExpense_notFound_throws() {
        when(expenseRepository.findById(4L)).thenReturn(Optional.empty());
        ExpenseCreateRequest dto = new ExpenseCreateRequest("x", BigDecimal.ONE, LocalDate.now());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateRejectedExpense(4L, dto));
        assertEquals("Expense not found", ex.getMessage());
    }

    @Test
    void updateRejectedExpense_wrongStatus_throws() {
        Expense exp = Expense.builder().id(7L).status(ExpenseStatus.PENDING).build();
        when(expenseRepository.findById(7L)).thenReturn(Optional.of(exp));
        ExpenseCreateRequest dto = new ExpenseCreateRequest("x", BigDecimal.ONE, LocalDate.now());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateRejectedExpense(7L, dto));
        assertEquals("Only rejected expenses can be updated!", ex.getMessage());
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void updateRejectedExpense_success_updatesFieldsSetsPending_andReturnsDto() {
        Expense exp = Expense.builder().id(7L).status(ExpenseStatus.REJECTED).description("old").amount(BigDecimal.ONE).expenseDate(LocalDate.of(2025,1,1)).build();
        when(expenseRepository.findById(7L)).thenReturn(Optional.of(exp));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseCreateRequest dto = new ExpenseCreateRequest("newDesc", BigDecimal.TEN, LocalDate.of(2025,2,2));
        ExpenseResponseDto res = service.updateRejectedExpense(7L, dto);

        assertEquals(7L, res.id());
        assertEquals("newDesc", res.description());
        assertEquals(BigDecimal.TEN, res.amount());
        assertEquals(LocalDate.of(2025,2,2), res.expenseDate());
        assertEquals(ExpenseStatus.PENDING, res.status());

        assertEquals("newDesc", exp.getDescription());
        assertEquals(BigDecimal.TEN, exp.getAmount());
        assertEquals(LocalDate.of(2025,2,2), exp.getExpenseDate());
        assertEquals(ExpenseStatus.PENDING, exp.getStatus());
        verify(expenseRepository).save(exp);
    }
}

