package com.ecetasci.hrmanagement.controller;


import com.ecetasci.hrmanagement.dto.request.AssetRequestDto;
import com.ecetasci.hrmanagement.dto.request.AssignAssetRequestDto;
import com.ecetasci.hrmanagement.dto.request.RegisterEmployeeRequestDto;
import com.ecetasci.hrmanagement.dto.response.AssetResponseDto;
import com.ecetasci.hrmanagement.dto.response.BaseResponse;
import com.ecetasci.hrmanagement.dto.response.EmployeeAssetResponseDto;
import com.ecetasci.hrmanagement.dto.response.ExpenseResponseDto;
import com.ecetasci.hrmanagement.dto.response.EmployeeResponseDto;
import com.ecetasci.hrmanagement.dto.response.LeaveResponseDto;
import com.ecetasci.hrmanagement.dto.response.LeaveTypeResponseDto;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.Expense;
import com.ecetasci.hrmanagement.entity.LeaveRequest;
import com.ecetasci.hrmanagement.entity.LeaveType;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import com.ecetasci.hrmanagement.repository.LeaveTypeRepository;
import com.ecetasci.hrmanagement.service.AssetService;
import com.ecetasci.hrmanagement.service.CompanyManagerService;
import com.ecetasci.hrmanagement.service.ExpenseService;
import com.ecetasci.hrmanagement.service.LeaveService;
import com.ecetasci.hrmanagement.mapper.LeaveMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.List;



@RestController
@RequiredArgsConstructor
@RequestMapping("api/manager")
public class CompanyManagerController {
    private final CompanyManagerService companyManagerService;
    private final LeaveService leaveService;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final AssetService assetService;
    private final ExpenseService expenseService;
    private final LeaveMapper leaveMapper; // added for mapping


    //Personel kayıt//
    @PostMapping("/employee-register")
    public ResponseEntity<BaseResponse<EmployeeResponseDto>> register(@RequestBody @Valid RegisterEmployeeRequestDto dto) {
        try {
            Employee saved = companyManagerService.createEmployee(dto);

            EmployeeResponseDto resp = new EmployeeResponseDto(
                    saved.getId(),
                    saved.getEmployeeNumber(),
                    saved.getName(),
                    saved.getEmail(),
                    saved.getPosition(),
                    saved.getDepartment()
            );

            return ResponseEntity.ok(BaseResponse.<EmployeeResponseDto>builder()
                    .success(true)
                    .code(201)
                    .message("Registration successful")
                    .data(resp)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(BaseResponse.<EmployeeResponseDto>builder()
                    .success(false)
                    .code(400)
                    .message("Registration failed: " + e.getMessage())
                    .build());
        }
    }

    // Şirket izin türleri listele
    @GetMapping("/leave-types")
    public ResponseEntity<List<LeaveTypeResponseDto>> getAllLeaveTypes() {
        List<LeaveTypeResponseDto> dtos = leaveTypeRepository.findAll().stream()
                .map(lt -> new LeaveTypeResponseDto(
                        lt.getId(),
                        lt.getName(),
                        lt.getDescription(),
                        lt.getMaxDays(),
                        lt.isPaid(),
                        lt.getCompany() != null ? lt.getCompany().getId() : null
                ))
                .toList();
        return ResponseEntity.ok(dtos);
    }


    // Tüm izin taleplerini listele
    @GetMapping("/leaves")
    public ResponseEntity<List<LeaveResponseDto>> getAllLeaves() {
        List<LeaveRequest> allLeaves = employeeRepository.findAll().stream()
                .flatMap(emp -> emp.getLeaveRequests().stream())
                .toList();

        List<LeaveResponseDto> dtos = allLeaves.stream()
                .map(leaveMapper::toDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    // İzin onayla//leave response dönelim
    @PutMapping("/leaves/{id}/approve")
    public ResponseEntity<String> approveLeave(@RequestParam String employeeNumber,
                                               @RequestParam LocalDate leaveStartDate,
                                               @RequestParam String managerEmployeeNumber
                                               ) {
        Employee manager = employeeRepository.findByEmployeeNumber(employeeNumber)
                .orElseThrow(() -> new RuntimeException("Manager not found"));
        leaveService.approveLeaveRequest(employeeNumber,leaveStartDate, managerEmployeeNumber);
        return ResponseEntity.ok("Leave approved");
    }

    // İzin reddet
    @PutMapping("/leaves/{id}/reject")
    public ResponseEntity<String> rejectLeave(@RequestParam String employeeNumber,
                                              @RequestParam String managerNumber,
                                              @RequestParam String managerNote) {
        Employee manager = employeeRepository.findByEmployeeNumber(managerNumber)
                .orElseThrow(() -> new RuntimeException("Manager not found"));
        leaveService.rejectLeaveRequestByEmployeeNumber(employeeNumber, managerNumber, managerNote);
        return ResponseEntity.ok("Leave rejected");
    }


    // Tüm zimmetleri listele
    @GetMapping("/assets")
    public ResponseEntity<BaseResponse<List<AssetResponseDto>>> getAllAssets() {
        List<AssetResponseDto> assets = assetService.getAllAssets();
        return ResponseEntity.ok(BaseResponse.<List<AssetResponseDto>>builder()
                .success(true)
                .code(200)
                .message("Assets retrieved successfully")
                .data(assets)
                .build());
    }

    // Yeni zimmet tanımla
    @PostMapping("/assets")
    public ResponseEntity<BaseResponse<AssetResponseDto>> createAsset(@RequestBody @Valid AssetRequestDto dto) {
        AssetResponseDto asset = assetService.createAsset(dto);
        return ResponseEntity.ok(BaseResponse.<AssetResponseDto>builder()
                .success(true)
                .code(201)
                .message("Asset created successfully")
                .data(asset)
                .build());
    }

    // Zimmet güncelle
    @PutMapping("/assets/{id}")
    public ResponseEntity<BaseResponse<AssetResponseDto>> updateAsset(@PathVariable Long id,
                                                                      @RequestBody @Valid AssetRequestDto dto) {
        AssetResponseDto asset = assetService.updateAsset(id, dto);
        return ResponseEntity.ok(BaseResponse.<AssetResponseDto>builder()
                .success(true)
                .code(200)
                .message("Asset updated successfully")
                .data(asset)
                .build());
    }

    // Zimmet sil
    @DeleteMapping("/assets/{id}")
    public ResponseEntity<BaseResponse<String>> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
        return ResponseEntity.ok(BaseResponse.<String>builder()
                .success(true)
                .code(200)
                .message("Asset deleted successfully")
                .data("Asset deleted")
                .build());
    }

    // Personel'e zimmet atama
    @PostMapping("/employees/{employeeId}/assets")
    public ResponseEntity<BaseResponse<EmployeeAssetResponseDto>> assignAsset(
            @PathVariable Long employeeId,
            @RequestBody @Valid AssignAssetRequestDto dto) {

        EmployeeAssetResponseDto assignment = assetService.assignAssetToEmployee(employeeId, dto);
        return ResponseEntity.ok(BaseResponse.<EmployeeAssetResponseDto>builder()
                .success(true)
                .code(201)
                .message("Asset assigned to employee")
                .data(assignment)
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
