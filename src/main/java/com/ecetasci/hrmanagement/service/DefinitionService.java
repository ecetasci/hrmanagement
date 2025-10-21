package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.LeaveTypeRequest;
import com.ecetasci.hrmanagement.dto.response.DepartmentDto;
import com.ecetasci.hrmanagement.dto.response.PositionDto;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.Department;
import com.ecetasci.hrmanagement.entity.LeaveType;
import com.ecetasci.hrmanagement.entity.Position;
import com.ecetasci.hrmanagement.exceptions.LeaveTypeExistException;
import com.ecetasci.hrmanagement.repository.CompanyRepository;
import com.ecetasci.hrmanagement.repository.DepartmentRepository;
import com.ecetasci.hrmanagement.repository.LeaveTypeRepository;
import com.ecetasci.hrmanagement.repository.PositionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DefinitionService {

    private final DepartmentRepository departmentRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final PositionRepository positionRepository;
    private final CompanyRepository companyRepository;

    // --- Leave Types ---
    public List<LeaveType> findAllLeaveTypes(Long companyId) {
        return leaveTypeRepository.findAllByCompanyId(companyId);
    }

    public Long saveLeaveType(LeaveTypeRequest leaveTypeRequest) {
        boolean isExist = leaveTypeRepository.findAll().stream().anyMatch(leaveType -> leaveTypeRequest.name().equalsIgnoreCase(leaveType.getName()));
        if (isExist) {
            throw new LeaveTypeExistException("Tatil Tipi zaten tanımlı");
        }

        LeaveType leaveType = LeaveType.builder()
                .company(companyRepository.getReferenceById(leaveTypeRequest.companyId()))
                .name(leaveTypeRequest.name())
                .description(leaveTypeRequest.description())
                .isPaid(leaveTypeRequest.isPaid())
                .maxDays(leaveTypeRequest.maxDay())
                .build();

        LeaveType saved = leaveTypeRepository.save(leaveType);
        return saved.getId();
    }

    public Long updateLeaveType(Long id, LeaveTypeRequest updated) {
        LeaveType existing = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("LeaveType not found"));
        existing.setName(updated.name());
        existing.setDescription(updated.description());
        existing.setMaxDays(updated.maxDay());
        existing.setPaid(updated.isPaid());
        LeaveType saved = leaveTypeRepository.save(existing);
        return saved.getId();
    }

    public void deleteLeaveType(Long id) {
        leaveTypeRepository.deleteById(id);
    }

    // --- Departments ---
    @Transactional
    public List<Department> findAllDepartments(Long companyId) {
        return departmentRepository.findAllByCompanyId(companyId);
    }

    @Transactional
    public Long saveDepartment(Long companyId, String departmentName, String description) {
        Department department = new Department();
        department.setName(departmentName);
        department.setDescription(description);

        department.setCompany(companyRepository.getReferenceById(companyId)); // sadece ID set yeterli
        department.setCreatedAt(LocalDateTime.now());
        Department saved = departmentRepository.save(department);
        return saved.getId();
    }


    public DepartmentDto updateDepartment(Long id, DepartmentDto departmentDto) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));
        existing.setName(departmentDto.name());

        Department saved = departmentRepository.save(existing);
        saved.setUpdatedAt(LocalDateTime.now());
        DepartmentDto departmentDto1 = new DepartmentDto(saved.getId(), saved.getName(), saved.getCompany().getCompanyName());
        return departmentDto1;
    }

    public void deleteDepartment(Long id) {
        departmentRepository.deleteById(id);
    }

    // --- Positions ---
    @Transactional
    public List<PositionDto> findAllPositions(Long companyId) {
        List<Position> positions = positionRepository.findAllByCompanyId(companyId).orElseThrow();

        return positions.stream()
                .map(position -> new PositionDto(
                        position.getId(),
                        position.getName(),
                        position.getDescription()
                )).toList();
    }

    @Transactional
    public Long savePosition(PositionDto positionDto) {

        Position position = new Position();
        position.setName(positionDto.name());
        position.setDescription(positionDto.description());
        position.setCompany(companyRepository.getReferenceById(positionDto.companyId()));
        position.setCreatedAt(LocalDateTime.now());

        Position saved = positionRepository.save(position);
        return saved.getId();
    }

    @Transactional
    public PositionDto updatePosition(Long id, PositionDto updated) {
        Position existing = positionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Position not found"));

        existing.setName(updated.name());
        existing.setDescription(updated.description());
        existing.setUpdatedAt(LocalDateTime.now());

        Position saved = positionRepository.save(existing);
        return new PositionDto(saved.getId(), saved.getName(), saved.getDescription());
    }

    @Transactional
    public void deletePosition(Long id) {
        positionRepository.deleteById(id);
    }
}
