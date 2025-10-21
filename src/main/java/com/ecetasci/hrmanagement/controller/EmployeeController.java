package com.ecetasci.hrmanagement.controller;

import com.ecetasci.hrmanagement.dto.request.LeaveRequestDto;
import com.ecetasci.hrmanagement.dto.request.RejectAssetRequestDto;
import com.ecetasci.hrmanagement.dto.response.BaseResponse;
import com.ecetasci.hrmanagement.dto.response.EmployeeAssetResponseDto;
import com.ecetasci.hrmanagement.dto.response.ExpenseResponseDto;
import com.ecetasci.hrmanagement.dto.response.LeaveResponseDto;
import com.ecetasci.hrmanagement.mapper.LeaveMapper;
import com.ecetasci.hrmanagement.service.AssetService;
import com.ecetasci.hrmanagement.service.ExpenseService;
import com.ecetasci.hrmanagement.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeController {
    private final LeaveService leaveService;
    private final AssetService assetService;
    private final LeaveMapper leaveMapper;
    private final ExpenseService expenseService;

    // İzin talebi oluştur //mapper çalışcak mı dene
    @PostMapping("/leave-request")
    public ResponseEntity<LeaveResponseDto> createLeave(@RequestBody @Valid LeaveRequestDto dto) {
        var request = leaveService.leaveRequestCreate(dto);
        return ResponseEntity.ok(leaveMapper.toDto(request));
    }
    // Çalışanın kendi zimmetlerini getir
    @GetMapping("/list-assets")
    public ResponseEntity<BaseResponse<List<EmployeeAssetResponseDto>>> getEmployeeAssets(
            @RequestParam Long employeeId) {
        List<EmployeeAssetResponseDto> assignments = assetService.getEmployeeAssets(employeeId);
        return ResponseEntity.ok(BaseResponse.<List<EmployeeAssetResponseDto>>builder()
                .success(true)
                .code(200)
                .message("Employee assets retrieved successfully")
                .data(assignments)
                .build());
    }

    // Zimmeti onayla
    @PutMapping("/assets/{assignmentId}/confirm")
    public ResponseEntity<BaseResponse<EmployeeAssetResponseDto>> confirmAsset(
            @PathVariable Long assignmentId) {
        EmployeeAssetResponseDto updated = assetService.confirmEmployeeAsset(assignmentId);
        return ResponseEntity.ok(BaseResponse.<EmployeeAssetResponseDto>builder()
                .success(true)
                .code(200)
                .message("Asset confirmed")
                .data(updated)
                .build());
    }

    // Zimmeti reddet (employeeNote zorunlu)
    @PutMapping("/assets/{assignmentId}/reject")
    public ResponseEntity<BaseResponse<EmployeeAssetResponseDto>> rejectAsset(
            @PathVariable Long assignmentId,
            @RequestBody @Valid RejectAssetRequestDto dto) {
        EmployeeAssetResponseDto updated = assetService.rejectEmployeeAsset(assignmentId, dto);
        return ResponseEntity.ok(BaseResponse.<EmployeeAssetResponseDto>builder()
                .success(true)
                .code(200)
                .message("Asset rejected")
                .data(updated)
                .build());
    }

    @GetMapping("/expenses")
    public ResponseEntity<BaseResponse<List<ExpenseResponseDto>>> getExpenses(
            @RequestParam Long employeeId) {
        List<ExpenseResponseDto> employeeExpenses = expenseService.getEmployeeExpenses(employeeId);
        return ResponseEntity.ok(BaseResponse.<List<ExpenseResponseDto>>builder()
                .success(true)
                .code(200)
                .message("Employee expenses retrieved successfully")
                        .data(employeeExpenses)
                .build());
    }
}
