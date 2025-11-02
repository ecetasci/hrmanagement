package com.ecetasci.hrmanagement.controller;

import com.ecetasci.hrmanagement.dto.request.BreakRequestDto;
import com.ecetasci.hrmanagement.dto.response.BreakResponseDto;
import com.ecetasci.hrmanagement.dto.response.BaseResponse;
import com.ecetasci.hrmanagement.service.BreakService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.ecetasci.hrmanagement.constant.Endpoints.COMPANY_BREAKS;

@RestController
@RequestMapping(COMPANY_BREAKS)
@RequiredArgsConstructor
public class BreakController {

    private final BreakService breakService;

    @PostMapping
    public ResponseEntity<BreakResponseDto> createBreak(@RequestBody @Valid BreakRequestDto dto) {
        return ResponseEntity.ok(breakService.createBreak(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BreakResponseDto> updateBreak(@PathVariable Long id, @RequestBody @Valid BreakRequestDto dto) {
        return ResponseEntity.ok(breakService.updateBreak(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BreakResponseDto> getBreak(@PathVariable Long id) {
        return ResponseEntity.ok(breakService.getBreak(id));
    }

    @GetMapping
    public ResponseEntity<List<BreakResponseDto>> getBreaksByShift(@RequestParam Long shiftId) {
        return ResponseEntity.ok(breakService.getBreaksByShift(shiftId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteBreak(@PathVariable Long id) {
        breakService.deleteBreak(id);
        return ResponseEntity.ok(BaseResponse.<Void>builder().success(true).code(200).message("deleted").build());
    }
}

