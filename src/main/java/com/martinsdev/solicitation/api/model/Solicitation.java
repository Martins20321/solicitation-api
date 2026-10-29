package com.martinsdev.solicitation.api.model;

import com.martinsdev.solicitation.api.model.embedded.StepOneData;
import com.martinsdev.solicitation.api.model.embedded.StepThreeData;
import com.martinsdev.solicitation.api.model.embedded.StepTwoData;
import com.martinsdev.solicitation.api.model.enums.StatusSolicitation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_solicitations")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "id")
public class Solicitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private StatusSolicitation status;
    @Column(name = "current_step", nullable = false)
    private Integer currentStep;

    // -- STEPS --
    @Embedded
    private StepOneData stepOneData;
    @Embedded
    private StepTwoData stepTwoData;
    @Embedded
    private StepThreeData stepThreeData;

    // -- AUDIT --
    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;
    @Column(name = "analyzed_by")
    private Long analyzedBy;
    @Column(name = "analysis_comment", length = 250)
    private String analysisComment;

}
