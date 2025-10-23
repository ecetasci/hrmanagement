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

/**
 * ExpenseController — gider (expense) ve gider dokümanları işlemleri.
 *
 * Sağladığı işlevler:
 * - Çalışan ve şirket giderlerini listeleme
 * - Gider oluşturma
 * - Gider dökümanları yükleme / indirme
 * - Gider onay/reddetme ve reddedilen gider güncelleme
 */
@RestController
@RequestMapping("api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExpenseDocumentService expenseDocumentService;


    /**
     * Belirtilen çalışanın giderlerini listeler.
     *
     * @param employeeId Çalışan ID'si
     * @return ExpenseResponseDto listesi
     */
    @GetMapping("/employee/expenses")
    public List<ExpenseResponseDto> getEmployeeExpenses(@RequestParam Long employeeId) {
        return expenseService.getEmployeeExpenses(employeeId);
    }


    /**
     * Yeni gider oluşturur.
     *
     * @param employeeId Çalışan ID'si
     * @param dto Gider oluşturma DTO
     * @return Oluşturulan gider DTO
     */
    @PostMapping("/employee/create-expense")
    public ResponseEntity<ExpenseResponseDto> createExpense(@RequestParam Long employeeId,
                                                            @Valid @RequestBody ExpenseCreateRequest dto) {
        ExpenseResponseDto response = expenseService.createExpense(employeeId, dto);
        return ResponseEntity.ok(response);
    }

    /**
     * Gider için doküman (fatura vb.) yükler.
     *
     * @param expenseId Gider ID'si
     * @param file Yüklenecek dosya (multipart/form-data)
     * @return Yüklenen doküman bilgileri
     */
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


    /**
     * Gider dokümanını indirir.
     *
     * @param docId Doküman ID'si
     * @return Dosya kaynağı (download)
     */
    @GetMapping("/employee/expenses/documents/{docId}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long docId) throws IOException {
        Resource resource = expenseDocumentService.downloadDocument(docId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }


    /**
     * Şirkete ait giderleri listeler.
     *
     * @param companyId Şirket ID'si
     * @return ExpenseResponseDto listesi
     */
    @GetMapping("/company/expenses")
    public List<ExpenseResponseDto> getCompanyExpenses(@RequestParam Long companyId) {
        return expenseService.getCompanyExpenses(companyId);
    }


    /**
     * Gideri onaylar (company tarafı).
     *
     * @param id Gider ID'si
     * @return Başarı mesajı
     */
    @PutMapping("/company/expenses/{id}/approve")
    public ResponseEntity<String> approveExpense(@PathVariable Long id) {
        expenseService.approveExpense(id);
        return ResponseEntity.ok("Expense approved successfully");
    }


    /**
     * Reddedilmiş gideri günceller.
     *
     * @param id Gider ID'si
     * @param dto Yeni gider verisi
     * @return Güncellenmiş gider DTO
     */
    @PutMapping("/expenses/{id}/update-rejected")
    public ResponseEntity<ExpenseResponseDto> updateRejectedExpense(@PathVariable Long id,
                                                                    @RequestBody ExpenseCreateRequest dto) {
        return ResponseEntity.ok(expenseService.updateRejectedExpense(id, dto));
    }

    /**
     * Gideri reddeder.
     *
     * @param id Gider ID'si
     * @return Başarı mesajı
     */
    @PutMapping("/expenses/{id}/reject")
    public ResponseEntity<String> rejectExpense(@PathVariable Long id) {
        expenseService.rejectExpense(id);
        return ResponseEntity.ok("Expense rejected successfully");
    }



}
