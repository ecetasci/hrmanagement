package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;
import com.ecetasci.hrmanagement.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyService companyService;

    @Test
    void findById_whenExists_returnsCompany() {
        Company company = new Company();
        company.setId(1L);
        company.setCompanyName("ACME");
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        Company result = companyService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ACME", result.getCompanyName());
        verify(companyRepository, times(1)).findById(1L);
    }

    @Test
    void findById_whenNotExists_throwsResourceNotFound() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> companyService.findById(99L));
        assertEquals("Company bulunamadı", ex.getMessage());
        verify(companyRepository, times(1)).findById(99L);
    }
}

