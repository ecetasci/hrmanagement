package com.ecetasci.hrmanagement.repository;

import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.CompanySubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanySubscriptionRepository extends JpaRepository<CompanySubscription, Long> {

    Optional<CompanySubscription> findCompanySubscriptionByCompany(Company company);

    @Query("SELECT s FROM CompanySubscription s WHERE s.endDate IS NOT NULL AND s.endDate BETWEEN CURRENT_DATE AND :limitDate")
    List<CompanySubscription> findExpiringSubs(@Param("limitDate") LocalDate limitDate);

    List<CompanySubscription> findByEndDateBetween(LocalDate start, LocalDate end);
   // Optional<CompanySubscription> findCompanySubscriptionByCompany(String companyName);


}
