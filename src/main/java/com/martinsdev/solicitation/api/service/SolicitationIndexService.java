package com.martinsdev.solicitation.api.service;

import com.martinsdev.solicitation.api.model.Solicitation;
import com.martinsdev.solicitation.api.model.document.SolicitationDocument;
import com.martinsdev.solicitation.api.repository.SolicitationSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SolicitationIndexService {

    private final SolicitationSearchRepository searchRepository;

    public void index(Solicitation solicitation) {
        SolicitationDocument document = SolicitationDocument.builder()
                .id(String.valueOf(solicitation.getId()))
                .status(String.valueOf(solicitation.getStatus()))
                // steps - podem nao estar preenchido
                .serviceType(solicitation.getStepOneData()!= null ? String.valueOf(solicitation.getStepOneData().getServiceType()) : "Ainda não preenchido")
                .priority(solicitation.getStepThreeData() != null ? String.valueOf(solicitation.getStepThreeData().getPriority()) : "Ainda não preenchido")
                .state(solicitation.getStepTwoData() != null ? solicitation.getStepTwoData().getState() : "Ainda não preenchido")
                .title(solicitation.getStepOneData() != null ? solicitation.getStepOneData().getTitle() : "Ainda não preenchido")
                .description(solicitation.getStepOneData() != null ? solicitation.getStepOneData().getDescription() : "Ainda não preechido")
                .createdAt(solicitation.getCreatedAt())
                .build();
        searchRepository.save(document);
    }
}
