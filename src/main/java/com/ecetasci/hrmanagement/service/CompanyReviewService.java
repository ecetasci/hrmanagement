package com.ecetasci.hrmanagement.service;

import com.ecetasci.hrmanagement.dto.response.CompanyReviewResponse;
import com.ecetasci.hrmanagement.dto.response.ReviewDetailResponse;
import com.ecetasci.hrmanagement.entity.Company;
import com.ecetasci.hrmanagement.entity.CompanyReview;
import com.ecetasci.hrmanagement.repository.CompanyRepository;
import com.ecetasci.hrmanagement.repository.CompanyReviewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import com.ecetasci.hrmanagement.exceptions.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class CompanyReviewService {

    private final CompanyReviewRepository companyReviewRepository;
    private final CompanyRepository companyRepository;

    public List<CompanyReviewResponse> findAll() {

        List<CompanyReview> companyReviews = companyReviewRepository.findAll();

        return companyReviews.stream().map(companyReview -> new CompanyReviewResponse(companyReview.getCompany().getCompanyName(),
                companyReview.getTitle(), companyReview.getContent(), companyReview.getRating())).toList();


    }


    public boolean isPublished(Long id) {

        Integer i = companyReviewRepository.countCompanyReviewByCompany_Id(id);
        if (i.equals(0) || i < 0) {
            return false;

        } else return true;
    }

    //dönüşü dto yaparım
    public CompanyReviewResponse createCompanyReview(Long id, String title, String content, Integer rating) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));

        if (!isPublished(id)) {
            CompanyReview companyReview = new CompanyReview();
            companyReview.setCompany(company);
            companyReview.setTitle(title);
            companyReview.setContent(content);
            companyReview.setRating(rating);
            CompanyReview saved = companyReviewRepository.save(companyReview);
            //company tablosuna da eklencek mi bak

            return new CompanyReviewResponse(saved.getCompany().getCompanyName(),
                    saved.getTitle(), saved.getContent(), saved.getRating());
        }
        throw new IllegalStateException("Daha önce yorum yapıldığından yorum eklenemez");
    }

    @Transactional
    public void deleteWithId(Long companyId) {
        companyReviewRepository.deleteCompanyReviewByCompany_Id(companyId);
    }

    public CompanyReview findByCompanyId(Long companyId) {
        return companyReviewRepository.findCompanyReviewByCompany_Id(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyReview not found for company id: " + companyId));
    }

    public CompanyReview save(CompanyReview companyReview) {
        return companyReviewRepository.save(companyReview);

    }

    public ReviewDetailResponse findById(Long reviewId) {
        CompanyReview companyReview = companyReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyReview not found with id: " + reviewId));
        ReviewDetailResponse reviewDetailResponse = new ReviewDetailResponse(companyReview.getCompany().getCompanyName(),
                companyReview.getTitle(), companyReview.getContent(), companyReview.getRating());

        return reviewDetailResponse;

    }
}
