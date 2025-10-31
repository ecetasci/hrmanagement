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
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;
import com.ecetasci.hrmanagement.repository.UserRepository;
import com.ecetasci.hrmanagement.utility.JwtManager;
import jakarta.servlet.UnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import com.ecetasci.hrmanagement.exceptions.UnauthorizedException;
import com.ecetasci.hrmanagement.exceptions.ForbiddenException;

import static com.ecetasci.hrmanagement.enums.EmployeeAssetStatus.CONFIRMED;

@Service
@RequiredArgsConstructor
public class AssetService {
    private final AssetRepository assetRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeAssetRepository employeeAssetRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final JwtManager jwtManager;

    // Tüm zimmetler
    public List<AssetResponseDto> getAllAssets() {
        return assetRepository.findAll().stream()
                .map(this::toAssetDto)
                .toList();
    }

    // Şirkete ait zimmetleri getirir
    public List<AssetResponseDto> getAssetsByCompanyId(Long companyId) {
        return assetRepository.findAllByCompanyId(companyId).stream()
                .map(this::toAssetDto)
                .toList();
    }

    // Çağıranın Authorization header'ından companyId çözümler ve ilgili asset'leri döner
    public List<AssetResponseDto> getAssetsForCaller(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or malformed Authorization header");
        }
        String token = auth.substring(7);
        String username;
        try {
            username = jwtManager.extractUsername(token);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid token");
        }
        if (username == null) throw new UnauthorizedException("Unauthorized");

        var userOpt = userRepository.findUserByEmail(username);
        if (userOpt.isEmpty()) throw new UnauthorizedException("User not found");
        var user = userOpt.get();

        var empOpt = employeeRepository.findByUserId(user.getId());
        Long companyId = empOpt.map(emp -> emp.getCompany() != null ? emp.getCompany().getId() : null).orElse(null);
        if (companyId == null) throw new ForbiddenException("Caller has no company");

        return getAssetsByCompanyId(companyId);
    }

    // Zimmet oluşturma (serial number unique kontrolü)
    public AssetResponseDto createAsset(AssetRequestDto dto) {
        if (assetRepository.existsBySerialNumber(dto.getSerialNumber())) {
            throw new IllegalArgumentException("Serial number already exists!");
        }
        Asset asset = new Asset();
        asset.setName(dto.getName());
        asset.setBrand(dto.getBrand());
        asset.setModel(dto.getModel());
        asset.setSerialNumber(dto.getSerialNumber());
        asset.setValue(dto.getValue());
        asset.setType(dto.getType());
        asset.setCompany(companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found")));
        Asset saved = assetRepository.save(asset);
        return toAssetDto(saved);
    }

    // Zimmet güncelleme//tekrar bak
    public AssetResponseDto updateAsset(Long id, AssetRequestDto dto) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
        asset.setName(dto.getName());
        asset.setBrand(dto.getBrand());
        asset.setModel(dto.getModel());
        asset.setSerialNumber(dto.getSerialNumber());
        asset.setValue(dto.getValue());
        asset.setType(dto.getType());
        asset.setCompany(companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found")));
        return toAssetDto(assetRepository.save(asset));
    }

    // Zimmet silme
    public void deleteAsset(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
        // Aktif zimmet varsa silinemez
        boolean hasActive = employeeAssetRepository.existsByAssetAndStatusIn(
                asset, List.of(EmployeeAssetStatus.ASSIGNED, CONFIRMED));
        if (hasActive) {
            throw new IllegalStateException("Asset is currently assigned and cannot be deleted!");
        }
        assetRepository.delete(asset);
    }

    // Zimmet atama
    public EmployeeAssetResponseDto assignAssetToEmployee(Long employeeId, AssignAssetRequestDto dto) {
        Asset asset = assetRepository.findById(dto.getAssetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        // Aynı zimmet başka birine atanmış mı?
        boolean alreadyAssigned = employeeAssetRepository.existsByAssetAndStatusIn(
                asset, List.of(EmployeeAssetStatus.ASSIGNED, CONFIRMED));
        if (alreadyAssigned) {
            throw new IllegalStateException("Asset is already assigned to another employee!");
        }
        // Şirket eşleştirmesi (opsiyonel kural)
        if (!asset.getCompany().equals(employee.getCompany())) {
            throw new IllegalStateException("Asset and employee belong to different companies!");
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
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        if (ea.getStatus() != EmployeeAssetStatus.ASSIGNED) {
            throw new IllegalStateException("Only ASSIGNED assets can be confirmed!");
        }
        ea.setStatus(CONFIRMED);
        ea.setEmployeeNote(null);
        return toEmployeeAssetDto(employeeAssetRepository.save(ea));
    }

    // Zimmeti reddet (not zorunlu)
    public EmployeeAssetResponseDto rejectEmployeeAsset(Long assignmentId, RejectAssetRequestDto dto) {
        EmployeeAsset asset = employeeAssetRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        if(asset.getStatus().equals(CONFIRMED)){
            throw new RuntimeException("Confirmed assets cannot be rejected!");
        }
        if (asset.getStatus() != EmployeeAssetStatus.ASSIGNED ) {
            throw new IllegalStateException("Only ASSIGNED assets can be rejected!");
        }
        asset.setStatus(EmployeeAssetStatus.REJECTED);
        asset.setEmployeeNote(dto.getEmployeeNote());
        return toEmployeeAssetDto(employeeAssetRepository.save(asset));
    }

    // Yeni: bir assignment'ın belirli bir employee'ye ait olup olmadığını kontrol eder
    public boolean assignmentBelongsToEmployee(Long assignmentId, Long employeeId) {
        EmployeeAsset ea = employeeAssetRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        var emp = ea.getEmployee();
        return emp != null && emp.getId() != null && emp.getId().equals(employeeId);
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

    public boolean assetBelongsToCompany(Long id, Long callerCompanyId) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        return asset.getCompany().getId().equals(callerCompanyId);
    }
}
