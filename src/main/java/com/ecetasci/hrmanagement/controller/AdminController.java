package com.ecetasci.hrmanagement.controller;

import com.ecetasci.hrmanagement.dto.request.*;
import com.ecetasci.hrmanagement.dto.response.*;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.Department;
import com.ecetasci.hrmanagement.entity.LeaveType;
import com.ecetasci.hrmanagement.enums.ResponseMessageEnum;
import com.ecetasci.hrmanagement.repository.CompanyRepository;
import com.ecetasci.hrmanagement.service.DefinitionService;
import com.ecetasci.hrmanagement.service.SiteAdminService;
import com.ecetasci.hrmanagement.service.UserService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.ecetasci.hrmanagement.constant.Endpoints.ADMIN;

/**
 * AdminController — site yöneticisi işlemleri ve şirket/definition yönetimi.
 * Sağladığı işlevler:
 * - Şirket listesini sayfalandırarak getirir
 * - Üyelik planı (subscription) oluşturma
 * - Şirket başvurularını onaylama/reddetme
 * - Tanım (leave types, departments, positions) CRUD işlemleri
 */
@RestController
@RequestMapping(ADMIN)
@RequiredArgsConstructor
public class AdminController {


    private final CompanyRepository companyRepository;
    private final SiteAdminService siteAdminService;
    private final DefinitionService definitionService;
    private final UserService userService;


    /**
     * Şirketleri sayfalandırılmış şekilde getirir.
     *
     * @param page      Sayfa numarası (varsayılan 0)
     * @param size      Sayfa boyutu (varsayılan 10)
     * @param sortBy    Sıralama alanı (varsayılan id)
     * @param direction Sıralama yönü (asc/desc)
     * @return PagedResponse içindeki CompanyResponse nesneleri ile BaseResponse
     */
    @GetMapping("/list-company")
    public ResponseEntity<BaseResponse<PagedResponse<CompanyResponse>>> getCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Company> companiesPage = companyRepository.findAll(pageable);

        System.out.println(companiesPage);

        List<CompanyResponse> content = companiesPage.getContent().stream()
                .map(company -> new CompanyResponse(company.getId(), company.getCompanyName(),
                        company.getCompanyEmail(), company.getPhoneNumber(), company.getAddress(), company.getTaxNumber(),
                        company.getWebsite(), company.getEmployeeCount(), company.getFoundedDate()))
                .toList();

        System.out.println(content);

        PagedResponse<CompanyResponse> pagedResponse = PagedResponse.<CompanyResponse>builder()
                .content(content)
                .page(companiesPage.getNumber())
                .size(companiesPage.getSize())
                .totalElements(companiesPage.getTotalElements())
                .totalPages(companiesPage.getTotalPages())
                .last(companiesPage.isLast())
                .build();

