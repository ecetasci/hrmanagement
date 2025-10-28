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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefinitionServiceTest {

    @Mock private DepartmentRepository departmentRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private CompanyRepository companyRepository;

    @InjectMocks
    private DefinitionService service;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setCompanyName("ACME");
    }

    // --- Leave Types ---
    @Test
    void findAllLeaveTypes_returnsList() {
        LeaveType lt1 = LeaveType.builder().name("Annual").company(company).build();
        LeaveType lt2 = LeaveType.builder().name("Sick").company(company).build();
        when(leaveTypeRepository.findAllByCompanyId(1L)).thenReturn(List.of(lt1, lt2));

        List<LeaveType> result = service.findAllLeaveTypes(1L);

        assertEquals(2, result.size());
        assertEquals("Annual", result.get(0).getName());
        verify(leaveTypeRepository).findAllByCompanyId(1L);
    }

    @Test
    void saveLeaveType_whenDuplicate_throwsLeaveTypeExistException() {
        LeaveTypeRequest req = new LeaveTypeRequest(1L, "Annual", "desc", true, 10);
        LeaveType existing = LeaveType.builder().name("ANNUAL").company(company).build();
        when(leaveTypeRepository.findAll()).thenReturn(List.of(existing));

        LeaveTypeExistException ex = assertThrows(LeaveTypeExistException.class, () -> service.saveLeaveType(req));
        assertEquals("Tatil Tipi zaten tanımlı", ex.getMessage());
        verify(leaveTypeRepository, never()).save(any());
    }

    @Test
    void saveLeaveType_success_returnsIdAndSavesFields() {
        LeaveTypeRequest req = new LeaveTypeRequest(1L, "Annual", "desc", true, 10);
        when(leaveTypeRepository.findAll()).thenReturn(List.of());
        when(companyRepository.getReferenceById(1L)).thenReturn(company);
        when(leaveTypeRepository.save(any(LeaveType.class))).thenAnswer(inv -> {
            LeaveType lt = inv.getArgument(0);
            lt.setId(55L);
            return lt;
        });

        Long id = service.saveLeaveType(req);

        assertEquals(55L, id);
        ArgumentCaptor<LeaveType> captor = ArgumentCaptor.forClass(LeaveType.class);
        verify(leaveTypeRepository).save(captor.capture());
        LeaveType saved = captor.getValue();
        assertEquals("Annual", saved.getName());
        assertEquals("desc", saved.getDescription());
        assertEquals(10, saved.getMaxDays());
        assertTrue(saved.isPaid());
        assertEquals(company, saved.getCompany());
    }

    @Test
    void updateLeaveType_notFound_throws() {
        when(leaveTypeRepository.findById(9L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateLeaveType(9L, new LeaveTypeRequest(1L, "n", "d", false, 1)));
        assertEquals("LeaveType not found", ex.getMessage());
    }

    @Test
    void updateLeaveType_success_updatesAndReturnsId() {
        LeaveType existing = LeaveType.builder().name("Old").description("od").maxDays(1).isPaid(false).company(company).build();
        existing.setId(3L);
        when(leaveTypeRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(leaveTypeRepository.save(any(LeaveType.class))).thenAnswer(inv -> inv.getArgument(0));

        Long id = service.updateLeaveType(3L, new LeaveTypeRequest(1L, "New", "nd", true, 7));

        assertEquals(3L, id);
        assertEquals("New", existing.getName());
        assertEquals("nd", existing.getDescription());
        assertEquals(7, existing.getMaxDays());
        assertTrue(existing.isPaid());
        verify(leaveTypeRepository).save(existing);
    }

    @Test
    void deleteLeaveType_callsRepository() {
        service.deleteLeaveType(8L);
        verify(leaveTypeRepository).deleteById(8L);
    }

    // --- Departments ---
    @Test
    void findAllDepartments_returnsList() {
        Department d1 = Department.builder().name("HR").company(company).build();
        Department d2 = Department.builder().name("IT").company(company).build();
        when(departmentRepository.findAllByCompanyId(1L)).thenReturn(List.of(d1, d2));

        List<Department> list = service.findAllDepartments(1L);

        assertEquals(2, list.size());
        assertEquals("HR", list.get(0).getName());
        verify(departmentRepository).findAllByCompanyId(1L);
    }

    @Test
    void saveDepartment_success_setsCompanyAndCreatedAt_andReturnsId() {
        when(companyRepository.getReferenceById(1L)).thenReturn(company);
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> {
            Department dep = inv.getArgument(0);
            dep.setId(77L);
            return dep;
        });

        Long id = service.saveDepartment(1L, "R&D", "desc");

        assertEquals(77L, id);
        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(captor.capture());
        Department saved = captor.getValue();
        assertEquals("R&D", saved.getName());
        assertEquals("desc", saved.getDescription());
        assertEquals(company, saved.getCompany());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void updateDepartment_notFound_throws() {
        when(departmentRepository.findById(5L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateDepartment(5L, new DepartmentDto(5L, "NewDept", "ACME")));
        assertEquals("Department not found", ex.getMessage());
    }

    @Test
    void updateDepartment_success_updatesName_andReturnsDtoWithCompanyName() {
        Department dep = Department.builder().name("Old").company(company).build();
        dep.setId(5L);
        when(departmentRepository.findById(5L)).thenReturn(Optional.of(dep));
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        DepartmentDto res = service.updateDepartment(5L, new DepartmentDto(5L, "NewDept", "ACME"));

        assertEquals(5L, res.id());
        assertEquals("NewDept", res.name());
        assertEquals("ACME", res.companyName());
        assertEquals("NewDept", dep.getName());
        verify(departmentRepository).save(dep);
    }

    @Test
    void deleteDepartment_callsRepository() {
        service.deleteDepartment(4L);
        verify(departmentRepository).deleteById(4L);
    }

    // --- Positions ---
    @Test
    void findAllPositions_success_mapsToDtos() {
        Position p1 = Position.builder().name("Dev").description("d").company(company).build();
        p1.setId(1L);
        Position p2 = Position.builder().name("QA").description("q").company(company).build();
        p2.setId(2L);
        when(positionRepository.findAllByCompanyId(1L)).thenReturn(Optional.of(List.of(p1, p2)));

        List<PositionDto> dtos = service.findAllPositions(1L);

        assertEquals(2, dtos.size());
        // Not: PositionDto ilk alanı id ismiyle tanımlı olsa da service id'yi burada geçiriyor
        assertEquals(1L, dtos.get(0).id());
        assertEquals("Dev", dtos.get(0).name());
        assertEquals("d", dtos.get(0).description());
    }

    @Test
    void findAllPositions_notFound_throws() {
        when(positionRepository.findAllByCompanyId(1L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.findAllPositions(1L));
    }

    @Test
    void savePosition_success_returnsId_andSavesFields() {
        PositionDto dto = new PositionDto(1L, "DevOps", "desc");
        when(companyRepository.getReferenceById(1L)).thenReturn(company);
        when(positionRepository.save(any(Position.class))).thenAnswer(inv -> {
            Position p = inv.getArgument(0);
            p.setId(88L);
            return p;
        });

        Long id = service.savePosition(dto);

        assertEquals(88L, id);
        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
        Position saved = captor.getValue();
        assertEquals("DevOps", saved.getName());
        assertEquals("desc", saved.getDescription());
        assertEquals(company, saved.getCompany());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void updatePosition_notFound_throws() {
        when(positionRepository.findById(3L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updatePosition(3L, new PositionDto(1L, "New", "d")));
        assertEquals("Position not found", ex.getMessage());
    }

    @Test
    void updatePosition_success_updatesAndReturnsDto() {
        Position pos = Position.builder().name("Old").description("od").company(company).build();
        pos.setId(3L);
        when(positionRepository.findById(3L)).thenReturn(Optional.of(pos));
        when(positionRepository.save(any(Position.class))).thenAnswer(inv -> inv.getArgument(0));

        PositionDto res = service.updatePosition(3L, new PositionDto(1L, "New", "nd"));

        assertEquals(3L, res.id()); // service burada id'yi geri döndürüyor
        assertEquals("New", res.name());
        assertEquals("nd", res.description());
        assertEquals("New", pos.getName());
        verify(positionRepository).save(pos);
    }

    @Test
    void deletePosition_callsRepository() {
        service.deletePosition(6L);
        verify(positionRepository).deleteById(6L);
    }
}
