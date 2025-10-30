package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.ShiftRequestDto;
import com.ecetasci.hrmanagement.dto.response.ShiftResponseDto;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.Shift;
import com.ecetasci.hrmanagement.exceptions.BusinessException;
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;
import com.ecetasci.hrmanagement.mapper.ShiftMapper;
import com.ecetasci.hrmanagement.repository.CompanyRepository;
import com.ecetasci.hrmanagement.repository.EmployeeShiftRepository;
import com.ecetasci.hrmanagement.repository.ShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftServiceTest {

    @Mock private ShiftRepository shiftRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private ShiftMapper shiftMapper;
    @Mock private EmployeeShiftRepository employeeShiftRepository;

    @InjectMocks
    private ShiftService service;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setCompanyName("ACME");
    }

    @Test
    void getShiftsByCompany_mapsToDtos() {
        Shift s1 = new Shift(); s1.setId(10L); s1.setName("Sabah"); s1.setCompany(company);
        Shift s2 = new Shift(); s2.setId(11L); s2.setName("Akşam"); s2.setCompany(company);
        when(shiftRepository.findByCompanyId(1L)).thenReturn(List.of(s1, s2));
        when(shiftMapper.toDto(s1)).thenReturn(new ShiftResponseDto(10L, "Sabah", null, null, 1L, "ACME"));
        when(shiftMapper.toDto(s2)).thenReturn(new ShiftResponseDto(11L, "Akşam", null, null, 1L, "ACME"));

        List<ShiftResponseDto> list = service.getShiftsByCompany(1L);

        assertEquals(2, list.size());
        assertEquals(10L, list.get(0).id());
        assertEquals("Akşam", list.get(1).name());
        verify(shiftRepository).findByCompanyId(1L);
    }

    @Test
    void createShift_companyNotFound_throws() {
        ShiftRequestDto dto = new ShiftRequestDto("Sabah", LocalDateTime.of(2025,1,1,9,0), LocalDateTime.of(2025,1,1,17,0), 1L);
        Shift mapped = new Shift(); mapped.setName("Sabah"); mapped.setStartTime(LocalDateTime.of(2025,1,1,9,0)); mapped.setEndTime(LocalDateTime.of(2025,1,1,17,0));
        when(shiftMapper.toEntity(dto)).thenReturn(mapped);
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.createShift(dto));
        assertEquals("Company not found", ex.getMessage());
        verify(shiftRepository, never()).save(any());
    }

    @Test
    void createShift_overlap_throwsBusinessException() {
        ShiftRequestDto dto = new ShiftRequestDto("Sabah", LocalDateTime.of(2025,1,1,9,0), LocalDateTime.of(2025,1,1,12,0), 1L);
        Shift newShift = new Shift(); newShift.setName("Sabah"); newShift.setStartTime(LocalDateTime.of(2025,1,1,9,0)); newShift.setEndTime(LocalDateTime.of(2025,1,1,12,0));
        when(shiftMapper.toEntity(dto)).thenReturn(newShift);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        // existing 10:00-11:00 overlaps with 9-12
        Shift existing = new Shift(); existing.setId(100L); existing.setCompany(company); existing.setStartTime(LocalDateTime.of(2025,1,1,10,0)); existing.setEndTime(LocalDateTime.of(2025,1,1,11,0));
        when(shiftRepository.findByCompanyId(1L)).thenReturn(List.of(existing));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createShift(dto));
        assertEquals("Vardiya saatleri çakışıyor!", ex.getMessage());
        verify(shiftRepository, never()).save(any());
    }

    @Test
    void createShift_success_persistsAndReturnsDto() {
        ShiftRequestDto dto = new ShiftRequestDto("Sabah", LocalDateTime.of(2025,1,1,8,0), LocalDateTime.of(2025,1,1,12,0), 1L);
        Shift newShift = new Shift(); newShift.setName("Sabah"); newShift.setStartTime(LocalDateTime.of(2025,1,1,8,0)); newShift.setEndTime(LocalDateTime.of(2025,1,1,12,0));
        when(shiftMapper.toEntity(dto)).thenReturn(newShift);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(shiftRepository.findByCompanyId(1L)).thenReturn(List.of());
        Shift saved = new Shift(); saved.setId(200L); saved.setName("Sabah"); saved.setStartTime(LocalDateTime.of(2025,1,1,8,0)); saved.setEndTime(LocalDateTime.of(2025,1,1,12,0)); saved.setCompany(company);
        when(shiftRepository.save(any(Shift.class))).thenReturn(saved);
        when(shiftMapper.toDto(saved)).thenReturn(new ShiftResponseDto(200L, "Sabah", LocalDateTime.of(2025,1,1,8,0), LocalDateTime.of(2025,1,1,12,0), 1L, "ACME"));

        ShiftResponseDto res = service.createShift(dto);

        assertEquals(200L, res.id());
        assertEquals("Sabah", res.name());
        assertEquals(LocalDateTime.of(2025,1,1,8,0), res.startTime());
        verify(shiftRepository).save(any(Shift.class));
    }

    @Test
    void updateShift_shiftNotFound_throws() {
        when(shiftRepository.findById(9L)).thenReturn(Optional.empty());
        ShiftRequestDto dto = new ShiftRequestDto("Akşam", LocalDateTime.of(2025,1,1,13,0), LocalDateTime.of(2025,1,1,18,0), 1L);
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.updateShift(9L, dto));
        assertEquals("Shift not found", ex.getMessage());
    }

    @Test
    void updateShift_companyNotFound_throws() {
        Shift existing = new Shift(); existing.setId(5L); existing.setCompany(company);
        when(shiftRepository.findById(5L)).thenReturn(Optional.of(existing));
        ShiftRequestDto dto = new ShiftRequestDto("Akşam", LocalDateTime.of(2025,1,1,13,0), LocalDateTime.of(2025,1,1,18,0), 99L);
        when(shiftMapper.toEntity(dto)).thenReturn(new Shift());
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.updateShift(5L, dto));
        assertEquals("Company not found", ex.getMessage());
    }

    @Test
    void updateShift_overlap_throwsBusinessException() {
        Shift existing = new Shift(); existing.setId(5L); existing.setCompany(company);
        when(shiftRepository.findById(5L)).thenReturn(Optional.of(existing));
        ShiftRequestDto dto = new ShiftRequestDto("Akşam", LocalDateTime.of(2025,1,1,13,0), LocalDateTime.of(2025,1,1,18,0), 1L);
        Shift updated = new Shift(); updated.setName("Akşam"); updated.setStartTime(LocalDateTime.of(2025,1,1,13,0)); updated.setEndTime(LocalDateTime.of(2025,1,1,18,0));
        when(shiftMapper.toEntity(dto)).thenReturn(updated);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        // another shift 17:00-19:00 overlaps with 13:00-18:00; id != 5 so not filtered
        Shift other = new Shift(); other.setId(7L); other.setCompany(company); other.setStartTime(LocalDateTime.of(2025,1,1,17,0)); other.setEndTime(LocalDateTime.of(2025,1,1,19,0));
        when(shiftRepository.findByCompanyId(1L)).thenReturn(List.of(existing, other));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateShift(5L, dto));
        assertEquals("Vardiya saatleri çakışıyor!", ex.getMessage());
    }

    @Test
    void updateShift_success_savesAndReturnsDto() {
        Shift existing = new Shift(); existing.setId(5L); existing.setCompany(company);
        when(shiftRepository.findById(5L)).thenReturn(Optional.of(existing));
        ShiftRequestDto dto = new ShiftRequestDto("Gece", LocalDateTime.of(2025,1,1,22,0), LocalDateTime.of(2025,1,2,6,0), 1L);
        Shift updated = new Shift(); updated.setName("Gece"); updated.setStartTime(LocalDateTime.of(2025,1,1,22,0)); updated.setEndTime(LocalDateTime.of(2025,1,2,6,0));
        when(shiftMapper.toEntity(dto)).thenReturn(updated);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(shiftRepository.findByCompanyId(1L)).thenReturn(List.of(existing)); // only itself
        Shift saved = new Shift(); saved.setId(5L); saved.setName("Gece"); saved.setStartTime(LocalDateTime.of(2025,1,1,22,0)); saved.setEndTime(LocalDateTime.of(2025,1,2,6,0)); saved.setCompany(company);
        when(shiftRepository.save(any(Shift.class))).thenReturn(saved);
        when(shiftMapper.toDto(saved)).thenReturn(new ShiftResponseDto(5L, "Gece", LocalDateTime.of(2025,1,1,22,0), LocalDateTime.of(2025,1,2,6,0), 1L, "ACME"));

        ShiftResponseDto res = service.updateShift(5L, dto);

        assertEquals(5L, res.id());
        assertEquals("Gece", res.name());
        verify(shiftRepository).save(any(Shift.class));
    }

    @Test
    void deleteShift_notFound_throws() {
        when(shiftRepository.findById(3L)).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.deleteShift(3L));
        assertEquals("Shift not found", ex.getMessage());
        verify(employeeShiftRepository, never()).deleteByShiftId(anyLong());
        verify(shiftRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteShift_success_deletesDependenciesThenShift() {
        Shift shift = new Shift(); shift.setId(3L);
        when(shiftRepository.findById(3L)).thenReturn(Optional.of(shift));

        service.deleteShift(3L);

        verify(employeeShiftRepository).deleteByShiftId(3L);
        verify(shiftRepository).deleteById(3L);
    }
}

