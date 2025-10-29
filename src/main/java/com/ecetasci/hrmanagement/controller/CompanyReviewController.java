package com.ecetasci.hrmanagement.controller;

import com.ecetasci.hrmanagement.dto.request.UpdateCompanyReviewRequest;
import com.ecetasci.hrmanagement.dto.response.BaseResponse;
import com.ecetasci.hrmanagement.dto.response.CompanyReviewResponse;
import com.ecetasci.hrmanagement.dto.response.ReviewDetailResponse;
import com.ecetasci.hrmanagement.entity.CompanyReview;
import com.ecetasci.hrmanagement.entity.User;
import com.ecetasci.hrmanagement.enums.Role;
import com.ecetasci.hrmanagement.exceptions.UnauthorizedException;
import com.ecetasci.hrmanagement.service.CompanyReviewService;
import com.ecetasci.hrmanagement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.ecetasci.hrmanagement.constant.Endpoints.REVIEWS;

/**
 * CompanyReviewController — şirket incelemeleri (reviews) ile ilgili endpointler.
 *
 * Sağladığı işlevler:
 * - Halka açık inceleme listeleme
 * - Site admin tarafından inceleme yayınlama
 * - İnceleme silme, güncelleme ve detay görüntüleme
 */
@RestController
@RequestMapping(REVIEWS)
@RequiredArgsConstructor
public class CompanyReviewController {

    private final CompanyReviewService companyReviewService;
    private final UserService userService;

    /**
     * Belirtilen şirketin tüm genel (public) incelemelerini listeler.
     *
     * @param companyId (opsiyonel) filtreleme için kullanılabilir
     * @return İnceleme listesi içeren BaseResponse
     */
    @GetMapping("/public")
    public ResponseEntity<BaseResponse<List<CompanyReviewResponse>>> listReview(Long companyId) {
        List<CompanyReviewResponse> all = companyReviewService.findAll();

        return ResponseEntity.ok(BaseResponse.<List<CompanyReviewResponse>>builder()
                .success(true)
                .code(200)
                .data(all)
                .build());
    }

    /**
     * Site admin yetkisine sahip kullanıcı tarafından yeni bir inceleme (review) oluşturur ve yayınlar.
     *
     * @param userId İncelemeyi yayınlayan kullanıcı ID'si
     * @param companyId İnceleme yapılacak şirket ID'si
     * @param title İnceleme başlığı
     * @param content İnceleme içeriği
     * @param rating Puan (ör. 1-5)
     * @return Oluşturulan CompanyReviewResponse içeren BaseResponse
     */
    @PostMapping("admin/publish")
    public ResponseEntity<BaseResponse<CompanyReviewResponse>> createReview(Long userId, Long companyId, String title,
                                                                            String content, Integer rating) {
        User user = userService.findById(userId).orElseThrow();
        if (user.getRole().equals(Role.SITE_ADMIN)) {
            CompanyReviewResponse companyReview = companyReviewService.createCompanyReview(companyId, title, content, rating);
            return ResponseEntity.ok(BaseResponse.<CompanyReviewResponse>builder().success(true).code(200).data(companyReview)
                    .build());
        } else {
            throw new UnauthorizedException("Admin değilsiniz");
        }
    }


    /**
     * Şirket incelemesini siler.
     *
     * @param companyId Silinecek incelemenin ait olduğu şirket ID'si
     * @return Başarı mesajı içeren BaseResponse
     */
    @DeleteMapping("/admin/delete")
    public ResponseEntity<BaseResponse<String>> deleteReview(@RequestParam Long companyId) {
        companyReviewService.deleteWithId(companyId);
        return ResponseEntity.ok(BaseResponse.<String>builder()
                .success(true)
                .code(200)
                .data("Company review deleted succesfully")
                .build());
    }

    /**
     * Mevcut bir incelemeyi günceller.
     *
     * @param updateCompanyReviewRequest Güncelleme için gerekli verileri taşıyan DTO
     * @return Başarı mesajı içeren BaseResponse
     */
    @PutMapping("/company/update-review")
    public ResponseEntity<BaseResponse<String>> updateReview(@RequestBody @Valid UpdateCompanyReviewRequest updateCompanyReviewRequest) {

        CompanyReview companyReview = companyReviewService.findByCompanyId(updateCompanyReviewRequest.companyId());

        companyReview.setTitle(updateCompanyReviewRequest.title());
        companyReview.setContent(updateCompanyReviewRequest.content());
        companyReview.setRating(updateCompanyReviewRequest.rating());

        CompanyReview saved = companyReviewService.save(companyReview);

        return ResponseEntity.ok(BaseResponse.<String>builder()
                .success(true)
                .code(200)
                .data("Review updated succesfully").build());


    }


    /**
     * İnceleme detaylarını döner.
     *
     * @param reviewId İnceleme ID'si
     * @return ReviewDetailResponse içeren BaseResponse
     */
    @PostMapping("/company/review_detail")
    public ResponseEntity<BaseResponse<ReviewDetailResponse>> reviewDetail(@RequestParam
                                                                           Long reviewId) {
        ReviewDetailResponse reviewDetailResponse = companyReviewService.findById(reviewId);
        return ResponseEntity.ok(BaseResponse.<ReviewDetailResponse>builder()
                .success(true).code(200).message("yorum detayları")
                .data(reviewDetailResponse)
                .build());
    }
}
