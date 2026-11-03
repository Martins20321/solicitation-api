package com.martinsdev.solicitation.api.controller;

import com.martinsdev.solicitation.api.dto.*;
import com.martinsdev.solicitation.api.model.User;
import com.martinsdev.solicitation.api.service.AnalystService;
import com.martinsdev.solicitation.api.service.SolicitationSearchService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analyst/solicitations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-key")
public class AnalystController {

    private final AnalystService service;
    private final SolicitationSearchService solicitationSearchService;

    @GetMapping("/{id}")
    public ResponseEntity<SolicitationDetailResponseDTO> getSolicitationById(@PathVariable Long id, @AuthenticationPrincipal User analyst) {
        return ResponseEntity.ok(service.getSolicitationById(id, analyst));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<SolicitationResponseDTO> start(@PathVariable Long id, @AuthenticationPrincipal User analyst) {
        return ResponseEntity.ok(service.startReview(id, analyst));
    }

    @PostMapping("/{id}/decide")
    public ResponseEntity<SolicitationResponseDTO> decide(@PathVariable Long id, @RequestBody @Valid DecisionRequestDTO decisionRequestDTO, @AuthenticationPrincipal User analyst) {
        return ResponseEntity.ok(service.decide(id, decisionRequestDTO, analyst));
    }

    @GetMapping("/search")
    public ResponseEntity<SolicitationSearchResponseDTO> search(@ModelAttribute SolicitationSearchRequestDTO searchRequestDTO, @AuthenticationPrincipal User analyst) {
        return ResponseEntity.ok(solicitationSearchService.search(searchRequestDTO, analyst));
    }
}
