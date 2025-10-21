package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.AssetRequestDto;
import com.ecetasci.hrmanagement.dto.request.AssignAssetRequestDto;
import com.ecetasci.hrmanagement.dto.request.RejectAssetRequestDto;
import com.ecetasci.hrmanagement.dto.response.AssetResponseDto;
import com.ecetasci.hrmanagement.dto.response.EmployeeAssetResponseDto;
import com.ecetasci.hrmanagement.entity.Asset;
import com.ecetasci.hrmanagement.entity.Employee;
import com.ecetasci.hrmanagement.entity.EmployeeAsset;
import com.ecetasci.hrmanagement.enums.EmployeeAssetStatus;
import com.ecetasci.hrmanagement.repository.AssetRepository;
import com.ecetasci.hrmanagement.repository.CompanyRepository;
import com.ecetasci.hrmanagement.repository.EmployeeAssetRepository;
import com.ecetasci.hrmanagement.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetService {
    private final AssetRepository assetRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeAssetRepository employeeAssetRepository;
    private final CompanyRepository companyRepository;

    // Tüm zimmetler
    public List<AssetResponseDto> getAllAssets() {
        return assetRepository.findAll().stream()
                .map(this::toAssetDto)
                .toList();
    }

    // Zimmet oluşturma (serial number unique kontrolü)
    public AssetResponseDto createAsset(AssetRequestDto dto) {
        if (assetRepository.existsBySerialNumber(dto.getSerialNumber())) {
            throw new RuntimeException("Serial number already exists!");
        }
        Asset asset = new Asset();
        asset.setName(dto.getName());
        asset.setBrand(dto.getBrand());
        asset.setModel(dto.getModel());
        asset.setSerialNumber(dto.getSerialNumber());
        asset.setValue(dto.getValue());
        asset.setType(dto.getType());
        asset.setCompany(companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found")));
        Asset saved = assetRepository.save(asset);
        return toAssetDto(saved);
    }

    // Zimmet güncelleme//tekrar bak
    public AssetResponseDto updateAsset(Long id, AssetRequestDto dto) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found"));
        asset.setName(dto.getName());
        asset.setBrand(dto.getBrand());
        asset.setModel(dto.getModel());
        asset.setSerialNumber(dto.getSerialNumber());
        asset.setValue(dto.getValue());
        asset.setType(dto.getType());
        asset.setCompany(companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found")));
        return toAssetDto(assetRepository.save(asset));
    }

    // Zimmet silme
    public void deleteAsset(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found"));
        // Aktif zimmet varsa silinemez
        boolean hasActive = employeeAssetRepository.existsByAssetAndStatusIn(
                asset, List.of(EmployeeAssetStatus.ASSIGNED, EmployeeAssetStatus.CONFIRMED));
        if (hasActive) {
            throw new RuntimeException("Asset is currently assigned and cannot be deleted!");
        }
        assetRepository.delete(asset);
    }

    // Zimmet atama
    public EmployeeAssetResponseDto assignAssetToEmployee(Long employeeId, AssignAssetRequestDto dto) {
        Asset asset = assetRepository.findById(dto.getAssetId())
                .orElseThrow(() -> new RuntimeException("Asset not found"));
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        // Aynı zimmet başka birine atanmış mı?
        boolean alreadyAssigned = employeeAssetRepository.existsByAssetAndStatusIn(
                asset, List.of(EmployeeAssetStatus.ASSIGNED, EmployeeAssetStatus.CONFIRMED));
        if (alreadyAssigned) {
            throw new RuntimeException("Asset is already assigned to another employee!");
        }
        // Şirket eşleştirmesi (opsiyonel kural)
        if (!asset.getCompany().equals(employee.getCompany())) {
            throw new RuntimeException("Asset and employee belong to different companies!");
        }

        EmployeeAsset ea = new EmployeeAsset();
        ea.setAsset(asset);
        ea.setEmployee(employee);
        ea.setAssignedDate(dto.getAssignedDate() != null ? dto.getAssignedDate() : LocalDate.now());
        ea.setStatus(EmployeeAssetStatus.ASSIGNED);
        EmployeeAsset saved = employeeAssetRepository.save(ea);
        return toEmployeeAssetDto(saved);
    }

    // Çalışanın zimmetleri
    public List<EmployeeAssetResponseDto> getEmployeeAssets(Long employeeId) {
        return employeeAssetRepository.findByEmployeeId(employeeId).stream()
                .map(this::toEmployeeAssetDto)
                .toList();
    }

    // Zimmeti onayla
    public EmployeeAssetResponseDto confirmEmployeeAsset(Long assignmentId) {
        EmployeeAsset ea = employeeAssetRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        if (ea.getStatus() != EmployeeAssetStatus.ASSIGNED) {
            throw new RuntimeException("Only ASSIGNED assets can be confirmed!");
        }
        ea.setStatus(EmployeeAssetStatus.CONFIRMED);
        ea.setEmployeeNote(null);
        return toEmployeeAssetDto(employeeAssetRepository.save(ea));
    }

    // Zimmeti reddet (not zorunlu)
    public EmployeeAssetResponseDto rejectEmployeeAsset(Long assignmentId, RejectAssetRequestDto dto) {
        EmployeeAsset ea = employeeAssetRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        if (ea.getStatus() != EmployeeAssetStatus.ASSIGNED) {
            throw new RuntimeException("Only ASSIGNED assets can be rejected!");
        }
        ea.setStatus(EmployeeAssetStatus.REJECTED);
        ea.setEmployeeNote(dto.getEmployeeNote());
        return toEmployeeAssetDto(employeeAssetRepository.save(ea));
    }

    // ------ DTO mapping yardımcı metodları ------
    private AssetResponseDto toAssetDto(Asset asset) {
        return AssetResponseDto.builder()
                .id(asset.getId())
                .name(asset.getName())
                .brand(asset.getBrand())
                .model(asset.getModel())
                .serialNumber(asset.getSerialNumber())
                .value(asset.getValue())
                .type(asset.getType())
                .companyId(asset.getCompany().getId())
                .build();
    }

    private EmployeeAssetResponseDto toEmployeeAssetDto(EmployeeAsset ea) {
        return EmployeeAssetResponseDto.builder()
                .id(ea.getId())
                .employeeNumber(ea.getEmployee().getEmployeeNumber())
                .employeeName(ea.getEmployee().getName())
                .assetName(ea.getAsset().getName())
                .assignedDate(ea.getAssignedDate())
                .status(ea.getStatus())
                .employeeNote(ea.getEmployeeNote())
                .build();
    }
}
