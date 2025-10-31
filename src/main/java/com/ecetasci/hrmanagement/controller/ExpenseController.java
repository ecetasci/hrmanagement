package com.ecetasci.hrmanagement.controller;

import com.ecetasci.hrmanagement.dto.request.ExpenseCreateRequest;
import com.ecetasci.hrmanagement.dto.response.ExpenseDocumentResponseDto;
import com.ecetasci.hrmanagement.dto.response.ExpenseResponseDto;
import com.ecetasci.hrmanagement.entity.ExpenseDocument;
import com.ecetasci.hrmanagement.service.ExpenseDocumentService;
import com.ecetasci.hrmanagement.service.ExpenseService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


import static com.ecetasci.hrmanagement.constant.Endpoints.EXPENSES;

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
@RequestMapping(EXPENSES)
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExpenseDocumentService expenseDocumentService;
    private final com.ecetasci.hrmanagement.utility.JwtManager jwtManager;
    private final com.ecetasci.hrmanagement.repository.UserRepository userRepository;
    private final com.ecetasci.hrmanagement.repository.EmployeeRepository employeeRepository;


    /**
     * Belirtilen çalışanın giderlerini listeler.
     *
     * İstek yapan kullanıcının employee id'si (Authorization header'dan) kullanılarak
     * kendi giderleri döndürülür. Eğer token geçersizse veya kullanıcı bulunamazsa 403 döner.
     *
     * @return ExpenseResponseDto listesi
     */
    @GetMapping("/employee/expenses")
    public ResponseEntity<com.ecetasci.hrmanagement.dto.response.BaseResponse<List<ExpenseResponseDto>>> getEmployeeExpenses(HttpServletRequest request) {
        Long callerEmployeeId = resolveCallerEmployeeId(request);// helper method
        if (callerEmployeeId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(com.ecetasci.hrmanagement.dto.response.BaseResponse.<List<ExpenseResponseDto>>builder()
                            .success(false)
                            .code(403)
                            .message("Access denied")
                            .build());
        }

        List<ExpenseResponseDto> employeeExpenses = expenseService.getEmployeeExpenses(callerEmployeeId);
        return ResponseEntity.ok(com.ecetasci.hrmanagement.dto.response.BaseResponse.<List<ExpenseResponseDto>>builder()
                .success(true)
                .code(200)
                .message("Employee expenses retrieved")
                .data(employeeExpenses)
                .build());
    }

    // Resolve caller employee id from Authorization header using JwtManager and repositories
    private Long resolveCallerEmployeeId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null) {
            return null;
        }
        if (!auth.startsWith("Bearer ")) {
            return null;
        }

        String token = auth.substring(7);
        String username;
        try {
            username = jwtManager.extractUsername(token);
        } catch (Exception e) {
            return null;
        }

        if (username == null) {
            return null;
        }

        java.util.Optional<com.ecetasci.hrmanagement.entity.User> userOpt = userRepository.findUserByEmail(username);
        if (userOpt.isEmpty()) {
            return null;
        }

        com.ecetasci.hrmanagement.entity.User user = userOpt.get();
        java.util.Optional<com.ecetasci.hrmanagement.entity.Employee> empOpt = employeeRepository.findByUserId(user.getId());
        if (empOpt.isEmpty()) {
            return null;
        }

        com.ecetasci.hrmanagement.entity.Employee emp = empOpt.get();
        return emp.getId();
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
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long docId) {
        Resource resource = expenseDocumentService.downloadDocument(docId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }


    /**
     * Şirkete ait giderleri listeler.
     *
     * @return ExpenseResponseDto listesi
     */
    @GetMapping("/company/expenses")
    public ResponseEntity<com.ecetasci.hrmanagement.dto.response.BaseResponse<List<ExpenseResponseDto>>> getCompanyExpenses(HttpServletRequest request) {
        Long callerCompanyId = resolveCallerCompanyId(request);
        if (callerCompanyId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(com.ecetasci.hrmanagement.dto.response.BaseResponse.<List<ExpenseResponseDto>>builder()
                            .success(false)
                            .code(403)
                            .message("Access denied")
                            .build());
        }

        List<ExpenseResponseDto> expenses = expenseService.getCompanyExpenses(callerCompanyId);
        return ResponseEntity.ok(com.ecetasci.hrmanagement.dto.response.BaseResponse.<List<ExpenseResponseDto>>builder()
                .success(true)
                .code(200)
                .message("Company expenses retrieved")
                .data(expenses)
                .build());
    }

    // Resolve caller company id from Authorization header using JwtManager and repositories
    private Long resolveCallerCompanyId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization"); // Bearer token
        if (auth == null) {
            return null; // no auth header
        }
        if (!auth.startsWith("Bearer ")) {
            return null; // invalid format
        }

        String token = auth.substring(7); // extract token after 'Bearer '

        String username;
        try {
            username = jwtManager.extractUsername(token);
        } catch (Exception e) {
            // token parsing failed
            return null;
        }

        if (username == null) {
            return null;
        }

        java.util.Optional<com.ecetasci.hrmanagement.entity.User> userOpt = userRepository.findUserByEmail(username);
        if (userOpt.isEmpty()) {
            return null; // no user found for username
        }

        com.ecetasci.hrmanagement.entity.User user = userOpt.get();
        java.util.Optional<com.ecetasci.hrmanagement.entity.Employee> empOpt = employeeRepository.findByUserId(user.getId());
        if (empOpt.isEmpty()) {
            return null; // no employee associated with this user
        }

        com.ecetasci.hrmanagement.entity.Employee emp = empOpt.get();
        if (emp.getCompany() == null) {
            return null; // employee has no company
        }

        return emp.getCompany().getId();
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
    @PutMapping("/{id}/update-rejected")
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
    @PutMapping("/{id}/reject")
    public ResponseEntity<String> rejectExpense(@PathVariable Long id) {
        expenseService.rejectExpense(id);
        return ResponseEntity.ok("Expense rejected successfully");
    }



}
