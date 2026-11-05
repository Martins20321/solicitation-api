package com.martinsdev.solicitation.api.service;

import com.martinsdev.solicitation.api.dto.DecisionRequestDTO;
import com.martinsdev.solicitation.api.dto.SolicitationDetailResponseDTO;
import com.martinsdev.solicitation.api.dto.SolicitationResponseDTO;
import com.martinsdev.solicitation.api.infra.exception.ForbiddenException;
import com.martinsdev.solicitation.api.infra.exception.InvalidOperationException;
import com.martinsdev.solicitation.api.infra.exception.ResourceNotFoundException;
import com.martinsdev.solicitation.api.model.AnalystCoverage;
import com.martinsdev.solicitation.api.model.Solicitation;
import com.martinsdev.solicitation.api.model.User;
import com.martinsdev.solicitation.api.model.embedded.StepTwoData;
import com.martinsdev.solicitation.api.model.enums.Decision;
import com.martinsdev.solicitation.api.model.enums.RoleUser;
import com.martinsdev.solicitation.api.model.enums.StatusSolicitation;
import com.martinsdev.solicitation.api.repository.AnalystCoverageRepository;
import com.martinsdev.solicitation.api.repository.SolicitationRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalystServiceTest {

    @InjectMocks
    private AnalystService service;

    @Mock
    private AnalystCoverageRepository coverageRepository;

    @Mock
    private SolicitationRepository solicitationRepository;

    @Mock
    private SolicitationIndexService indexService;

    @Captor
    private ArgumentCaptor<Solicitation> solicitationCaptor;

    // ------------------------------------------------ GET BY ID ------------------------------------------------

    @Test
    @DisplayName("It should return the solicitation when the state is covered by the analyst")
    void verificationSucessAnalystGetById() {

        //ARRANGE
        User analyst = User.builder().id(5L).role(RoleUser.ANALYST).build();
        Solicitation solicitation = Solicitation.builder().id(10L).status(StatusSolicitation.SUBMITTED).currentStep(3)
                .stepTwoData(StepTwoData.builder().state("SP").build()).build();
        AnalystCoverage coverage = AnalystCoverage.builder().user(analyst).states(Set.of("SP", "RJ")).build();

        when(solicitationRepository.findById(10L)).thenReturn(Optional.of(solicitation));
        when(coverageRepository.findByUser(analyst)).thenReturn(Optional.of(coverage));

        //ACT
        SolicitationDetailResponseDTO result = service.getSolicitationById(10L, analyst);

        //ASSERT
        Assertions.assertEquals(10L, result.id());
        Assertions.assertEquals("SP", result.stepTwo().state());
    }

    @Test
    @DisplayName("It should let the ADMIN see any solicitation without looking for a coverage")
    void verificationSucessAnalystGetByIdAsAdmin() {

        //ARRANGE
        User admin = User.builder().id(1L).role(RoleUser.ADMIN).build();
        Solicitation solicitation = Solicitation.builder().id(10L).status(StatusSolicitation.SUBMITTED).currentStep(3)
                .stepTwoData(StepTwoData.builder().state("MG").build()).build();

        when(solicitationRepository.findById(10L)).thenReturn(Optional.of(solicitation));

        //ACT
        SolicitationDetailResponseDTO result = service.getSolicitationById(10L, admin);

        //ASSERT
        Assertions.assertEquals(10L, result.id());
        then(coverageRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("It should throw an exception when the solicitation does not exist")
    void verificationErrorAnalystGetByIdNotFound() {

        //ARRANGE
        User analyst = User.builder().id(5L).role(RoleUser.ANALYST).build();

        when(solicitationRepository.findById(10L)).thenReturn(Optional.empty());

        //ASSERT + ACT
        ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
                () -> service.getSolicitationById(10L, analyst));
        Assertions.assertEquals("Solicitation not found by id: 10", exception.getMessage());
    }

    @Test
    @DisplayName("It should hide a DRAFT from the analyst as if it did not exist")
    void verificationErrorAnalystGetByIdDraft() {

        //ARRANGE
        User analyst = User.builder().id(5L).role(RoleUser.ANALYST).build();
        Solicitation solicitation = Solicitation.builder().id(10L).status(StatusSolicitation.DRAFT).currentStep(1).build();

        when(solicitationRepository.findById(10L)).thenReturn(Optional.of(solicitation));

        //ASSERT + ACT
        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.getSolicitationById(10L, analyst));
        then(coverageRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("It should throw an exception when the analyst has no coverage registered")
    void verificationErrorAnalystGetByIdWithoutCoverage() {

        //ARRANGE
        User analyst = User.builder().id(5L).role(RoleUser.ANALYST).build();
        Solicitation solicitation = Solicitation.builder().id(10L).status(StatusSolicitation.SUBMITTED).currentStep(3)
                .stepTwoData(StepTwoData.builder().state("SP").build()).build();

        when(solicitationRepository.findById(10L)).thenReturn(Optional.of(solicitation));
        when(coverageRepository.findByUser(analyst)).thenReturn(Optional.empty());

        //ASSERT + ACT
        ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
                () -> service.getSolicitationById(10L, analyst));
        Assertions.assertEquals("AnalystCoverage not found", exception.getMessage());
    }

    @Test
    @DisplayName("It should throw an exception when the state is outside the coverage of the analyst")
    void verificationErrorAnalystGetByIdOutsideCoverage() {

        //ARRANGE
        User analyst = User.builder().id(5L).role(RoleUser.ANALYST).build();
        Solicitation solicitation = Solicitation.builder().id(10L).status(StatusSolicitation.SUBMITTED).currentStep(3)
                .stepTwoData(StepTwoData.builder().state("MG").build()).build();
        AnalystCoverage coverage = AnalystCoverage.builder().user(analyst).states(Set.of("SP", "RJ")).build();

        when(solicitationRepository.findById(10L)).thenReturn(Optional.of(solicitation));
        when(coverageRepository.findByUser(analyst)).thenReturn(Optional.of(coverage));

        //ASSERT + ACT
        ForbiddenException exception = Assertions.assertThrows(ForbiddenException.class,
                () -> service.getSolicitationById(10L, analyst));
        Assertions.assertEquals("Access denied: this solicitation is outside your state coverage", exception.getMessage());
    }

    // ------------------------------------------------ START REVIEW ------------------------------------------------

    @Test
    @DisplayName("It should change the status from SUBMITTED to IN_REVIEW when starting the review")
    void verificationSucessAnalystStartReview() {

        //ARRANGE
        User analyst = User.builder().id(5L).role(RoleUser.ANALYST).build();
        Solicitation solicitation = Solicitation.builder().id(10L).status(StatusSolicitation.SUBMITTED).currentStep(3)
                .stepTwoData(StepTwoData.builder().state("SP").build()).build();
        AnalystCoverage coverage = AnalystCoverage.builder().user(analyst).states(Set.of("SP")).build();

        when(solicitationRepository.findById(10L)).thenReturn(Optional.of(solicitation));
        when(coverageRepository.findByUser(analyst)).thenReturn(Optional.of(coverage));

        //ACT
        SolicitationResponseDTO result = service.startReview(10L, analyst);

        //ASSERT
        then(solicitationRepository).should().save(solicitationCaptor.capture());
        Assertions.assertEquals(StatusSolicitation.IN_REVIEW, solicitationCaptor.getValue().getStatus());
        Assertions.assertEquals(StatusSolicitation.IN_REVIEW, result.status());
        then(indexService).should().index(solicitationCaptor.getValue());
    }

    @Test
    @DisplayName("It should not start the review when the status is not SUBMITTED")
    void verificationErrorAnalystStartReviewWrongStatus() {

        //ARRANGE
        User analyst = User.builder().id(5L).role(RoleUser.ANALYST).build();
        Solicitation solicitation = Solicitation.builder().id(10L).status(StatusSolicitation.IN_REVIEW).currentStep(3)
                .stepTwoData(StepTwoData.builder().state("SP").build()).build();
        AnalystCoverage coverage = AnalystCoverage.builder().user(analyst).states(Set.of("SP")).build();

        when(solicitationRepository.findById(10L)).thenReturn(Optional.of(solicitation));
        when(coverageRepository.findByUser(analyst)).thenReturn(Optional.of(coverage));

        //ASSERT + ACT
        InvalidOperationException exception = Assertions.assertThrows(InvalidOperationException.class,
                () -> service.startReview(10L, analyst));
        then(solicitationRepository).should(Mockito.never()).save(any());
        Assertions.assertEquals("This operation can only be performed with the status 'SUBMITTED'", exception.getMessage());
    }

    @Test
    @DisplayName("It should not start the review when the state is outside the coverage of the analyst")
    void verificationErrorAnalystStartReviewOutsideCoverage() {

        //ARRANGE
        User analyst = User.builder().id(5L).role(RoleUser.ANALYST).build();
        Solicitation solicitation = Solicitation.builder().id(10L).status(StatusSolicitation.SUBMITTED).currentStep(3)
                .stepTwoData(StepTwoData.builder().state("MG").build()).build();
        AnalystCoverage coverage = AnalystCoverage.builder().user(analyst).states(Set.of("SP")).build();

        when(solicitationRepository.findById(10L)).thenReturn(Optional.of(solicitation));
        when(coverageRepository.findByUser(analyst)).thenReturn(Optional.of(coverage));

        //ASSERT + ACT
        Assertions.assertThrows(ForbiddenException.class, () -> service.startReview(10L, analyst));
        then(solicitationRepository).should(Mockito.never()).save(any());
    }

    // ------------------------------------------------ DECIDE ------------------------------------------------

    @Test
    @DisplayName("It should approve the solicitation and register who analyzed it")
    void verificationSucessAnalystDecideApprove() {

        //ARRANGE
        User analyst = User.builder().id(5L).role(RoleUser.ANALYST).build();
        Solicitation solicitation = Solicitation.builder().id(10L).status(StatusSolicitation.IN_REVIEW).currentStep(3)
                .stepTwoData(StepTwoData.builder().state("SP").build()).build();
        AnalystCoverage coverage = AnalystCoverage.builder().user(analyst).states(Set.of("SP")).build();
        DecisionRequestDTO decisionDTO = new DecisionRequestDTO(Decision.APPROVE, "Everything is in order, approved");

        when(solicitationRepository.findById(10L)).thenReturn(Optional.of(solicitation));
        when(coverageRepository.findByUser(analyst)).thenReturn(Optional.of(coverage));

        //ACT
        SolicitationResponseDTO result = service.decide(10L, decisionDTO, analyst);

        //ASSERT
        then(solicitationRepository).should().save(solicitationCaptor.capture());
        Solicitation saved = solicitationCaptor.getValue();

        Assertions.assertEquals(StatusSolicitation.APPROVED, saved.getStatus());
        Assertions.assertEquals(5L, saved.getAnalyzedBy());
        Assertions.assertEquals("Everything is in order, approved", saved.getAnalysisComment());
        Assertions.assertNotNull(saved.getAnalyzedAt());
        Assertions.assertEquals(StatusSolicitation.APPROVED, result.status());
        then(indexService).should().index(saved);
    }

    @Test
    @DisplayName("It should reject the solicitation when the decision is REJECT")
    void verificationSucessAnalystDecideReject() {

        //ARRANGE
        User analyst = User.builder().id(5L).role(RoleUser.ANALYST).build();
        Solicitation solicitation = Solicitation.builder().id(10L).status(StatusSolicitation.IN_REVIEW).currentStep(3)
                .stepTwoData(StepTwoData.builder().state("SP").build()).build();
        AnalystCoverage coverage = AnalystCoverage.builder().user(analyst).states(Set.of("SP")).build();
        DecisionRequestDTO decisionDTO = new DecisionRequestDTO(Decision.REJECT, "Missing information about the address");

        when(solicitationRepository.findById(10L)).thenReturn(Optional.of(solicitation));
        when(coverageRepository.findByUser(analyst)).thenReturn(Optional.of(coverage));

        //ACT
        service.decide(10L, decisionDTO, analyst);

        //ASSERT
        then(solicitationRepository).should().save(solicitationCaptor.capture());
        Assertions.assertEquals(StatusSolicitation.REJECTED, solicitationCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("It should not decide when the status is not IN_REVIEW")
    void verificationErrorAnalystDecideWrongStatus() {

        //ARRANGE
        User analyst = User.builder().id(5L).role(RoleUser.ANALYST).build();
        Solicitation solicitation = Solicitation.builder().id(10L).status(StatusSolicitation.SUBMITTED).currentStep(3)
                .stepTwoData(StepTwoData.builder().state("SP").build()).build();
        AnalystCoverage coverage = AnalystCoverage.builder().user(analyst).states(Set.of("SP")).build();
        DecisionRequestDTO decisionDTO = new DecisionRequestDTO(Decision.APPROVE, "Approving before starting the review");

        when(solicitationRepository.findById(10L)).thenReturn(Optional.of(solicitation));
        when(coverageRepository.findByUser(analyst)).thenReturn(Optional.of(coverage));

        //ASSERT + ACT
        InvalidOperationException exception = Assertions.assertThrows(InvalidOperationException.class,
                () -> service.decide(10L, decisionDTO, analyst));
        then(solicitationRepository).should(Mockito.never()).save(any());
        Assertions.assertEquals("You can only decide if the status is `IN_REVIEW`", exception.getMessage());
    }

    @Test
    @DisplayName("It should not decide when the state is outside the coverage of the analyst")
    void verificationErrorAnalystDecideOutsideCoverage() {

        //ARRANGE
        User analyst = User.builder().id(5L).role(RoleUser.ANALYST).build();
        Solicitation solicitation = Solicitation.builder().id(10L).status(StatusSolicitation.IN_REVIEW).currentStep(3)
                .stepTwoData(StepTwoData.builder().state("MG").build()).build();
        AnalystCoverage coverage = AnalystCoverage.builder().user(analyst).states(Set.of("SP")).build();
        DecisionRequestDTO decisionDTO = new DecisionRequestDTO(Decision.APPROVE, "Trying to approve outside my states");

        when(solicitationRepository.findById(10L)).thenReturn(Optional.of(solicitation));
        when(coverageRepository.findByUser(analyst)).thenReturn(Optional.of(coverage));

        //ASSERT + ACT
        Assertions.assertThrows(ForbiddenException.class, () -> service.decide(10L, decisionDTO, analyst));
        then(solicitationRepository).should(Mockito.never()).save(any());
    }
}
