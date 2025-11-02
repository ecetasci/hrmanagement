package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.BreakRequestDto;
import com.ecetasci.hrmanagement.dto.response.BreakResponseDto;
import com.ecetasci.hrmanagement.entity.Break;
import com.ecetasci.hrmanagement.entity.Shift;
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;
import com.ecetasci.hrmanagement.repository.BreakRepository;
import com.ecetasci.hrmanagement.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BreakService {

    private final BreakRepository breakRepository;
    private final ShiftRepository shiftRepository;

    public BreakResponseDto createBreak(BreakRequestDto dto) {
        Shift shift = shiftRepository.findById(dto.shiftId())
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found"));

        // validation: Mola shifft içinde olmalı
        if (dto.startTime().isBefore(shift.getStartTime()) || dto.endTime().isAfter(shift.getEndTime())) {
            throw new IllegalArgumentException("Break times must be within shift start and end times");
        }// validation: endTime startTime'dan sonra mı
        if (!dto.endTime().isAfter(dto.startTime())) {
            throw new IllegalArgumentException("Break endTime must be after startTime");
        }
// validation: duration doğru mu
        int minutes = (int) Duration.between(dto.startTime(), dto.endTime()).toMinutes();
        Integer duration = dto.duration();
        if (duration == null) duration = minutes;// eğer duration null ise otomatik olarak hesaplanan değeri ata
        if (duration != minutes) {
            throw new IllegalArgumentException("Provided duration does not match start/end times");
        }

        Break b = Break.builder()
                .name(dto.name())
                .startTime(dto.startTime())
                .endTime(dto.endTime())
                .duration(duration)
                .shift(shift)
                .build();

        Break saved = breakRepository.save(b);
        return toDto(saved);
    }

    public BreakResponseDto updateBreak(Long id, BreakRequestDto dto) {
        Break existing = breakRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Break not found"));

        Shift shift = shiftRepository.findById(dto.shiftId())
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found"));

        if (dto.startTime().isBefore(shift.getStartTime()) || dto.endTime().isAfter(shift.getEndTime())) {
            throw new IllegalArgumentException("Break times must be within shift start and end times");
        }
        if (!dto.endTime().isAfter(dto.startTime())) {
            throw new IllegalArgumentException("Break endTime must be after startTime");
        }

        int minutes = (int) Duration.between(dto.startTime(), dto.endTime()).toMinutes();
        Integer duration = dto.duration();
        if (duration == null) duration = minutes;
        if (duration != minutes) {
            throw new IllegalArgumentException("Provided duration does not match start/end times");
        }

        existing.setName(dto.name());
        existing.setStartTime(dto.startTime());
        existing.setEndTime(dto.endTime());
        existing.setDuration(duration);
        existing.setShift(shift);

        Break saved = breakRepository.save(existing);
        return toDto(saved);
    }

    public void deleteBreak(Long id) {
        if (!breakRepository.existsById(id)) throw new ResourceNotFoundException("Break not found");
        breakRepository.deleteById(id);
    }

    public BreakResponseDto getBreak(Long id) {
        Break b = breakRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Break not found"));
        return toDto(b);
    }

    public List<BreakResponseDto> getBreaksByShift(Long shiftId) {
        return breakRepository.findByShiftId(shiftId).stream().map(this::toDto).collect(Collectors.toList());
    }

    private BreakResponseDto toDto(Break b) {
        return new BreakResponseDto(b.getId(), b.getName(), b.getStartTime(), b.getEndTime(), b.getDuration(), b.getShift().getId());
    }
}

