package com.ecetasci.hrmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ExpenseDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //expense, fileName, filePath, fileType, uploadDate

    private String fileName;

    private  String filePath;

    private String fileType;

    private LocalDate uploadDate;

    @ManyToOne(fetch = FetchType.LAZY)
    private Expense expense;


}
