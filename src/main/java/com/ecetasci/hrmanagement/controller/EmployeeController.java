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
import com.ecetasci.hrmanagement.utility.JwtManager;
import com.ecetasci.hrmanagement.repository.UserRepository;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.ecetasci.hrmanagement.constant.Endpoints.EMPLOYEE;

/**
 * EmployeeController — çalışanlara ait işlemler (izin, zimmet, gider vb.).
 *
 * Sağladığı işlevler:
 * - İzin talebi oluşturma
 * - Çalışanın kendi zimmetlerini listeleme
 * - Zimmet onay/reddetme
 * - Çalışanın giderlerini listeleme
 */
@RestController
@RequestMapping(EMPLOYEE)
@RequiredArgsConstructor
public class EmployeeController {
    private final LeaveService leaveService;
    private final AssetService assetService;
    private final LeaveMapper leaveMapper;
    private final ExpenseService expenseService;
    private final JwtManager jwtManager;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Yeni bir izin talebi oluşturur.
     *
     * @param dto LeaveRequestDto
     * @return Oluşturulan izin talebi DTO
     */
    @PostMapping("/leave-request")
    public ResponseEntity<LeaveResponseDto> createLeave(@RequestBody @Valid LeaveRequestDto dto) {
        var request = leaveService.leaveRequestCreate(dto);
        return ResponseEntity.ok(leaveMapper.toDto(request));
    }

    /**
     * Çalışanın zimmetlerini listeler. Employee id artık Authorization'dan çözülür.
     *
     * @return Zimmet atamaları listesi wrapped ile BaseResponse
     */
    @GetMapping("/list-assets")
    public ResponseEntity<BaseResponse<List<EmployeeAssetResponseDto>>> getEmployeeAssets(
            HttpServletRequest request) {
        Long employeeId = resolveCallerEmployeeId(request);
        if (employeeId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.<List<EmployeeAssetResponseDto>>builder()
                            .success(false)
                            .code(401)
                            .message("Unauthorized")
                            .build());
        }

        List<EmployeeAssetResponseDto> assignments = assetService.getEmployeeAssets(employeeId);
        return ResponseEntity.ok(BaseResponse.<List<EmployeeAssetResponseDto>>builder()
                .success(true)
                .code(200)
                .message("Employee assets retrieved successfully")
                .data(assignments)
                .build());
    }

    /**
     * Zimmeti onaylar.
     * Artık çağıran JWT'den çözülen employeeId yalnızca kendisine atanmış atamaları onaylayabilir.
     */
    @PutMapping("/assets/{assignmentId}/confirm")
    public ResponseEntity<BaseResponse<EmployeeAssetResponseDto>> confirmAsset(
            @PathVariable Long assignmentId,
            HttpServletRequest request) {
        Long employeeId = resolveCallerEmployeeId(request);
        if (employeeId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.<EmployeeAssetResponseDto>builder()
                            .success(false)
                            .code(401)
                            .message("Unauthorized")
                            .build());
        }

        try {
            if (!assetService.assignmentBelongsToEmployee(assignmentId, employeeId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(BaseResponse.<EmployeeAssetResponseDto>builder()
                                .success(false)
                                .code(403)
                                .message("You are not allowed to confirm this assignment")
                                .build());
            }
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.<EmployeeAssetResponseDto>builder()
                            .success(false)
                            .code(404)
                            .message("Assignment not found")
                            .build());
        }

        try {
            EmployeeAssetResponseDto updated = assetService.confirmEmployeeAsset(assignmentId);
            return ResponseEntity.ok(BaseResponse.<EmployeeAssetResponseDto>builder()
                    .success(true)
                    .code(200)
                    .message("Asset confirmed")
                    .data(updated)
                    .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.<EmployeeAssetResponseDto>builder()
                            .success(false)
                            .code(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Zimmeti reddeder. `employeeNote` zorunludur.
     * Artık çağıran sadece kendisine atanmış atamaları reddedebilir.
     */
    @PutMapping("/assets/{assignmentId}/reject")
    public ResponseEntity<BaseResponse<EmployeeAssetResponseDto>> rejectAsset(
            @PathVariable Long assignmentId,
            @RequestBody @Valid RejectAssetRequestDto dto,
            HttpServletRequest request) {
        Long employeeId = resolveCallerEmployeeId(request);
        if (employeeId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.<EmployeeAssetResponseDto>builder()
                            .success(false)
                            .code(401)
                            .message("Unauthorized")
                            .build());
        }

        try {
            if (!assetService.assignmentBelongsToEmployee(assignmentId, employeeId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(BaseResponse.<EmployeeAssetResponseDto>builder()
                                .success(false)
                                .code(403)
                                .message("You are not allowed to reject this assignment")
                                .build());
            }
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.<EmployeeAssetResponseDto>builder()
                            .success(false)
                            .code(404)
                            .message("Assignment not found")
                            .build());
        }

        try {
            EmployeeAssetResponseDto updated = assetService.rejectEmployeeAsset(assignmentId, dto);
            return ResponseEntity.ok(BaseResponse.<EmployeeAssetResponseDto>builder()
                    .success(true)
                    .code(200)
                    .message("Asset rejected")
                    .data(updated)
                    .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.<EmployeeAssetResponseDto>builder()
                            .success(false)
                            .code(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Çalışanın giderlerini getirir. Employee id artık Authorization'dan çözülür.
     *
     * @return Gider DTO listesi wrapped ile BaseResponse
     */
    @Deprecated
    @GetMapping("/expenses")
    public ResponseEntity<BaseResponse<List<ExpenseResponseDto>>> getExpenses(
            HttpServletRequest request) {
        Long employeeId = resolveCallerEmployeeId(request);
        if (employeeId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.<List<ExpenseResponseDto>>builder()
                            .success(false)
                            .code(401)
                            .message("Unauthorized")
                            .build());
        }

        List<ExpenseResponseDto> employeeExpenses = expenseService.getEmployeeExpenses(employeeId);
        return ResponseEntity.ok(BaseResponse.<List<ExpenseResponseDto>>builder()
                .success(true)
                .code(200)
                .message("Employee expenses retrieved successfully")
                .data(employeeExpenses)
                .build());
    }

    // helper to resolve caller's employee id from Authorization header (JWT)
    private Long resolveCallerEmployeeId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        String token = auth.substring(7);
        String username;
        try {
            username = jwtManager.extractUsername(token);
        } catch (Exception e) {
            return null;
        }
        if (username == null) return null;
        var userOpt = userRepository.findUserByEmail(username);
        if (userOpt.isEmpty()) return null;
        var user = userOpt.get();
        var empOpt = employeeRepository.findByUserId(user.getId());
        return empOpt.map(emp -> emp.getId()).orElse(null);
    }

}
