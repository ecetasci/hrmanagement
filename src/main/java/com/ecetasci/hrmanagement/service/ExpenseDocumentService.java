package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.entity.Expense;
import com.ecetasci.hrmanagement.entity.ExpenseDocument;
import com.ecetasci.hrmanagement.repository.ExpenseDocumentRepository;
import com.ecetasci.hrmanagement.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseDocumentService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseDocumentRepository expenseDocumentRepository;

    @Value("${app.file.upload-dir:uploads/expenses}")
    private String uploadDir;

    /**
     * Tek belge yükleme
     */
    public ExpenseDocument uploadDocument(Long expenseId, MultipartFile file) throws IOException {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        // 1️⃣ Dosya tipi ve boyut validasyonu
        validateFile(file);

        // 2️⃣ Klasör oluşturma
        Path expenseFolder = Path.of(uploadDir, String.valueOf(expenseId));
        Files.createDirectories(expenseFolder);

        // 3️⃣ Dosyayı kaydet
        String fileName = file.getOriginalFilename();
        Path filePath = expenseFolder.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 4️⃣ Veritabanına belge ekle
        ExpenseDocument document = ExpenseDocument.builder()
                .expense(expense)
                .fileName(fileName)
                .filePath(filePath.toString())
                .fileType(file.getContentType())
                .uploadDate(LocalDate.now())
                .build();

        return expenseDocumentRepository.save(document);
    }

    /**
     * Çoklu belge yükleme
     */
    public List<ExpenseDocument> uploadDocuments(Long expenseId, List<MultipartFile> files) throws IOException {
        for (MultipartFile file : files) {
            uploadDocument(expenseId, file);
        }
        return expenseDocumentRepository.findAllByExpense_Id(expenseId);
    }

    /**
     * Belge indirme
     */
    public Resource downloadDocument(Long documentId) {
        ExpenseDocument document = expenseDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        Path filePath = Path.of(document.getFilePath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found on disk: " + filePath);
        }

        return new FileSystemResource(filePath.toFile());
    }

    /**
     * Belge listeleme (masraf bazında)
     */
    public List<ExpenseDocument> getDocumentsByExpense(Long expenseId) {
        return expenseDocumentRepository.findAllByExpense_Id(expenseId);
    }

    /**
     * Belge silme
     */
    public void deleteDocument(Long documentId) {
        ExpenseDocument document = expenseDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        try {
            Files.deleteIfExists(Path.of(document.getFilePath()));
        } catch (IOException e) {
            throw new RuntimeException("Could not delete file from disk", e);
        }

        expenseDocumentRepository.delete(document);
    }

    /**
     * 📋 Yardımcı metot: dosya doğrulama
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Empty file cannot be uploaded");
        }

        String fileType = file.getContentType();
        if (!(fileType.equals("image/jpeg") ||
                fileType.equals("image/png") ||
                fileType.equals("application/pdf"))) {
            throw new RuntimeException("Unsupported file type: " + fileType);
        }

        if (file.getSize() > 5 * 1024 * 1024) { // 5MB
            throw new RuntimeException("File size exceeds 5MB limit");
        }
    }
}
