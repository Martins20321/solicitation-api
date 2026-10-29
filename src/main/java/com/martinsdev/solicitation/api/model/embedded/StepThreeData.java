package com.martinsdev.solicitation.api.model.embedded;

import com.martinsdev.solicitation.api.model.enums.Priority;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class StepThreeData {

    @Enumerated(EnumType.STRING)
    private Priority priority;
    @Column(name = "preferred_date")
    private LocalDate preferredDate;
    @Column(name = "estimated_value")
    private BigDecimal estimatedValue;
    @Column(name = "terms_accepted")
    private Boolean termsAccepted;
}
