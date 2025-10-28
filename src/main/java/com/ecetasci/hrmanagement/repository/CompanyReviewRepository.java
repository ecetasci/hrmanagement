package com.ecetasci.hrmanagement.repository;

import com.ecetasci.hrmanagement.entity.CompanyReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyReviewRepository extends JpaRepository<CompanyReview,Long> {

    Optional<CompanyReview> findCompanyReviewByCompany_CompanyName(String name);

    Optional<CompanyReview> findCompanyReviewByCompany_Id(Long id);

    Integer countCompanyReviewByCompany_Id(Long id);

    void deleteCompanyReviewByCompany_Id(Long id);

   // void deleteByCompanyId(Long id);

}
