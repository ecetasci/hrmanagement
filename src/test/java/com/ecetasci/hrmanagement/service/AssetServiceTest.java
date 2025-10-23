package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.AssetRequestDto;
import com.ecetasci.hrmanagement.dto.request.AssignAssetRequestDto;
import com.ecetasci.hrmanagement.dto.request.RejectAssetRequestDto;
import com.ecetasci.hrmanagement.dto.response.AssetResponseDto;
import com.ecetasci.hrmanagement.dto.response.EmployeeAssetResponseDto;
import com.ecetasci.hrmanagement.entity.Asset;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.EmployeeAsset;
import com.ecetasci.hrmanagement.enums.AssetType;
import com.ecetasci.hrmanagement.enums.EmployeeAssetStatus;
import com.ecetasci.hrmanagement.repository.AssetRepository;
import com.ecetasci.hrmanagement.repository.CompanyRepository;
import com.ecetasci.hrmanagement.repository.EmployeeAssetRepository;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeAssetRepository employeeAssetRepository;
    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private AssetService assetService;

    private Company company1;
    private Company company2;

    @BeforeEach
    void setUp() {
        company1 = new Company();
        company1.setId(1L);
        company1.setCompanyName("CompA");

        company2 = new Company();
        company2.setId(2L);
        company2.setCompanyName("CompB");
    }

    @Test
    void getAllAssets_returnsDtos() {
        Asset a1 = buildAsset(10L, "Laptop", company1);
        Asset a2 = buildAsset(11L, "Phone", company1);
        when(assetRepository.findAll()).thenReturn(List.of(a1, a2));

        List<AssetResponseDto> result = assetService.getAllAssets();

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals("Laptop", result.get(0).getName());
        assertEquals(company1.getId(), result.get(0).getCompanyId());
        assertEquals("Phone", result.get(1).getName());
    }

    @Test
    void createAsset_whenSerialExists_throws() {
        AssetRequestDto dto = defaultAssetRequestDto();
        when(assetRepository.existsBySerialNumber(dto.getSerialNumber())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> assetService.createAsset(dto));
        assertEquals("Serial number already exists!", ex.getMessage());
    }

    @Test
    void createAsset_success() {
        AssetRequestDto dto = defaultAssetRequestDto();
        when(assetRepository.existsBySerialNumber(dto.getSerialNumber())).thenReturn(false);
        when(companyRepository.findById(dto.getCompanyId())).thenReturn(Optional.of(company1));
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });

        AssetResponseDto res = assetService.createAsset(dto);

        assertEquals(100L, res.getId());
        assertEquals(dto.getName(), res.getName());
        assertEquals(dto.getSerialNumber(), res.getSerialNumber());
        assertEquals(dto.getCompanyId(), res.getCompanyId());
    }

    @Test
    void updateAsset_assetNotFound_throws() {
        when(assetRepository.findById(1L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> assetService.updateAsset(1L, defaultAssetRequestDto()));
        assertEquals("Asset not found", ex.getMessage());
    }

    @Test
    void updateAsset_companyNotFound_throws() {
        Asset existing = buildAsset(7L, "Old", company1);
        when(assetRepository.findById(7L)).thenReturn(Optional.of(existing));
        AssetRequestDto dto = defaultAssetRequestDto();
        when(companyRepository.findById(dto.getCompanyId())).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> assetService.updateAsset(7L, dto));
        assertEquals("Company not found", ex.getMessage());
    }

    @Test
    void updateAsset_success() {
        Asset existing = buildAsset(7L, "Old", company1);
        when(assetRepository.findById(7L)).thenReturn(Optional.of(existing));
        AssetRequestDto dto = defaultAssetRequestDto();
        when(companyRepository.findById(dto.getCompanyId())).thenReturn(Optional.of(company1));
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        AssetResponseDto res = assetService.updateAsset(7L, dto);

        assertEquals(dto.getName(), res.getName());
        assertEquals(dto.getBrand(), res.getBrand());
        assertEquals(dto.getModel(), res.getModel());
        assertEquals(dto.getSerialNumber(), res.getSerialNumber());
        assertEquals(dto.getType(), res.getType());
        assertEquals(dto.getCompanyId(), res.getCompanyId());
    }

    @Test
    void deleteAsset_assetNotFound_throws() {
        when(assetRepository.findById(5L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> assetService.deleteAsset(5L));
        assertEquals("Asset not found", ex.getMessage());
    }

    @Test
    void deleteAsset_whenActiveAssignment_throws() {
        Asset asset = buildAsset(3L, "Device", company1);
        when(assetRepository.findById(3L)).thenReturn(Optional.of(asset));
        when(employeeAssetRepository.existsByAssetAndStatusIn(eq(asset), anyList())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> assetService.deleteAsset(3L));
        assertEquals("Asset is currently assigned and cannot be deleted!", ex.getMessage());
    }

    @Test
    void deleteAsset_success() {
        Asset asset = buildAsset(4L, "Device", company1);
        when(assetRepository.findById(4L)).thenReturn(Optional.of(asset));
        when(employeeAssetRepository.existsByAssetAndStatusIn(eq(asset), anyList())).thenReturn(false);

        assetService.deleteAsset(4L);

        verify(assetRepository, times(1)).delete(asset);
    }

    @Test
    void assignAssetToEmployee_assetNotFound_throws() {
        AssignAssetRequestDto dto = new AssignAssetRequestDto(10L, LocalDate.now());
        when(assetRepository.findById(10L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> assetService.assignAssetToEmployee(1L, dto));
        assertEquals("Asset not found", ex.getMessage());
    }

    @Test
    void assignAssetToEmployee_employeeNotFound_throws() {
        AssignAssetRequestDto dto = new AssignAssetRequestDto(10L, LocalDate.now());
        Asset asset = buildAsset(10L, "Laptop", company1);
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> assetService.assignAssetToEmployee(1L, dto));
        assertEquals("Employee not found", ex.getMessage());
    }

    @Test
    void assignAssetToEmployee_alreadyAssigned_throws() {
        AssignAssetRequestDto dto = new AssignAssetRequestDto(10L, LocalDate.now());
        Asset asset = buildAsset(10L, "Laptop", company1);
        Employee emp = buildEmployee(1L, company1);
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));
        when(employeeAssetRepository.existsByAssetAndStatusIn(eq(asset), anyList())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> assetService.assignAssetToEmployee(1L, dto));
        assertEquals("Asset is already assigned to another employee!", ex.getMessage());
    }

    @Test
    void assignAssetToEmployee_companyMismatch_throws() {
        AssignAssetRequestDto dto = new AssignAssetRequestDto(10L, LocalDate.now());
        Asset asset = buildAsset(10L, "Laptop", company1);
        Employee emp = buildEmployee(1L, company2); // farklı şirket
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));
        when(employeeAssetRepository.existsByAssetAndStatusIn(eq(asset), anyList())).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> assetService.assignAssetToEmployee(1L, dto));
        assertEquals("Asset and employee belong to different companies!", ex.getMessage());
    }

    @Test
    void assignAssetToEmployee_success_withDate() {
        LocalDate assigned = LocalDate.of(2025, 1, 2);
        AssignAssetRequestDto dto = new AssignAssetRequestDto(10L, assigned);
        Asset asset = buildAsset(10L, "Laptop", company1);
        Employee emp = buildEmployee(1L, company1);
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));
        when(employeeAssetRepository.existsByAssetAndStatusIn(eq(asset), anyList())).thenReturn(false);
        when(employeeAssetRepository.save(any(EmployeeAsset.class))).thenAnswer(inv -> {
            EmployeeAsset ea = inv.getArgument(0);
            ea.setId(999L);
            return ea;
        });

        EmployeeAssetResponseDto res = assetService.assignAssetToEmployee(1L, dto);

        assertEquals(999L, res.getId());
        assertEquals(emp.getEmployeeNumber(), res.getEmployeeNumber());
        assertEquals(emp.getName(), res.getEmployeeName());
        assertEquals(asset.getName(), res.getAssetName());
        assertEquals(assigned, res.getAssignedDate());
        assertEquals(EmployeeAssetStatus.ASSIGNED, res.getStatus());
    }

    @Test
    void assignAssetToEmployee_success_nullDate_setsNow() {
        AssignAssetRequestDto dto = new AssignAssetRequestDto(10L, null);
        Asset asset = buildAsset(10L, "Laptop", company1);
        Employee emp = buildEmployee(1L, company1);
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));
        when(employeeAssetRepository.existsByAssetAndStatusIn(eq(asset), anyList())).thenReturn(false);
        ArgumentCaptor<EmployeeAsset> captor = ArgumentCaptor.forClass(EmployeeAsset.class);
        when(employeeAssetRepository.save(any(EmployeeAsset.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeAssetResponseDto res = assetService.assignAssetToEmployee(1L, dto);

        // capture and assert date set
        verify(employeeAssetRepository).save(captor.capture());
        EmployeeAsset saved = captor.getValue();
        assertNotNull(saved.getAssignedDate());
        assertEquals(saved.getAssignedDate(), res.getAssignedDate());
        assertEquals(EmployeeAssetStatus.ASSIGNED, res.getStatus());
    }

    @Test
    void getEmployeeAssets_returnsDtos() {
        Employee emp = buildEmployee(2L, company1);
        Asset asset = buildAsset(10L, "Laptop", company1);
        EmployeeAsset ea1 = buildEmployeeAsset(101L, emp, asset, LocalDate.of(2025, 1, 1), EmployeeAssetStatus.ASSIGNED, "note1");
        EmployeeAsset ea2 = buildEmployeeAsset(102L, emp, asset, LocalDate.of(2025, 1, 2), EmployeeAssetStatus.CONFIRMED, "note2");
        when(employeeAssetRepository.findByEmployeeId(2L)).thenReturn(List.of(ea1, ea2));

        List<EmployeeAssetResponseDto> list = assetService.getEmployeeAssets(2L);

        assertEquals(2, list.size());
        assertEquals("note1", list.get(0).getEmployeeNote());
        assertEquals(EmployeeAssetStatus.CONFIRMED, list.get(1).getStatus());
    }

    @Test
    void confirmEmployeeAsset_notFound_throws() {
        when(employeeAssetRepository.findById(50L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> assetService.confirmEmployeeAsset(50L));
        assertEquals("Assignment not found", ex.getMessage());
    }

    @Test
    void confirmEmployeeAsset_wrongStatus_throws() {
        Employee emp = buildEmployee(1L, company1);
        Asset asset = buildAsset(1L, "Device", company1);
        EmployeeAsset ea = buildEmployeeAsset(70L, emp, asset, LocalDate.now(), EmployeeAssetStatus.CONFIRMED, null);
        when(employeeAssetRepository.findById(70L)).thenReturn(Optional.of(ea));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> assetService.confirmEmployeeAsset(70L));
        assertEquals("Only ASSIGNED assets can be confirmed!", ex.getMessage());
    }

    @Test
    void confirmEmployeeAsset_success() {
        Employee emp = buildEmployee(1L, company1);
        Asset asset = buildAsset(1L, "Device", company1);
        EmployeeAsset ea = buildEmployeeAsset(71L, emp, asset, LocalDate.now(), EmployeeAssetStatus.ASSIGNED, "some note");
        when(employeeAssetRepository.findById(71L)).thenReturn(Optional.of(ea));
        when(employeeAssetRepository.save(any(EmployeeAsset.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeAssetResponseDto res = assetService.confirmEmployeeAsset(71L);

        assertEquals(EmployeeAssetStatus.CONFIRMED, res.getStatus());
        assertNull(res.getEmployeeNote());
    }

    @Test
    void rejectEmployeeAsset_notFound_throws() {
        when(employeeAssetRepository.findById(80L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> assetService.rejectEmployeeAsset(80L, new RejectAssetRequestDto("n")));
        assertEquals("Assignment not found", ex.getMessage());
    }

    @Test
    void rejectEmployeeAsset_wrongStatus_throws() {
        Employee emp = buildEmployee(1L, company1);
        Asset asset = buildAsset(1L, "Device", company1);
        EmployeeAsset ea = buildEmployeeAsset(90L, emp, asset, LocalDate.now(), EmployeeAssetStatus.CONFIRMED, null);
        when(employeeAssetRepository.findById(90L)).thenReturn(Optional.of(ea));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> assetService.rejectEmployeeAsset(90L, new RejectAssetRequestDto("n")));
        assertEquals("Only ASSIGNED assets can be rejected!", ex.getMessage());
    }

    @Test
    void rejectEmployeeAsset_success() {
        Employee emp = buildEmployee(1L, company1);
        Asset asset = buildAsset(1L, "Device", company1);
        EmployeeAsset ea = buildEmployeeAsset(91L, emp, asset, LocalDate.now(), EmployeeAssetStatus.ASSIGNED, null);
        when(employeeAssetRepository.findById(91L)).thenReturn(Optional.of(ea));
        when(employeeAssetRepository.save(any(EmployeeAsset.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeAssetResponseDto res = assetService.rejectEmployeeAsset(91L, new RejectAssetRequestDto("noted"));

        assertEquals(EmployeeAssetStatus.REJECTED, res.getStatus());
        assertEquals("noted", res.getEmployeeNote());
    }

    // helpers
    private Asset buildAsset(Long id, String name, Company company) {
        Asset a = new Asset();
        a.setId(id);
        a.setName(name);
        a.setBrand("Brand");
        a.setModel("Model");
        a.setSerialNumber("SN-" + id);
        a.setValue(BigDecimal.TEN);
        a.setType(AssetType.COMPUTER);
        a.setCompany(company);
        return a;
        }

    private Employee buildEmployee(Long id, Company company) {
        Employee e = new Employee();
        e.setId(id);
        e.setName("Emp" + id);
        e.setEmployeeNumber("E" + id);
        e.setCompany(company);
        e.setEmail("e" + id + "@x.com");
        e.setPassword("p");
        return e;
    }

    private EmployeeAsset buildEmployeeAsset(Long id, Employee e, Asset a, LocalDate date, EmployeeAssetStatus status, String note) {
        EmployeeAsset ea = new EmployeeAsset();
        ea.setId(id);
        ea.setEmployee(e);
        ea.setAsset(a);
        ea.setAssignedDate(date);
        ea.setStatus(status);
        ea.setEmployeeNote(note);
        return ea;
    }

    private AssetRequestDto defaultAssetRequestDto() {
        AssetRequestDto dto = new AssetRequestDto();
        dto.setName("New Device");
        dto.setBrand("BrandX");
        dto.setModel("ModelY");
        dto.setSerialNumber("SN-NEW");
        dto.setValue(BigDecimal.valueOf(1234));
        dto.setType(AssetType.PHONE);
        dto.setCompanyId(company1.getId());
        return dto;
    }
}

