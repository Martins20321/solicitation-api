package com.martinsdev.solicitation.api.dto;

import java.time.LocalDate;

public record SolicitationSearchRequestDTO(String q,
                                           String status,
                                           String serviceType,
                                           String priority,
                                           String state,
                                           LocalDate dateFrom,
                                           LocalDate dateTo,
                                           Integer page,
                                           Integer size,
                                           String sort) {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public SolicitationSearchRequestDTO {
        if (page == null || page < 0 ) {
            page = 0;
        }
        if (size == null || size <= 0) {
            size = DEFAULT_SIZE;
        } else if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
    }
}
