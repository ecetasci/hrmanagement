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

/**
 * ShiftController — şirket vardiya (shift) yönetimi.
 *
 * Sağladığı işlevler:
 * - Vardiya oluşturma, güncelleme, listeleme ve silme
 * - Çalışanlara vardiya atama
 */
@RestController
@RequestMapping("api/company/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;
    private final EmployeeShiftService employeeShiftService;
    private final ShiftMapper shiftMapper;

    /**
     * Yeni bir vardiya oluşturur.
     *
     * @param dto ShiftRequestDto
     * @return Oluşturulan vardiya DTO
     */
    @PostMapping
    public ResponseEntity<ShiftResponseDto> createShift(@RequestBody @Valid ShiftRequestDto dto) {
        return ResponseEntity.ok(shiftService.createShift(dto));
    }

    /**
     * Varolan bir vardiyayı günceller.
     *
     * @param id Vardiya ID
     * @param dto Güncel vardiya verisi
     * @return Güncellenmiş vardiya DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<ShiftResponseDto> updateShift(@PathVariable Long id,
                                                        @RequestBody @Valid ShiftRequestDto dto) {
        return ResponseEntity.ok(shiftService.updateShift(id, dto));
    }

    /**
     * Şirkete ait tüm vardiyaları listeler.
     *
     * @param companyId Şirket ID'si
     * @return Vardiya DTO listesi
     */
    @GetMapping
    public ResponseEntity<List<ShiftResponseDto>> getAllShifts(@RequestParam Long companyId) {
        return ResponseEntity.ok(shiftService.getShiftsByCompany(companyId));
    }


    /**
     * Vardiyayı siler.
     *
     * @param id Vardiya ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShift(@PathVariable Long id) {
        shiftService.deleteShift(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Personel'e vardiya atar.
     *
     * @param employeeId Çalışan ID'si
     * @param dto Atama DTO
     * @return Atama sonucu (created id wrapped)
     */
    @PostMapping("/employees/{employeeId}/assign")
    public ResponseEntity<BaseResponse<Long>> assignShift(@PathVariable Long employeeId,
                                                                  @RequestBody @Valid EmployeeShiftAssignRequestDto dto) {

        EmployeeShift employeeShift = employeeShiftService.assignShift(employeeId, dto.shiftId(), dto.assignedDate());

        return ResponseEntity.ok(BaseResponse.<Long>builder().success(true).code(200).data(employeeShift.getId()).message("assigned").build());
    }
}
