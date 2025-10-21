package com.ecetasci.hrmanagement.controller;

import com.ecetasci.hrmanagement.dto.request.LeaveTypeRequest;
import com.ecetasci.hrmanagement.dto.request.SubscriptionRequestDto;
import com.ecetasci.hrmanagement.dto.response.*;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.Department;
import com.ecetasci.hrmanagement.entity.LeaveType;
import com.ecetasci.hrmanagement.entity.Position;
import com.ecetasci.hrmanagement.enums.ResponseMessageEnum;
import com.ecetasci.hrmanagement.enums.SubscriptionType;
import com.ecetasci.hrmanagement.repository.CompanyRepository;
import com.ecetasci.hrmanagement.service.DefinitionService;
import com.ecetasci.hrmanagement.service.SiteAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/admin")
@RequiredArgsConstructor
public class AdminController {


    private final CompanyRepository companyRepository;
    private final SiteAdminService siteAdminService;
    private final DefinitionService definitionService;


    @GetMapping("/list-company")
    public ResponseEntity<BaseResponse<List<CompanyResponse>>> getCompanies() {
        List<Company> companies = companyRepository.findAll();
        List<CompanyResponse> list = companies.stream().map(company -> new CompanyResponse(company.getId(), company.getCompanyName(),
                company.getCompanyEmail(), company.getPhoneNumber(), company.getAddress(), company.getTaxNumber(),
                company.getWebsite(), company.getEmployeeCount(), company.getFoundedDate())).toList();
        return ResponseEntity.ok(BaseResponse.<List<CompanyResponse>>builder()
                .success(true)
                .code(200)
                .message("Companies retrieved successfully")
                .data(list)
                .build());
    }

    //POST /api/admin/companies/{id}/subscription - Üyelik planı oluştur*/

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


    /* ● PUT /api/admin/companies/{id}/approve - Başvuru onayı */
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

    /* ● PUT /api/admin/companies/{id}/reject - Başvuru reddi */
    @PutMapping("/{id}/reject")
    public ResponseEntity<BaseResponse<String>> rejectCompany(@PathVariable Long id) {
        siteAdminService.rejectCompanyApplication(id);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.<String>builder()
                        .success(false)
                        .code(200)
                        .message("Company application rejected")
                        .data("Company ID: " + id)
                        .build());
    }


    // === Definitions: Leave Types ===
    @GetMapping("/definitions/leave-types")
    public ResponseEntity<BaseResponse<List<LeaveType>>> getLeaveTypes(Long id) {
        List<LeaveType> leaveTypeList = definitionService.findAllLeaveTypes(id);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.<List<LeaveType>>builder()
                        .success(true)
                        .code(200)
                        .message("Company subscription created")
                        .data(leaveTypeList)
                        .build());

    }

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

    @PutMapping("/definitions/leave-types/{id}")
    public ResponseEntity<BaseResponse<Long>> updateLeaveType(@PathVariable Long id, @RequestBody LeaveTypeRequest leaveTypeRequest) {
        Long updatedLeaveType = definitionService.updateLeaveType(id, leaveTypeRequest);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.<Long>builder()
                        .success(true)
                        .code(200)
                        .message("Leave created")
                        .data(updatedLeaveType)
                        .build());
    }

    @DeleteMapping("/definitions/leave-types/{id}")
    public void deleteLeaveType(@PathVariable Long id) {
        definitionService.deleteLeaveType(id);
    }


    // ================= DEPARTMENTS ==================
    @GetMapping("/definitions/departments")
    public ResponseEntity<BaseResponse<List<DepartmentDto>>> getDepartments(@RequestParam Long id) {
        List<Department> departmentList = definitionService.findAllDepartments(id);
        List<DepartmentDto> departmantDtoList = departmentList.stream().map(department -> {
            return new DepartmentDto(department.getId(), department.getName(), department.getCompany().getCompanyName());
        }).toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.<List<DepartmentDto>>builder()
                        .success(true)
                        .code(200)
                        .message("departments")
                        .data(departmantDtoList)
                        .build());

    }

    @PostMapping("/definitions/create-departments")
    public Long createDepartment(@RequestBody Long companyId, String departmentName, String description) {
        return definitionService.saveDepartment(companyId, departmentName, description);
    }

    @PutMapping("/definitions/departments/{id}")
    public DepartmentDto updateDepartment(@PathVariable Long id, DepartmentDto departmentDto) {
        return definitionService.updateDepartment(id, departmentDto);
    }

    @DeleteMapping("/definitions/departments/{id}")
    public void deleteDepartment(@PathVariable Long id) {
        definitionService.deleteDepartment(id);
    }

    // ================= POSITIONS ==================
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

    @PostMapping("/definitions/create-positions")
    public Long createPosition(@RequestBody PositionDto position) {
        return definitionService.savePosition(position);
    }

    @PutMapping("/definitions/positions/{id}")
    public PositionDto updatePosition(@PathVariable Long id, @RequestBody PositionDto position) {
        return definitionService.updatePosition(id, position);
    }

    @DeleteMapping("/definitions/positions/{id}")
    public void deletePosition(@PathVariable Long id) {
        definitionService.deletePosition(id);
    }


}

