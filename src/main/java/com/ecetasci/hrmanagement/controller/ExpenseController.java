package com.ecetasci.hrmanagement.controller;

import com.ecetasci.hrmanagement.dto.request.ExpenseCreateRequest;
import com.ecetasci.hrmanagement.dto.response.ExpenseDocumentResponseDto;
import com.ecetasci.hrmanagement.dto.response.ExpenseResponseDto;
import com.ecetasci.hrmanagement.entity.ExpenseDocument;
import com.ecetasci.hrmanagement.service.ExpenseDocumentService;
import com.ecetasci.hrmanagement.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExpenseDocumentService expenseDocumentService;


    @GetMapping("/employee/expenses")
    public List<ExpenseResponseDto> getEmployeeExpenses(@RequestParam Long employeeId) {
        return expenseService.getEmployeeExpenses(employeeId);
    }


    @PostMapping("/employee/create-expense")
    public ResponseEntity<ExpenseResponseDto> createExpense(@RequestParam Long employeeId,
                                                            @Valid @RequestBody ExpenseCreateRequest dto) {
        ExpenseResponseDto response = expenseService.createExpense(employeeId, dto);
        return ResponseEntity.ok(response);
    }

//expense-id ile çalışır
@PostMapping(value = "/employee/expenses/{expense-id}/documents", consumes = "multipart/form-data")
public ResponseEntity<ExpenseDocumentResponseDto> uploadDocument(
        @PathVariable("expense-id") Long expenseId,
        @RequestParam("file") MultipartFile file
) throws IOException {

    ExpenseDocument doc = expenseDocumentService.uploadDocument(expenseId, file);

    //  Manuel DTO mapping
    ExpenseDocumentResponseDto response = new ExpenseDocumentResponseDto(
            doc.getId(),
            doc.getFileName(),
            doc.getFilePath(),
            doc.getFileType(),
            doc.getExpense().getId() // sadece ID alıyoruz, recursion yok
    );

    return ResponseEntity.ok(response);
}



    @GetMapping("/employee/expenses/documents/{docId}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long docId) throws IOException {
        Resource resource = expenseDocumentService.downloadDocument(docId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }


    @GetMapping("/company/expenses")
    public List<ExpenseResponseDto> getCompanyExpenses(@RequestParam Long companyId) {
        return expenseService.getCompanyExpenses(companyId);
    }


    @PutMapping("/company/expenses/{id}/approve")
    public ResponseEntity<String> approveExpense(@PathVariable Long id) {
        expenseService.approveExpense(id);
        return ResponseEntity.ok("Expense approved successfully");
    }


    @PutMapping("/expenses/{id}/update-rejected")
    public ResponseEntity<ExpenseResponseDto> updateRejectedExpense(@PathVariable Long id,
                                                                    @RequestBody ExpenseCreateRequest dto) {
        return ResponseEntity.ok(expenseService.updateRejectedExpense(id, dto));
    }

}
