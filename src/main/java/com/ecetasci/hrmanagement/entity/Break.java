package com.ecetasci.hrmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "breaks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Break {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer duration; // dakika cinsinden

    @ManyToOne
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;
}
