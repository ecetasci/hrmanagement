package com.ecetasci.hrmanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table
public class CompanyReview {
    //company (OneToOne), title, content, rating (1-5),
    //isPublished

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @OneToOne
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @Column
    private String title;

    @Column
    private String content;

    @Min(1)
    @Max(5)
    @Column()
    private Integer rating;

    @Column
    private boolean isPublished;


}
