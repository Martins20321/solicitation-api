package com.martinsdev.solicitation.api.model.embedded;

import com.martinsdev.solicitation.api.model.enums.ServiceType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class StepOneData {

    @Column(name = "service_type", length = 30)
    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;
    @Column(length = 80)
    private String title;
    @Column(length = 1000)
    private String description;
}
