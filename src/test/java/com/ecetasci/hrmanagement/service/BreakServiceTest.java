package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.BreakRequestDto;
import com.ecetasci.hrmanagement.entity.Break;
import com.ecetasci.hrmanagement.entity.Shift;
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;
import com.ecetasci.hrmanagement.repository.BreakRepository;
import com.ecetasci.hrmanagement.repository.ShiftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BreakServiceTest {

    @Mock private BreakRepository breakRepository;
    @Mock private ShiftRepository shiftRepository;

    @InjectMocks private BreakService service;

    @Test
    void createBreak_success() {
        Shift shift = new Shift(); shift.setId(1L); shift.setStartTime(LocalTime.of(9,0)); shift.setEndTime(LocalTime.of(17,0));
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift));
        when(breakRepository.save(any(Break.class))).thenAnswer(inv -> inv.getArgument(0));

        BreakRequestDto dto = new BreakRequestDto("Lunch", LocalTime.of(12,0), LocalTime.of(12,30), null, 1L);
        var res = service.createBreak(dto);

        assertEquals("Lunch", res.name());
        assertEquals(30, res.duration());
        verify(breakRepository).save(any(Break.class));
    }

    @Test
    void createBreak_shiftNotFound_throws() {
        when(shiftRepository.findById(99L)).thenReturn(Optional.empty());
        BreakRequestDto dto = new BreakRequestDto("B", LocalTime.of(9,0), LocalTime.of(9,15), 15, 99L);
        assertThrows(ResourceNotFoundException.class, () -> service.createBreak(dto));
    }
}

