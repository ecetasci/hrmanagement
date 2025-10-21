package com.ecetasci.hrmanagement.repository;

import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company,Long> {

    Boolean existsCompanyByCompanyName(String companyName);

    Optional<List<Company>> findAllByCompanyEmail(String email);

    Optional<List<Company>> findByAddress(String address);

    //null olmasın diye primitive long
    @Query("SELECT COUNT(c) FROM Company c")
    long countActiveCompanies();
}
