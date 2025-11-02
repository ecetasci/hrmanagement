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
import com.ecetasci.hrmanagement.entity.LeaveRequest;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import com.ecetasci.hrmanagement.repository.LeaveTypeRepository;
import com.ecetasci.hrmanagement.repository.UserRepository;
import com.ecetasci.hrmanagement.service.*;
import com.ecetasci.hrmanagement.mapper.LeaveMapper;
import com.ecetasci.hrmanagement.utility.JwtManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static com.ecetasci.hrmanagement.constant.Endpoints.MANAGER;

import com.ecetasci.hrmanagement.exceptions.UnauthorizedException;
import com.ecetasci.hrmanagement.exceptions.ForbiddenException;
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;


/**
 * CompanyManagerController — şirket içi yönetici işlemleri (personel, izin, zimmet, gider vb.).
 * <p>
 * Sağladığı başlıca işlevler:
 * - Personel kayıt ve listeleme
 * - İzin türleri ve izin talepleri yönetimi
 * - Zimmet (asset) CRUD ve atama işlemleri
 * - Gider (expense) listeleme ve yönetimi
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(MANAGER)
public class CompanyManagerController {
    private final CompanyManagerService companyManagerService;
    private final LeaveService leaveService;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final AssetService assetService;
    private final ExpenseService expenseService;
    private final LeaveMapper leaveMapper; // added for mapping
    private final JwtManager jwtManager;
    private final UserRepository userRepository;


    /**
     * Yeni personel kaydı oluşturur.
     *
     * @param dto Kayıt için gerekli alanları içeren DTO
     * @return Oluşturulan personel bilgilerini içeren BaseResponse
     */

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

    /**
     * Şirkete ait izin türlerini döner.
     *
     * @return İzin türleri DTO listesi
     */
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


    /**
     * Tüm izin taleplerini getirir.
     *
     * @return İzin talepleri DTO listesi
     */
    @GetMapping("/leaves")
    public ResponseEntity<List<LeaveResponseDto>> getAllLeaves(Long companyId) {
        //önce şirketin çalışanlarını bul, sonra onların izin taleplerini topla
        List<LeaveRequest> allLeaves = employeeRepository.findAllByCompanyId(companyId).stream()
                .flatMap(emp -> emp.getLeaveRequests().stream())
                .toList();

        List<LeaveResponseDto> dtos = allLeaves.stream()
                .map(leaveMapper::toDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Bir izin talebini onaylar.
     *
     * @param employeeNumber        Onaylayan yönetici/çalışanın numarası
     * @param leaveStartDate        İzin başlangıç tarihi
     * @param managerEmployeeNumber Yönetici numarası
     * @return Onay sonucu mesajı
     */
    @PutMapping("/leaves/{id}/approve")
    public ResponseEntity<String> approveLeave(@RequestParam String employeeNumber,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate leaveStartDate,
                                               @RequestParam String managerEmployeeNumber
    ) {
        leaveService.approveLeaveRequest(employeeNumber, leaveStartDate, managerEmployeeNumber);
        return ResponseEntity.ok("Leave approved");
    }

    /**
     * Bir izin talebini reddeder.
     *
     * @param employeeNumber Çalışanın numarası
     * @param managerNumber  Yönetici numarası
     * @param managerNote    Yönetici notu
     * @return Red sonucu mesajı
     */
    @PutMapping("/leaves/{id}/reject")
    public ResponseEntity<String> rejectLeave(@RequestParam String employeeNumber,
                                              @RequestParam String managerNumber,
                                              @RequestParam String managerNote) {
        leaveService.rejectLeaveRequestByEmployeeNumber(employeeNumber, managerNumber, managerNote);
        return ResponseEntity.ok("Leave rejected");
    }


    /**
     * Tüm zimmetleri listeler.
     * İstek yapan kullanıcının şirketine ait zimmetleri döner.
     *
     * @return Asset DTO listesi
     */
    @GetMapping("/assets")
    public ResponseEntity<BaseResponse<List<AssetResponseDto>>> getAllAssets(HttpServletRequest request) {
        try {
            //HttpServletRequest, servlet API'sinden gelen bir arayüzdür ve gelen HTTP isteğini (header, query/body parametreleri,
            // method, URI, session, client IP vb.) temsil eder. Spring MVC içinde controller metoduna parametre olarak geçirilebilir
            // ve istekten veri okumak için kullanılır
            List<AssetResponseDto> assets = assetService.getAssetsForCaller(request);
            return ResponseEntity.ok(BaseResponse.<List<AssetResponseDto>>builder()
                    .success(true)
                    .code(200)
                    .message("Assets retrieved successfully")
                    .data(assets)
                    .build());
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.<List<AssetResponseDto>>builder()
                            .success(false)
                            .code(401)
                            .message("Unauthorized: " + e.getMessage())
                            .build());
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.<List<AssetResponseDto>>builder()
                            .success(false)
                            .code(403)
                            .message("Forbidden: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Yeni zimmet oluşturur.
     *
     * @param dto Zimmet oluşturma DTO
     * @return Oluşturulan zimmet DTO
     */
    @PostMapping("/assets")
    public ResponseEntity<BaseResponse<AssetResponseDto>> createAsset(@RequestBody @Valid AssetRequestDto dto, HttpServletRequest request) {
        Long callerCompanyId = resolveCallerCompanyId(request);
        if (callerCompanyId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.<AssetResponseDto>builder().success(false).code(401).message("Unauthorized").build());
        }
        if (!callerCompanyId.equals(dto.getCompanyId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.<AssetResponseDto>builder().success(false).code(403).message("Cannot create asset for another company").build());
        }

        AssetResponseDto asset = assetService.createAsset(dto);
        return ResponseEntity.ok(BaseResponse.<AssetResponseDto>builder()
                .success(true)
                .code(201)
                .message("Asset created successfully")
                .data(asset)
                .build());
    }

    /**
     * Zimmet günceller.
     *
     * @param id  Zimmet ID
     * @param dto Güncel veri
     * @return Güncellenmiş zimmet DTO
     */
    @PutMapping("/assets/{id}")
    public ResponseEntity<BaseResponse<AssetResponseDto>> updateAsset(@PathVariable Long id,
                                                                      @RequestBody @Valid AssetRequestDto dto,
                                                                      HttpServletRequest request) {
        Long callerCompanyId = resolveCallerCompanyId(request);
        if (callerCompanyId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.<AssetResponseDto>builder().success(false).code(401).message("Unauthorized").build());
        }
        if (!callerCompanyId.equals(dto.getCompanyId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.<AssetResponseDto>builder().success(false).code(403).message("Cannot update asset for another company").build());
        }
        try {
            if (!assetService.assetBelongsToCompany(id, callerCompanyId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(BaseResponse.<AssetResponseDto>builder().success(false).code(403).message("Asset does not belong to your company").build());
            }
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.<AssetResponseDto>builder().success(false).code(404).message("Asset not found").build());
        }

        AssetResponseDto asset = assetService.updateAsset(id, dto);
        return ResponseEntity.ok(BaseResponse.<AssetResponseDto>builder()
                .success(true)
                .code(200)
                .message("Asset updated successfully")
                .data(asset)
                .build());
    }

    /**
     * Zimmet siler.
     *
     * @param id Zimmet ID
     * @return Başarı mesajı
     */
    @DeleteMapping("/assets/{id}")
    public ResponseEntity<BaseResponse<String>> deleteAsset(@PathVariable Long id, HttpServletRequest request) {
        Long callerCompanyId = resolveCallerCompanyId(request);
        if (callerCompanyId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.<String>builder().success(false).code(401).message("Unauthorized").build());
        }
        try {
            if (!assetService.assetBelongsToCompany(id, callerCompanyId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(BaseResponse.<String>builder().success(false).code(403).message("Asset does not belong to your company").build());
            }
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.<String>builder().success(false).code(404).message("Asset not found").build());
        }

        assetService.deleteAsset(id);
        return ResponseEntity.ok(BaseResponse.<String>builder()
                .success(true)
                .code(200)
                .message("Asset deleted successfully")
                .data("Asset deleted")
                .build());
    }

    /**
     * Personel'e zimmet atar.
     *
     * @param employeeId Atanacak personelin ID'si
     * @param dto        Atama bilgileri
     * @return Atamaya dair DTO
     */
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

    /**
     * Çalışanın gider (expense) kayıtlarını getirir.
     *
     * @param employeeId Çalışan ID'si
     * @return Gider DTO listesi
     */
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

    //// Çağıran kullanıcının şirket ID'sini çözümler

    private Long resolveCallerCompanyId(HttpServletRequest request) {
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
        // find user by email
        var userOpt = userRepository.findUserByEmail(username);
        if (userOpt.isEmpty()) return null;
        var user = userOpt.get();
        // find employee by user id to get company
        var empOpt = employeeRepository.findByUserId(user.getId());
        return empOpt.map(emp -> emp.getCompany() != null ? emp.getCompany().getId() : null).orElse(null);
    }

    @GetMapping("/employees")
    public ResponseEntity<BaseResponse<List<EmployeeResponseDto>>> listEmployees(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {

        Long companyId = resolveCallerCompanyId(request);
        if (companyId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.<List<EmployeeResponseDto>>builder()
                            .success(false)
                            .code(403)
                            .message("Erişim reddedildi")
                            .build());
        }

        var pageResult = employeeRepository.findAllByCompanyId(companyId, PageRequest.of(page, size));

        List<EmployeeResponseDto> dtos = pageResult.getContent().stream()
                .map(emp -> new EmployeeResponseDto(
                        emp.getId(),
                        emp.getEmployeeNumber(),
                        emp.getName(),
                        emp.getEmail(),
                        emp.getPosition(),
                        emp.getDepartment()))
                .toList();

        return ResponseEntity.ok(BaseResponse.<List<EmployeeResponseDto>>builder()
                .success(true)
                .code(200)
                .message("Çalışanlar listelendi")
                .data(dtos)
                .build());
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<BaseResponse<EmployeeResponseDto>> updateEmployee(
            @PathVariable Long id,
            @RequestBody RegisterEmployeeRequestDto dto,
            HttpServletRequest request) {

        Long callerCompanyId = resolveCallerCompanyId(request);
        if (callerCompanyId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.<EmployeeResponseDto>builder()
                            .success(false)
                            .code(403)
                            .message("Erişim reddedildi")
                            .build());
        }

        var target = employeeRepository.findById(id).orElse(null);
        if (target == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.<EmployeeResponseDto>builder()
                            .success(false)
                            .code(404)
                            .message("Çalışan bulunamadı")
                            .build());
        }

        Long targetCompanyId = target.getCompany() != null ? target.getCompany().getId() : null;
        if (!Objects.equals(callerCompanyId, targetCompanyId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.<EmployeeResponseDto>builder()
                            .success(false)
                            .code(403)
                            .message("Erişim reddedildi")
                            .build());
        }

        var updated = companyManagerService.updateEmployee(id, dto);

        var resp = new EmployeeResponseDto(
                updated.getId(),
                updated.getEmployeeNumber(),
                updated.getName(),
                updated.getEmail(),
                updated.getPosition(),
                updated.getDepartment()
        );

        return ResponseEntity.ok(BaseResponse.<EmployeeResponseDto>builder()
                .success(true)
                .code(200)
                .message("Çalışan güncellendi")
                .data(resp)
                .build());
    }

    private Employee fetchEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    // returns true when caller is NOT in same company as target
    private boolean callerNotInSameCompany(Employee target, HttpServletRequest request) {
        Long callerCompanyId = resolveCallerCompanyId(request);
        Long targetCompanyId = target.getCompany() != null ? target.getCompany().getId() : null;
        return !Objects.equals(callerCompanyId, targetCompanyId);
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<BaseResponse<String>> deleteEmployee(@PathVariable Long id, HttpServletRequest request) {
        var target = fetchEmployeeOrThrow(id);
        if (callerNotInSameCompany(target, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.<String>builder()
                            .success(false)
                            .code(403)
                            .message("Access denied")
                            .build());
        }
        companyManagerService.deleteEmployee(id);
        return ResponseEntity.ok(BaseResponse.<String>builder()
                .success(true)
                .code(200)
                .message("Employee deleted")
                .data("deleted")
                .build());
    }

    @Deprecated
    @PutMapping("/employees/{id}/activate")
    public ResponseEntity<BaseResponse<EmployeeResponseDto>> activateEmployee(@PathVariable Long id, @RequestParam(
            defaultValue = "true") boolean active, HttpServletRequest request) {
        var target = fetchEmployeeOrThrow(id);
        if (callerNotInSameCompany(target, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.<EmployeeResponseDto>builder().success(false).code(403).message("Access denied").build());
        }
        var updated = companyManagerService.setEmployeeActiveStatus(id, active);
        var resp = new EmployeeResponseDto(updated.getId(), updated.getEmployeeNumber(), updated.getName(), updated.getEmail(), updated.getPosition(), updated.getDepartment());
        return ResponseEntity.ok(BaseResponse.<EmployeeResponseDto>builder()
                .success(true)
                .code(200)
                .message("Employee activation updated")
                .data(resp).build());
    }

}
