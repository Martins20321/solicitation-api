package com.martinsdev.solicitation.api.service;

import com.martinsdev.solicitation.api.dto.DecisionRequestDTO;
import com.martinsdev.solicitation.api.dto.SolicitationResponseDTO;
import com.martinsdev.solicitation.api.infra.exception.ForbiddenException;
import com.martinsdev.solicitation.api.infra.exception.InvalidOperationException;
import com.martinsdev.solicitation.api.infra.exception.ResourceNotFoundException;
import com.martinsdev.solicitation.api.model.AnalystCoverage;
import com.martinsdev.solicitation.api.model.Solicitation;
import com.martinsdev.solicitation.api.model.User;
import com.martinsdev.solicitation.api.model.enums.Decision;
import com.martinsdev.solicitation.api.model.enums.StatusSolicitation;
import com.martinsdev.solicitation.api.repository.AnalystCoverageRepository;
import com.martinsdev.solicitation.api.repository.SolicitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnalystService {

    private final AnalystCoverageRepository converageRepository;
    private final SolicitationRepository solicitationRepository;

    // 1. GET /{id}
    public SolicitationResponseDTO getSolicitationById(Long id, User analyst) {
        Solicitation solicitation = findAndValidateAnalystCoverage(id, analyst);

        return new SolicitationResponseDTO(solicitation);
    }

    // 2. POST /{id}/start
    public SolicitationResponseDTO startReview(Long id, User analyst) {
        Solicitation solicitation = findAndValidateAnalystCoverage(id, analyst);

        if (solicitation.getStatus() != StatusSolicitation.SUBMITTED) {
            throw new InvalidOperationException("This operation can only be performed with the status 'SUBMITTED'");
        }

        solicitation.setStatus(StatusSolicitation.IN_REVIEW);

        solicitationRepository.save(solicitation);
        return new SolicitationResponseDTO(solicitation);
    }

    // 3. POST /{id}/decide
    public SolicitationResponseDTO decide(Long id, DecisionRequestDTO decisionRequestDTO, User analyst) {
        Solicitation solicitation = findAndValidateAnalystCoverage(id, analyst);

        if (solicitation.getStatus() != StatusSolicitation.IN_REVIEW) {
            throw new InvalidOperationException("You can only decide if the status is `IN_REVIEW`");
        }

        if (decisionRequestDTO.decision() == Decision.APPROVE) {
            solicitation.setStatus(StatusSolicitation.APPROVED);
        } else {
            solicitation.setStatus(StatusSolicitation.REJECTED);
        }

        solicitation.setAnalyzedAt(LocalDateTime.now());
        solicitation.setAnalyzedBy(analyst.getId());
        solicitation.setAnalysisComment(decisionRequestDTO.comment());

        solicitationRepository.save(solicitation);

        return new SolicitationResponseDTO(solicitation);
    }

    private Solicitation findAndValidateAnalystCoverage(Long id, User analyst) {
        Solicitation solicitation = solicitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitation not found by id: " + id));

        AnalystCoverage analystFound = converageRepository.findByUser(analyst)
                .orElseThrow(() -> new ResourceNotFoundException("AnalystCoverage not found"));

        // verifica se os estados do analyst abrangem o estado da solicitation
        if (!analystFound.getStates().contains(solicitation.getStepTwoData().getState())) {
            throw new ForbiddenException();
        }

        return solicitation;
    }
}
