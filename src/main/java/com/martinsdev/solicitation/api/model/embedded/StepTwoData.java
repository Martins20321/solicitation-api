package com.martinsdev.solicitation.api.model.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class StepTwoData {

    @Column(length = 9)
    private String cep;
    @Column(length = 20)
    private String number;
    @Column(length = 100)
    private String complement;
    @Column(length = 100)
    private String street;
    @Column(length = 80)
    private String neighborhood;
    @Column(length = 50)
    private String city;
    @Column(length = 2)
    private String state;
}
