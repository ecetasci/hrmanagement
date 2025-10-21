// ShiftController.java
package com.ecetasci.hrmanagement.controller;

import com.ecetasci.hrmanagement.dto.request.EmployeeShiftAssignRequestDto;
import com.ecetasci.hrmanagement.dto.request.ShiftRequestDto;
import com.ecetasci.hrmanagement.dto.response.BaseResponse;
import com.ecetasci.hrmanagement.dto.response.ShiftResponseDto;
import com.ecetasci.hrmanagement.entity.EmployeeShift;
import com.ecetasci.hrmanagement.mapper.ShiftMapper;
import com.ecetasci.hrmanagement.service.EmployeeShiftService;
import com.ecetasci.hrmanagement.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/company/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;
    private final EmployeeShiftService employeeShiftService;
    private final ShiftMapper shiftMapper;

    @PostMapping
    public ResponseEntity<ShiftResponseDto> createShift(@RequestBody @Valid ShiftRequestDto dto) {
        return ResponseEntity.ok(shiftService.createShift(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShiftResponseDto> updateShift(@PathVariable Long id,
                                                        @RequestBody @Valid ShiftRequestDto dto) {
        return ResponseEntity.ok(shiftService.updateShift(id, dto));
    }

    @GetMapping
    public ResponseEntity<List<ShiftResponseDto>> getAllShifts(@RequestParam Long companyId) {
        return ResponseEntity.ok(shiftService.getShiftsByCompany(companyId));
    }


    // Vardiya sil
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShift(@PathVariable Long id) {
        shiftService.deleteShift(id);
        return ResponseEntity.noContent().build();
    }

    // Personel’e vardiya ata
    @PostMapping("/employees/{employeeId}/assign")
    public ResponseEntity<BaseResponse<Long>> assignShift(@PathVariable Long employeeId,
                                                                  @RequestBody @Valid EmployeeShiftAssignRequestDto dto) {

        EmployeeShift employeeShift = employeeShiftService.assignShift(employeeId, dto.shiftId(), dto.assignedDate());

        return ResponseEntity.ok(BaseResponse.<Long>builder().success(true).code(200).data(employeeShift.getId()).message("assigned").build());
    }
}
