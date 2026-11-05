package com.martinsdev.solicitation.api.service;

import com.martinsdev.solicitation.api.dto.*;
import com.martinsdev.solicitation.api.infra.client.ViaCepClient;
import com.martinsdev.solicitation.api.infra.exception.ForbiddenException;
import com.martinsdev.solicitation.api.infra.exception.IncompleteSubmissionException;
import com.martinsdev.solicitation.api.infra.exception.InvalidCepException;
import com.martinsdev.solicitation.api.infra.exception.InvalidOperationException;
import com.martinsdev.solicitation.api.infra.exception.ResourceNotFoundException;
import com.martinsdev.solicitation.api.model.Solicitation;
import com.martinsdev.solicitation.api.model.User;
import com.martinsdev.solicitation.api.model.embedded.StepOneData;
import com.martinsdev.solicitation.api.model.embedded.StepThreeData;
import com.martinsdev.solicitation.api.model.embedded.StepTwoData;
import com.martinsdev.solicitation.api.model.enums.Priority;
import com.martinsdev.solicitation.api.model.enums.ServiceType;
import com.martinsdev.solicitation.api.model.enums.StatusSolicitation;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitationServiceTest {

    @InjectMocks
    private SolicitationService service;

    @Mock
    private SolicitationRepository repository;

    @Mock
    private ViaCepClient cepClient;

    @Mock
    private SolicitationIndexService indexService;

    @Captor
    private ArgumentCaptor<Solicitation> solicitationCaptor;

    // ------------------------------------------------ CREATE ------------------------------------------------

    @Test
    @DisplayName("It should save a DRAFT at step 1 and index it when creating a solicitation")
    void verificationSucessSolicitationCreate() {

        //ARRANGE
        User client = User.builder().id(1L).build();

        //ACT
        SolicitationResponseDTO result = service.create(client);

        //ASSERT
        then(repository).should().save(solicitationCaptor.capture());
        Solicitation saved = solicitationCaptor.getValue();

        Assertions.assertEquals(StatusSolicitation.DRAFT, saved.getStatus());
        Assertions.assertEquals(1, saved.getCurrentStep());
        Assertions.assertEquals(client, saved.getClient());
        Assertions.assertEquals(StatusSolicitation.DRAFT, result.status());
        then(indexService).should().index(any(Solicitation.class));
    }

    // ------------------------------------------------ READ ------------------------------------------------

    @Test
    @DisplayName("It should return the page of solicitations of the client")
    void verificationSucessSolicitationFindAllByClient() {

        //ARRANGE
        User client = User.builder().id(1L).build();
        Solicitation first = Solicitation.builder().id(10L).client(client).status(StatusSolicitation.DRAFT).currentStep(1).build();
        Solicitation second = Solicitation.builder().id(11L).client(client).status(StatusSolicitation.DRAFT).currentStep(1).build();
        Pageable pageable = PageRequest.of(0, 20);

        when(repository.findByClient(client, pageable)).thenReturn(new PageImpl<>(List.of(first, second), pageable, 2));

        //ACT
        Page<SolicitationDetailResponseDTO> result = service.findAllByClient(client, pageable);

        //ASSERT
        Assertions.assertEquals(2, result.getTotalElements());
        Assertions.assertEquals(10L, result.getContent().get(0).id());
        Assertions.assertEquals(11L, result.getContent().get(1).id());
    }

    @Test
    @DisplayName("It should return the solicitation when the client is the owner")
    void verificationSucessSolicitationGetByIdClient() {

        //ARRANGE
        User client = User.builder().id(1L).build();
        Solicitation solicitation = Solicitation.builder().id(10L).client(client).status(StatusSolicitation.DRAFT).currentStep(1).build();

        when(repository.findById(10L)).thenReturn(Optional.of(solicitation));

        //ACT
        SolicitationDetailResponseDTO result = service.getSolicitationByIdClient(10L, client);

        //ASSERT
        Assertions.assertEquals(10L, result.id());
        Assertions.assertEquals(StatusSolicitation.DRAFT, result.status());
    }

    @Test
    @DisplayName("It should throw an exception when the solicitation does not exist")
    void verificationErrorSolicitationGetByIdClientNotFound() {

        //ARRANGE
        User client = User.builder().id(1L).build();

        when(repository.findById(10L)).thenReturn(Optional.empty());

        //ASSERT + ACT
        ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
                () -> service.getSolicitationByIdClient(10L, client));
        Assertions.assertEquals("Solicitation not found by id: 10", exception.getMessage());
    }

    @Test
    @DisplayName("It should throw an exception when the client is not the owner of the solicitation")
    void verificationErrorSolicitationGetByIdClientForbidden() {

        //ARRANGE
        User owner = User.builder().id(1L).build();
        User otherClient = User.builder().id(2L).build();
        Solicitation solicitation = Solicitation.builder().id(10L).client(owner).status(StatusSolicitation.DRAFT).currentStep(1).build();

        when(repository.findById(10L)).thenReturn(Optional.of(solicitation));

        //ASSERT + ACT
        Assertions.assertThrows(ForbiddenException.class, () -> service.getSolicitationByIdClient(10L, otherClient));
    }

    // ------------------------------------------------ STEP 1 ------------------------------------------------

    @Test
    @DisplayName("It should save the data of step 1 and keep the current step as 1")
    void verificationSucessSolicitationSaveStep1() {

        //ARRANGE
        User client = User.builder().id(1L).build();
        Solicitation solicitation = Solicitation.builder().id(10L).client(client).status(StatusSolicitation.DRAFT).currentStep(1).build();
        StepOneRequestDTO stepOneDTO = new StepOneRequestDTO(ServiceType.MAINTENANCE, "Boiler maintenance", "Maintenance of the boiler on the second floor");

        when(repository.findById(10L)).thenReturn(Optional.of(solicitation));

        //ACT
        service.saveStep1(10L, stepOneDTO, client);

        //ASSERT
        then(repository).should().save(solicitationCaptor.capture());
        Solicitation saved = solicitationCaptor.getValue();

        Assertions.assertEquals(ServiceType.MAINTENANCE, saved.getStepOneData().getServiceType());
        Assertions.assertEquals("Boiler maintenance", saved.getStepOneData().getTitle());
        Assertions.assertEquals(1, saved.getCurrentStep());
        then(indexService).should().index(saved);
    }

    @Test
    @DisplayName("It should not save step 1 when the solicitation is not a DRAFT anymore")
    void verificationErrorSolicitationSaveStep1NotDraft() {

        //ARRANGE
        User client = User.builder().id(1L).build();
        Solicitation solicitation = Solicitation.builder().id(10L).client(client).status(StatusSolicitation.SUBMITTED).currentStep(3).build();
        StepOneRequestDTO stepOneDTO = new StepOneRequestDTO(ServiceType.MAINTENANCE, "Boiler maintenance", "Maintenance of the boiler on the second floor");

        when(repository.findById(10L)).thenReturn(Optional.of(solicitation));

        //ASSERT + ACT
        InvalidOperationException exception = Assertions.assertThrows(InvalidOperationException.class,
                () -> service.saveStep1(10L, stepOneDTO, client));
        then(repository).should(Mockito.never()).save(any());
        Assertions.assertEquals("Solicitation can only be edited when status is DRAFT", exception.getMessage());
    }

    @Test
    @DisplayName("It should not save step 1 when the client is not the owner")
    void verificationErrorSolicitationSaveStep1Forbidden() {

        //ARRANGE
        User owner = User.builder().id(1L).build();
        User otherClient = User.builder().id(2L).build();
        Solicitation solicitation = Solicitation.builder().id(10L).client(owner).status(StatusSolicitation.DRAFT).currentStep(1).build();
        StepOneRequestDTO stepOneDTO = new StepOneRequestDTO(ServiceType.MAINTENANCE, "Boiler maintenance", "Maintenance of the boiler on the second floor");

        when(repository.findById(10L)).thenReturn(Optional.of(solicitation));

        //ASSERT + ACT
        Assertions.assertThrows(ForbiddenException.class, () -> service.saveStep1(10L, stepOneDTO, otherClient));
        then(repository).should(Mockito.never()).save(any());
    }

    // ------------------------------------------------ STEP 2 ------------------------------------------------

    @Test
    @DisplayName("It should fill the address with the ViaCEP data and move to step 2")
    void verificationSucessSolicitationSaveStep2() {

        //ARRANGE
        User client = User.builder().id(1L).build();
        Solicitation solicitation = Solicitation.builder().id(10L).client(client).status(StatusSolicitation.DRAFT).currentStep(1).build();
        StepTwoRequestDTO stepTwoDTO = new StepTwoRequestDTO("01001000", "12A");
        ViaCepResponseDTO viaCep = new ViaCepResponseDTO("01001-000", "Praça da Sé", "lado ímpar", "Sé", "São Paulo", "SP", null);

        when(repository.findById(10L)).thenReturn(Optional.of(solicitation));
        when(cepClient.findAddressByCep("01001000")).thenReturn(viaCep);

        //ACT
        service.saveStep2(10L, stepTwoDTO, client);

        //ASSERT
        then(repository).should().save(solicitationCaptor.capture());
        StepTwoData saved = solicitationCaptor.getValue().getStepTwoData();

        Assertions.assertEquals("01001-000", saved.getCep());
        Assertions.assertEquals("12A", saved.getNumber());
        Assertions.assertEquals("Praça da Sé", saved.getStreet());
        Assertions.assertEquals("São Paulo", saved.getCity());
        Assertions.assertEquals("SP", saved.getState());
        Assertions.assertEquals(2, solicitationCaptor.getValue().getCurrentStep());
        then(indexService).should().index(solicitationCaptor.getValue());
    }

    @Test
    @DisplayName("It should not save step 2 when the CEP is invalid")
    void verificationErrorSolicitationSaveStep2InvalidCep() {

        //ARRANGE
        User client = User.builder().id(1L).build();
        Solicitation solicitation = Solicitation.builder().id(10L).client(client).status(StatusSolicitation.DRAFT).currentStep(1).build();
        StepTwoRequestDTO stepTwoDTO = new StepTwoRequestDTO("00000000", "12A");

        when(repository.findById(10L)).thenReturn(Optional.of(solicitation));
        when(cepClient.findAddressByCep("00000000")).thenThrow(new InvalidCepException("00000000"));

        //ASSERT + ACT
        Assertions.assertThrows(InvalidCepException.class, () -> service.saveStep2(10L, stepTwoDTO, client));
        then(repository).should(Mockito.never()).save(any());
        then(indexService).should(Mockito.never()).index(any());
    }

    @Test
    @DisplayName("It should not call ViaCEP when the client is not the owner")
    void verificationErrorSolicitationSaveStep2Forbidden() {

        //ARRANGE
        User owner = User.builder().id(1L).build();
        User otherClient = User.builder().id(2L).build();
        Solicitation solicitation = Solicitation.builder().id(10L).client(owner).status(StatusSolicitation.DRAFT).currentStep(1).build();
        StepTwoRequestDTO stepTwoDTO = new StepTwoRequestDTO("01001000", "12A");

        when(repository.findById(10L)).thenReturn(Optional.of(solicitation));

        //ASSERT + ACT
        Assertions.assertThrows(ForbiddenException.class, () -> service.saveStep2(10L, stepTwoDTO, otherClient));
        then(cepClient).shouldHaveNoInteractions();
    }

    // ------------------------------------------------ STEP 3 ------------------------------------------------

    @Test
    @DisplayName("It should save the data of step 3 and move to step 3")
    void verificationSucessSolicitationSaveStep3() {

        //ARRANGE
        User client = User.builder().id(1L).build();
        Solicitation solicitation = Solicitation.builder().id(10L).client(client).status(StatusSolicitation.DRAFT).currentStep(2).build();
        StepThreeRequestDTO stepThreeDTO = new StepThreeRequestDTO(Priority.MEDIUM, LocalDate.now().plusDays(5), new BigDecimal("50"), true);

        when(repository.findById(10L)).thenReturn(Optional.of(solicitation));

        //ACT
        service.saveStep3(10L, stepThreeDTO, client);

        //ASSERT
        then(repository).should().save(solicitationCaptor.capture());
        StepThreeData saved = solicitationCaptor.getValue().getStepThreeData();

        Assertions.assertEquals(Priority.MEDIUM, saved.getPriority());
        Assertions.assertEquals(new BigDecimal("50"), saved.getEstimatedValue());
        Assertions.assertTrue(saved.getTermsAccepted());
        Assertions.assertEquals(3, solicitationCaptor.getValue().getCurrentStep());
    }

    @Test
    @DisplayName("It should accept HIGH priority when the estimated value is exactly 100")
    void verificationSucessSolicitationSaveStep3HighPriorityAtLimit() {

        //ARRANGE
        User client = User.builder().id(1L).build();
        Solicitation solicitation = Solicitation.builder().id(10L).client(client).status(StatusSolicitation.DRAFT).currentStep(2).build();
        StepThreeRequestDTO stepThreeDTO = new StepThreeRequestDTO(Priority.HIGH, LocalDate.now().plusDays(5), new BigDecimal("100"), true);

        when(repository.findById(10L)).thenReturn(Optional.of(solicitation));

        //ACT
        service.saveStep3(10L, stepThreeDTO, client);

        //ASSERT
        then(repository).should().save(solicitationCaptor.capture());
        Assertions.assertEquals(Priority.HIGH, solicitationCaptor.getValue().getStepThreeData().getPriority());
    }

    @Test
    @DisplayName("It should not save step 3 when the priority is HIGH and the estimated value is lower than 100")
    void verificationErrorSolicitationSaveStep3HighPriorityBelowMinimum() {

        //ARRANGE
        User client = User.builder().id(1L).build();
        Solicitation solicitation = Solicitation.builder().id(10L).client(client).status(StatusSolicitation.DRAFT).currentStep(2).build();
        StepThreeRequestDTO stepThreeDTO = new StepThreeRequestDTO(Priority.HIGH, LocalDate.now().plusDays(5), new BigDecimal("99.99"), true);

        when(repository.findById(10L)).thenReturn(Optional.of(solicitation));

        //ASSERT + ACT
        InvalidOperationException exception = Assertions.assertThrows(InvalidOperationException.class,
                () -> service.saveStep3(10L, stepThreeDTO, client));
        then(repository).should(Mockito.never()).save(any());
        Assertions.assertEquals("If the priority is High, the estimated minimum value is 100.0!", exception.getMessage());
    }

    // ------------------------------------------------ SUBMIT ------------------------------------------------

    @Test
    @DisplayName("It should change the status to SUBMITTED when all the steps are complete")
    void verificationSucessSolicitationSubmit() {

        //ARRANGE
        User client = User.builder().id(1L).build();
        Solicitation solicitation = Solicitation.builder().id(10L).client(client).status(StatusSolicitation.DRAFT).currentStep(3)
                .stepOneData(StepOneData.builder().serviceType(ServiceType.INSTALLATION).title("Title").description("Description").build())
                .stepTwoData(StepTwoData.builder().cep("01001-000").state("SP").build())
                .stepThreeData(StepThreeData.builder().priority(Priority.LOW).termsAccepted(true).estimatedValue(BigDecimal.TEN)
                        .preferredDate(LocalDate.now().plusDays(1)).build())
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(solicitation));

        //ACT
        SolicitationResponseDTO result = service.submit(10L, client);

        //ASSERT
        then(repository).should().save(solicitationCaptor.capture());
        Solicitation saved = solicitationCaptor.getValue();

        Assertions.assertEquals(StatusSolicitation.SUBMITTED, saved.getStatus());
        Assertions.assertNotNull(saved.getSubmittedAt());
        Assertions.assertEquals(StatusSolicitation.SUBMITTED, result.status());
        then(indexService).should().index(saved);
    }

    @Test
    @DisplayName("It should list every missing step when submitting an empty DRAFT")
    void verificationErrorSolicitationSubmitIncomplete() {

        //ARRANGE
        User client = User.builder().id(1L).build();
        Solicitation solicitation = Solicitation.builder().id(10L).client(client).status(StatusSolicitation.DRAFT).currentStep(1).build();

        when(repository.findById(10L)).thenReturn(Optional.of(solicitation));

        //ASSERT + ACT
        IncompleteSubmissionException exception = Assertions.assertThrows(IncompleteSubmissionException.class,
                () -> service.submit(10L, client));
        then(repository).should(Mockito.never()).save(any());
        Assertions.assertEquals(List.of("Step 1 is not complete", "Step 2 is not complete", "Step 3 is not complete"), exception.getErrors());
    }

    @Test
    @DisplayName("It should not submit when the terms were not accepted")
    void verificationErrorSolicitationSubmitTermsNotAccepted() {

        //ARRANGE
        User client = User.builder().id(1L).build();
        Solicitation solicitation = Solicitation.builder().id(10L).client(client).status(StatusSolicitation.DRAFT).currentStep(3)
                .stepOneData(StepOneData.builder().serviceType(ServiceType.INSTALLATION).title("Title").description("Description").build())
                .stepTwoData(StepTwoData.builder().cep("01001-000").state("SP").build())
                .stepThreeData(StepThreeData.builder().priority(Priority.LOW).termsAccepted(false).estimatedValue(BigDecimal.TEN)
                        .preferredDate(LocalDate.now().plusDays(1)).build())
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(solicitation));

        //ASSERT + ACT
        IncompleteSubmissionException exception = Assertions.assertThrows(IncompleteSubmissionException.class,
                () -> service.submit(10L, client));
        then(repository).should(Mockito.never()).save(any());
        Assertions.assertEquals(List.of("Terms must be accepted to submit the solicitation"), exception.getErrors());
        Assertions.assertEquals(StatusSolicitation.DRAFT, solicitation.getStatus());
    }

    @Test
    @DisplayName("It should not submit a solicitation that was already submitted")
    void verificationErrorSolicitationSubmitNotDraft() {

        //ARRANGE
        User client = User.builder().id(1L).build();
        Solicitation solicitation = Solicitation.builder().id(10L).client(client).status(StatusSolicitation.SUBMITTED).currentStep(3).build();

        when(repository.findById(10L)).thenReturn(Optional.of(solicitation));

        //ASSERT + ACT
        Assertions.assertThrows(InvalidOperationException.class, () -> service.submit(10L, client));
        then(repository).should(Mockito.never()).save(any());
    }
}
