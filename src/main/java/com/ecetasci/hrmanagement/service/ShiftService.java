package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.request.ShiftRequestDto;
import com.ecetasci.hrmanagement.dto.response.ShiftResponseDto;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.Shift;
import com.ecetasci.hrmanagement.exceptions.BusinessException;
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;
import com.ecetasci.hrmanagement.mapper.ShiftMapper;
import com.ecetasci.hrmanagement.repository.CompanyRepository;
import com.ecetasci.hrmanagement.repository.EmployeeShiftRepository;
import com.ecetasci.hrmanagement.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final CompanyRepository companyRepository;
    private final ShiftMapper shiftMapper;
    private final EmployeeShiftRepository employeeShiftRepository; // eklendi




    public List<ShiftResponseDto> getShiftsByCompany(Long companyId) {
        List<Shift> shifts = shiftRepository.findByCompanyId(companyId);
        return shifts.stream()
                .map(shiftMapper::toDto)
                .toList();
    }


    public ShiftResponseDto createShift(ShiftRequestDto dto) {
        // DTO → Entity
        Shift shift = shiftMapper.toEntity(dto);

        // id’den şirketi bul ve set et
        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        shift.setCompany(company);

        // çakışma kontrolü
        validateShiftOverlap(shift);

        // Kaydet ve DTO olarak geri döndür
        Shift saved = shiftRepository.save(shift);

        ShiftResponseDto dto1 = shiftMapper.toDto(saved);
       dto1.setCompanyId(company.getId());

        return dto1;



    }

    // Vardiya güncelleme
    public ShiftResponseDto updateShift(Long id, ShiftRequestDto dto) {
        Shift existing = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found"));

        // DTO → Entity (maple)
        Shift updatedShift = shiftMapper.toEntity(dto);
        updatedShift.setId(existing.getId());

        // Company’yi yine set et
        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        updatedShift.setCompany(company);

        validateShiftOverlap(updatedShift);

        Shift saved = shiftRepository.save(updatedShift);
        return shiftMapper.toDto(saved);
    }


    public void deleteShift(Long id) {
        // varlık yoksa hata at
      shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found"));

        // 1) Önce EmployeeShift tablosunda ilişkili kayıtları sil (FK kısıtlamasını önlemek için)
        employeeShiftRepository.deleteByShiftId(id);

        // 2) Sonra Shift'i sil
        shiftRepository.deleteById(id);
    }

    // ========== BUSINESS RULES ==========
    private void validateShiftOverlap(Shift newShift) {
        List<Shift> companyShifts = shiftRepository.findByCompanyId(newShift.getCompany().getId());

        boolean overlaps = companyShifts.stream()
                .filter(s -> !s.getId().equals(newShift.getId())) // güncellemelerde kendisini hariç tut
                .anyMatch(s -> isOverlapping(s, newShift));

        if (overlaps) {
            throw new BusinessException("Vardiya saatleri çakışıyor!");
        }
    }

    private boolean isOverlapping(Shift s1, Shift s2) {
        return s1.getStartTime().isBefore(s2.getEndTime()) &&
                s2.getStartTime().isBefore(s1.getEndTime());
    }
}
