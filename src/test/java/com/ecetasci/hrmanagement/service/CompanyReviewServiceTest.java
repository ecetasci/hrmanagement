package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.response.CompanyReviewResponse;
import com.ecetasci.hrmanagement.dto.response.ReviewDetailResponse;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.CompanyReview;
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;
import com.ecetasci.hrmanagement.repository.CompanyRepository;
import com.ecetasci.hrmanagement.repository.CompanyReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyReviewServiceTest {

    @Mock
    private CompanyReviewRepository companyReviewRepository;
    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyReviewService service;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setCompanyName("ACME");
    }

    @Test
    void findAll_returnsMappedResponses() {
        CompanyReview r1 = buildReview(10L, company, "T1", "C1", 4);
        CompanyReview r2 = buildReview(11L, company, "T2", "C2", 5);
        when(companyReviewRepository.findAll()).thenReturn(List.of(r1, r2));

        List<CompanyReviewResponse> list = service.findAll();

        assertEquals(2, list.size());
        assertEquals("ACME", list.get(0).companyName());
        assertEquals("T1", list.get(0).title());
        assertEquals("C2", list.get(1).content());
        assertEquals(5, list.get(1).rating());
        verify(companyReviewRepository).findAll();
    }

    @Test
    void isPublished_zero_false() {
        when(companyReviewRepository.countCompanyReviewByCompany_Id(1L)).thenReturn(0);
        assertFalse(service.isPublished(1L));
    }

    @Test
    void isPublished_negative_false() {
        when(companyReviewRepository.countCompanyReviewByCompany_Id(1L)).thenReturn(-1);
        assertFalse(service.isPublished(1L));
    }

    @Test
    void isPublished_positive_true() {
        when(companyReviewRepository.countCompanyReviewByCompany_Id(1L)).thenReturn(2);
        assertTrue(service.isPublished(1L));
    }

    @Test
    void createCompanyReview_success_whenNotPublished() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyReviewRepository.countCompanyReviewByCompany_Id(1L)).thenReturn(0); // isPublished -> false
        when(companyReviewRepository.save(any(CompanyReview.class))).thenAnswer(inv -> {
            CompanyReview cr = inv.getArgument(0);
            cr.setId(99L);
            return cr;
        });

        CompanyReviewResponse res = service.createCompanyReview(1L, "Title", "Content", 5);

        assertEquals("ACME", res.companyName());
        assertEquals("Title", res.title());
        assertEquals("Content", res.content());
        assertEquals(5, res.rating());

        ArgumentCaptor<CompanyReview> captor = ArgumentCaptor.forClass(CompanyReview.class);
        verify(companyReviewRepository).save(captor.capture());
        CompanyReview saved = captor.getValue();
        assertEquals(company, saved.getCompany());
        assertEquals("Title", saved.getTitle());
        assertEquals("Content", saved.getContent());
        assertEquals(5, saved.getRating());
    }

    @Test
    void createCompanyReview_whenAlreadyPublished_throws() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyReviewRepository.countCompanyReviewByCompany_Id(1L)).thenReturn(1);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.createCompanyReview(1L, "T", "C", 3));
        assertEquals("Daha önce yorum yapıldığından yorum eklenemez", ex.getMessage());
        verify(companyReviewRepository, never()).save(any());
    }

    @Test
    void createCompanyReview_companyNotFound_throws() {
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.createCompanyReview(1L, "T", "C", 3));
        verify(companyReviewRepository, never()).save(any());
    }

    @Test
    void deleteWithId_invokesRepository() {
        service.deleteWithId(7L);
        verify(companyReviewRepository).deleteCompanyReviewByCompany_Id(7L);
    }

    @Test
    void findByCompanyId_success() {
        CompanyReview review = buildReview(5L, company, "T", "C", 4);
        when(companyReviewRepository.findCompanyReviewByCompany_Id(1L)).thenReturn(Optional.of(review));

        CompanyReview result = service.findByCompanyId(1L);
        assertEquals(review, result);
    }

    @Test
    void findByCompanyId_notFound_throws() {
        when(companyReviewRepository.findCompanyReviewByCompany_Id(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findByCompanyId(1L));
    }

    @Test
    void save_delegatesToRepository() {
        CompanyReview review = buildReview(5L, company, "T", "C", 4);
        when(companyReviewRepository.save(review)).thenReturn(review);

        CompanyReview result = service.save(review);
        assertEquals(review, result);
        verify(companyReviewRepository).save(review);
    }

    @Test
    void findById_success_returnsDetailResponse() {
        CompanyReview review = buildReview(44L, company, "T", "C", 2);
        when(companyReviewRepository.findById(44L)).thenReturn(Optional.of(review));

        ReviewDetailResponse res = service.findById(44L);

        assertEquals("ACME", res.companyName());
        assertEquals("T", res.title());
        assertEquals("C", res.content());
        assertEquals(2, res.rating());
    }

    @Test
    void findById_notFound_throws() {
        when(companyReviewRepository.findById(44L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(44L));
    }

    // helpers
    private CompanyReview buildReview(Long id, Company company, String title, String content, Integer rating) {
        CompanyReview r = new CompanyReview();
        r.setId(id);
        r.setCompany(company);
        r.setTitle(title);
        r.setContent(content);
        r.setRating(rating);
        return r;
    }
}

