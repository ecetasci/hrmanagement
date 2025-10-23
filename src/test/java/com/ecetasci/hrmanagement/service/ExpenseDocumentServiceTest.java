package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.entity.Expense;
import com.ecetasci.hrmanagement.entity.ExpenseDocument;
import com.ecetasci.hrmanagement.repository.ExpenseDocumentRepository;
import com.ecetasci.hrmanagement.repository.ExpenseRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private ExpenseDocumentRepository expenseDocumentRepository;

    @InjectMocks
    private ExpenseDocumentService service;

    private Path tempUploadDir;
    private Expense expense;

    @BeforeEach
    void setUp() throws IOException {
        tempUploadDir = Files.createTempDirectory("uploads-expense-test");
        // uploadDir alanını geçici klasöre yönlendir
        ReflectionTestUtils.setField(service, "uploadDir", tempUploadDir.toString());

        expense = new Expense();
        expense.setId(2L);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempUploadDir != null) {
            // Temizlik: oluşturulan tüm dosya/klasörleri sil
            if (Files.exists(tempUploadDir)) {
                Files.walk(tempUploadDir)
                        .sorted((p1, p2) -> p2.getNameCount() - p1.getNameCount())
                        .forEach(path -> {
                            try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                        });
            }
        }
    }

    @Test
    void uploadDocument_success_savesFileAndEntity() throws Exception {
        when(expenseRepository.findById(2L)).thenReturn(Optional.of(expense));
        when(expenseDocumentRepository.save(any(ExpenseDocument.class))).thenAnswer(inv -> {
            ExpenseDocument d = inv.getArgument(0);
            d.setId(111L);
            return d;
        });

        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.jpg", "image/jpeg", "hello".getBytes());

        ExpenseDocument doc = service.uploadDocument(2L, file);

        assertEquals(111L, doc.getId());
        assertEquals("receipt.jpg", doc.getFileName());
        assertEquals("image/jpeg", doc.getFileType());
        assertEquals(LocalDate.now(), doc.getUploadDate());
        assertNotNull(doc.getFilePath());
        assertTrue(Files.exists(Path.of(doc.getFilePath())));

        // Kaydedilen entity doğrulaması
        ArgumentCaptor<ExpenseDocument> captor = ArgumentCaptor.forClass(ExpenseDocument.class);
        verify(expenseDocumentRepository).save(captor.capture());
        ExpenseDocument saved = captor.getValue();
        assertEquals(expense, saved.getExpense());
        assertTrue(saved.getFilePath().endsWith("2\\receipt.jpg") || saved.getFilePath().endsWith("2/receipt.jpg"));
    }

    @Test
    void uploadDocument_expenseNotFound_throws() {
        when(expenseRepository.findById(9L)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1});
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.uploadDocument(9L, file));
        assertEquals("Expense not found", ex.getMessage());
        verify(expenseDocumentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_emptyFile_throws() {
        when(expenseRepository.findById(2L)).thenReturn(Optional.of(expense));
        MockMultipartFile empty = new MockMultipartFile("file", "e.jpg", "image/jpeg", new byte[]{});
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.uploadDocument(2L, empty));
        assertEquals("Empty file cannot be uploaded", ex.getMessage());
    }

    @Test
    void uploadDocument_unsupportedType_throws() {
        when(expenseRepository.findById(2L)).thenReturn(Optional.of(expense));
        MockMultipartFile file = new MockMultipartFile("file", "doc.txt", "text/plain", "x".getBytes());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.uploadDocument(2L, file));
        assertEquals("Unsupported file type: text/plain", ex.getMessage());
    }

    @Test
    void uploadDocument_tooLarge_throws() {
        when(expenseRepository.findById(2L)).thenReturn(Optional.of(expense));
        byte[] big = new byte[5 * 1024 * 1024 + 1]; // 5MB + 1 byte
        MockMultipartFile file = new MockMultipartFile("file", "b.jpg", "image/jpeg", big);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.uploadDocument(2L, file));
        assertEquals("File size exceeds 5MB limit", ex.getMessage());
    }

    @Test
    void uploadDocuments_success_returnsRepositoryList_andSavesEach() throws Exception {
        when(expenseRepository.findById(2L)).thenReturn(Optional.of(expense));
        when(expenseDocumentRepository.save(any(ExpenseDocument.class))).thenAnswer(inv -> inv.getArgument(0));
        when(expenseDocumentRepository.findAllByExpense_Id(2L)).thenReturn(List.of(
                doc(1L, "r1.jpg"), doc(2L, "r2.jpg")
        ));

        MockMultipartFile f1 = new MockMultipartFile("file", "r1.jpg", "image/jpeg", new byte[]{1});
        MockMultipartFile f2 = new MockMultipartFile("file", "r2.jpg", "image/jpeg", new byte[]{2});

        List<ExpenseDocument> list = service.uploadDocuments(2L, List.of(f1, f2));

        assertEquals(2, list.size());
        verify(expenseDocumentRepository, times(2)).save(any(ExpenseDocument.class));
        verify(expenseDocumentRepository).findAllByExpense_Id(2L);
    }

    @Test
    void downloadDocument_success_returnsResource() throws Exception {
        Path file = tempUploadDir.resolve("2").resolve("d.pdf");
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[]{1,2,3});

        ExpenseDocument d = new ExpenseDocument();
        d.setId(50L);
        d.setFilePath(file.toString());
        when(expenseDocumentRepository.findById(50L)).thenReturn(Optional.of(d));

        Resource res = service.downloadDocument(50L);
        assertTrue(res.exists());
        assertTrue(res.getFilename().endsWith("d.pdf"));
    }

    @Test
    void downloadDocument_notFound_throws() {
        when(expenseDocumentRepository.findById(77L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.downloadDocument(77L));
        assertEquals("Document not found", ex.getMessage());
    }

    @Test
    void downloadDocument_fileMissingOnDisk_throws() {
        ExpenseDocument d = new ExpenseDocument();
        d.setId(51L);
        d.setFilePath(tempUploadDir.resolve("2").resolve("missing.jpg").toString());
        when(expenseDocumentRepository.findById(51L)).thenReturn(Optional.of(d));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.downloadDocument(51L));
        assertTrue(ex.getMessage().startsWith("File not found on disk:"));
    }

    @Test
    void getDocumentsByExpense_delegatesToRepository() {
        List<ExpenseDocument> docs = List.of(doc(1L, "a.jpg"));
        when(expenseDocumentRepository.findAllByExpense_Id(2L)).thenReturn(docs);
        List<ExpenseDocument> res = service.getDocumentsByExpense(2L);
        assertEquals(docs, res);
        verify(expenseDocumentRepository).findAllByExpense_Id(2L);
    }

    @Test
    void deleteDocument_notFound_throws() {
        when(expenseDocumentRepository.findById(90L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.deleteDocument(90L));
        assertEquals("Document not found", ex.getMessage());
    }

    @Test
    void deleteDocument_success_deletesFileAndEntity() throws Exception {
        Path file = tempUploadDir.resolve("2").resolve("x.png");
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[]{5});

        ExpenseDocument d = new ExpenseDocument();
        d.setId(60L);
        d.setFilePath(file.toString());
        when(expenseDocumentRepository.findById(60L)).thenReturn(Optional.of(d));

        service.deleteDocument(60L);

        assertFalse(Files.exists(file));
        verify(expenseDocumentRepository).delete(d);
    }

    private ExpenseDocument doc(Long id, String filename) {
        ExpenseDocument d = new ExpenseDocument();
        d.setId(id);
        d.setFileName(filename);
        d.setUploadDate(LocalDate.now());
        return d;
    }
}

