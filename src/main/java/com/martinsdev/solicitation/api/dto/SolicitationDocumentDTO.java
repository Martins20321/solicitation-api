package com.martinsdev.solicitation.api.dto;

import com.martinsdev.solicitation.api.model.document.SolicitationDocument;

import java.time.LocalDateTime;

public record SolicitationDocumentDTO(String id,
                                      String status,
                                      String serviceType,
                                      String priority,
                                      String state,
                                      String title,
                                      String description,
                                      LocalDateTime createdAt) {
    public SolicitationDocumentDTO(SolicitationDocument document) {
        this(document.getId(), document.getStatus(), document.getServiceType(), document.getPriority(),
                document.getState(), document.getTitle(), document.getDescription(), document.getCreatedAt());
    }
}
