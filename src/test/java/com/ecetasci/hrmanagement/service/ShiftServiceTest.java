package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.ShiftRequestDto;
import com.ecetasci.hrmanagement.dto.response.ShiftResponseDto;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.Shift;
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;
import com.ecetasci.hrmanagement.mapper.ShiftMapper;
import com.ecetasci.hrmanagement.repository.CompanyRepository;
import com.ecetasci.hrmanagement.repository.ShiftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftServiceTest {

    @Mock
    private ShiftRepository shiftRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private ShiftMapper shiftMapper;

    @InjectMocks
    private ShiftService service;

    @Test
    void getShiftsByCompany_returnsDtos() {
        Company c = new Company(); c.setId(1L);
        Shift s = new Shift(); s.setId(2L); s.setName("S1"); s.setCompany(c);
        when(shiftRepository.findByCompanyId(1L)).thenReturn(List.of(s));
        ShiftResponseDto dto = new ShiftResponseDto(2L, "S1", LocalTime.NOON, LocalTime.NOON.plusHours(8), 1L, "C1");
        when(shiftMapper.toDto(s)).thenReturn(dto);

        var list = service.getShiftsByCompany(1L);

        assertEquals(1, list.size());
        assertEquals("S1", list.get(0).name());
        verify(shiftRepository).findByCompanyId(1L);
    }

    @Test
    void createShift_companyNotFound_throws() {
        ShiftRequestDto req = new ShiftRequestDto("n", LocalTime.NOON, LocalTime.NOON.plusHours(8), 99L);
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.createShift(req));
    }
}