        return ResponseEntity.ok(BaseResponse.<PagedResponse<CompanyResponse>>builder()
                .success(true)
                .code(200)
                .message("Companies retrieved successfully")
                .data(pagedResponse)
                .build());
    }


    /**
     * Yeni bir şirket başvurusu oluşturur.Admin onayına sunar. Aynı zamanda user oluşturarak,
     * şirket yöneticisi kaydı da yapar. Bu şekilde subscription oluşturulmadan önce şirket yöneticisi de sisteme eklenmiş olur.
     * Ve security kısmında da yalnızca yetkili  kişi olan company admin subscription oluşturabilir.
     *
     * @param companyRequest Şirket başvurusu bilgilerini içeren DTO
     * @return Oluşturulan şirket başvurusu bilgileri
     */
    @Transactional
    @PostMapping("/create-application-company")
    public ResponseEntity<BaseResponse<String>> createApplication(
            @Valid @RequestBody CompanyRequest companyRequest, RegisterCompanyManagerRequestDto registerCompanyManagerRequestDto) {
        CompanyResponse application = siteAdminService.createApplication(companyRequest);
        Long compId = application.id();
        System.out.println(compId);
        registerCompanyManagerRequestDto.setCompanyId(compId);
        RegisterResponseDto registered = userService.registerForManager(registerCompanyManagerRequestDto);


        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.<String>builder()
                        .success(true)
                        .code(HttpStatus.CREATED.value())
                        .message("Application created successfully")
                        .data(application+" | Company Manager Registered: " + registered)
                        .build());
    }


    /**
     * Yeni bir şirket için abonelik (subscription) oluşturur.
     *
     * @param subscriptionRequestDto Abonelik bilgilerini içeren DTO
     * @return Oluşturulan abonelik bilgileri
     */
    @PostMapping("/create-subscription")
    public ResponseEntity<BaseResponse<SubscriptionResponseDto>> createSubscription(
            @Valid @RequestBody SubscriptionRequestDto subscriptionRequestDto) {

        SubscriptionResponseDto subscription = siteAdminService.createSubscription(subscriptionRequestDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.<SubscriptionResponseDto>builder()
                        .success(true)
                        .code(HttpStatus.CREATED.value())
                        .message("Company subscription created successfully")
                        .data(subscription)
                        .build());
    }

    /**
     * Şirket başvurusunu onaylar.
     *
     * @param id Onaylanacak şirket başvurusunun ID'si
     * @return Başarı mesajı ve şirket ID'si
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<BaseResponse<String>> approveCompany(@PathVariable Long id) {

        siteAdminService.approveCompanyApplication(id);//Bu tarihi subscription başlangıç tarihine set edelim

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(BaseResponse.<String>builder()
                        .success(true)
                        .code(ResponseMessageEnum.COMPANY_APPLICATION_APPROVED.getCode())
                        .message(ResponseMessageEnum.COMPANY_APPLICATION_APPROVED.getDesc())
                        .data("Company ID: " + id)
                        .build());
    }

    /**
     * Şirket başvurusunu reddeder.
     *
     * @param id Reddedilecek şirket başvurusunun ID'si
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<BaseResponse<String>> rejectCompany(@PathVariable Long id) {
        siteAdminService.rejectCompanyApplication(id);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.<String>builder()
                        .success(true)
                        .code(200)
                        .message("Company application rejected")
                        .data("Company ID: " + id)
                        .build());
    }

    /**
     * İzin türlerini listeler.
     *
     * @param id (Opsiyonel) filtreleme için kullanılabilir
     * @return İzin türleri listesi
     */
    @GetMapping("/definitions/leave-types")
    public ResponseEntity<BaseResponse<List<LeaveTypeResponseDto>>> getLeaveTypes(@RequestParam(required = false) Long id) {
        List<LeaveType> leaveTypeList = definitionService.findAllLeaveTypes(id);

        List<LeaveTypeResponseDto> dtoList = leaveTypeList.stream()
                .map(lt -> new LeaveTypeResponseDto(
                        lt.getId(),
                        lt.getName(),
                        lt.getDescription(),
                        lt.getMaxDays(),
                        // boolean getter olabilir getIsPaid() veya isPaid(); fallback handled at compile if mismatch
                        lt.isPaid(),
                        lt.getCompany() != null ? lt.getCompany().getId() : null
                ))
                .toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.<List<LeaveTypeResponseDto>>builder()
                        .success(true)
                        .code(200)
                        .message("Leave types retrieved successfully")
                        .data(dtoList)
                        .build());

    }

    /**
     * Yeni izin türü oluşturur.
     *
     * @param leaveTypeRequest Oluşturulacak izin türü bilgileri
     * @return Oluşturulan izin türünün ID'si
     */
    @PostMapping("/definitions/create-leave-types")
    public ResponseEntity<BaseResponse<Long>> createLeaveType(@RequestBody @Valid LeaveTypeRequest leaveTypeRequest) {
        Long leaveTypeId = definitionService.saveLeaveType(leaveTypeRequest);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.<Long>builder()
                        .success(true)
                        .code(200)
                        .message("Leave created")
                        .data(leaveTypeId)
                        .build());
    }

    /**
     * İzin türünü günceller.
     *
     * @param id               Güncellenecek izin türünün ID'si
     * @param leaveTypeRequest Yeni veri
     * @return Güncellenmiş izin türünün ID'si
     */
    @PutMapping("/definitions/leave-types/{id}")
    public ResponseEntity<BaseResponse<Long>> updateLeaveType(@PathVariable Long id, @RequestBody LeaveTypeRequest leaveTypeRequest) {
        Long updatedLeaveType = definitionService.updateLeaveType(id, leaveTypeRequest);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.<Long>builder()
                        .success(true)
                        .code(200)
                        .message("Leave updated")
                        .data(updatedLeaveType)
                        .build());
    }

    /**
     * İzin türünü siler.
     *
     * @param id Silinecek izin türünün ID'si
     */
    @DeleteMapping("/definitions/leave-types/{id}")
    public void deleteLeaveType(@PathVariable Long id) {
        definitionService.deleteLeaveType(id);
    }


    /**
     * Departmanları getirir.
     *
     * @param id (Opsiyonel) şirket id ile filtreleme
     * @return Departman DTO listesi
     */
    @GetMapping("/definitions/departments")
    public ResponseEntity<BaseResponse<List<DepartmentDto>>> getDepartments(@RequestParam Long id) {
        List<Department> departmentList = definitionService.findAllDepartments(id);
        List<DepartmentDto> departmantDtoList = departmentList.stream()
                .map(department -> new DepartmentDto(department.getId(), department.getName(), department.getCompany().getCompanyName()))
                .toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.<List<DepartmentDto>>builder()
                        .success(true)
                        .code(200)
                        .message("departments")
                        .data(departmantDtoList)
                        .build());

    }

    /**
     * Yeni departman oluşturur.
     *
     * @param companyId      Şirket ID'si
     * @param departmentName Departman adı
     * @param description    Açıklama
     * @return Oluşturulan departman ID'si
     */
    @PostMapping("/definitions/create-departments")
    public Long createDepartment(@RequestBody Long companyId, String departmentName, String description) {
        return definitionService.saveDepartment(companyId, departmentName, description);
    }

    /**
     * Departmanı günceller.
     *
     * @param id            Departman ID
     * @param departmentDto Güncel departman verisi
     * @return Güncellenmiş departman DTO
     */
    @PutMapping("/definitions/departments/{id}")
    public DepartmentDto updateDepartment(@PathVariable Long id, DepartmentDto departmentDto) {
        return definitionService.updateDepartment(id, departmentDto);
    }

    /**
     * Departmanı siler.
     *
     * @param id Departman ID
     */
    @DeleteMapping("/definitions/departments/{id}")
    public void deleteDepartment(@PathVariable Long id) {
        definitionService.deleteDepartment(id);
    }

    /**
     * Pozisyonları listeler.
     *
     * @param companyId Şirket ID'si ile filtreleme
     * @return Pozisyon DTO listesi
     */
    @GetMapping("/definitions/positions")
    public ResponseEntity<BaseResponse<List<PositionDto>>> getPositions(@RequestParam Long companyId) {
        List<PositionDto> allPositions = definitionService.findAllPositions(companyId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.<List<PositionDto>>builder()
                        .success(true)
                        .code(200)
                        .message("positions")
                        .data(allPositions)
                        .build());

    }

    /**
     * Yeni pozisyon oluşturur.
     *
     * @param position Pozisyon DTO
     * @return Oluşturulan pozisyon ID'si
     */
    @PostMapping("/definitions/create-positions")
    public Long createPosition(@RequestBody PositionDto position) {
        return definitionService.savePosition(position);
    }

    /**
     * Pozisyonu günceller.
     *
     * @param id       Pozisyon ID
     * @param position Yeni pozisyon verisi
     * @return Güncellenmiş pozisyon DTO
     */
    @PutMapping("/definitions/positions/{id}")
    public PositionDto updatePosition(@PathVariable Long id, @RequestBody PositionDto position) {
        return definitionService.updatePosition(id, position);
    }

    /**
     * Pozisyonu siler.
     *
     * @param id Pozisyon ID
     */
    @DeleteMapping("/definitions/positions/{id}")
    public void deletePosition(@PathVariable Long id) {
        definitionService.deletePosition(id);
    }


}
