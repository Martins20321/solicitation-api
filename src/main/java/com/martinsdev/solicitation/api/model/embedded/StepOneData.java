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

    @Column(name = "service_type")
    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;
    private String title;
    private String description;
}
