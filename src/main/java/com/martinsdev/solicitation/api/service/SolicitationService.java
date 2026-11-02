package com.martinsdev.solicitation.api.service;

import com.martinsdev.solicitation.api.dto.*;
import com.martinsdev.solicitation.api.infra.client.ViaCepClient;
import com.martinsdev.solicitation.api.infra.exception.ForbiddenException;
import com.martinsdev.solicitation.api.infra.exception.IncompleteSubmissionException;
import com.martinsdev.solicitation.api.infra.exception.InvalidOperationException;
import com.martinsdev.solicitation.api.infra.exception.ResourceNotFoundException;
import com.martinsdev.solicitation.api.model.Solicitation;
import com.martinsdev.solicitation.api.model.User;
import com.martinsdev.solicitation.api.model.embedded.StepOneData;
import com.martinsdev.solicitation.api.model.embedded.StepThreeData;
import com.martinsdev.solicitation.api.model.embedded.StepTwoData;
import com.martinsdev.solicitation.api.model.enums.Priority;
import com.martinsdev.solicitation.api.model.enums.StatusSolicitation;
import com.martinsdev.solicitation.api.repository.SolicitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SolicitationService {

    private final SolicitationRepository repository;
    private final ViaCepClient cepClient;

    // criacao de rascunho/DRAFT
    public SolicitationResponseDTO create(User client) {
        Solicitation solicitation = Solicitation.builder()
                .status(StatusSolicitation.DRAFT)
                .currentStep(1)
                .client(client)
                .build();
        repository.save(solicitation);

        return new SolicitationResponseDTO(solicitation);
    }

    // cliente buscando todas as suas solicitacoes
    public List<SolicitationDetailResponseDTO> findAllByClient(User client) {
        return repository.findByClient(client)
                .stream().map(SolicitationDetailResponseDTO::new).toList();
    }

    // cliente buscando uma solicitacao somente
    public SolicitationDetailResponseDTO getSolicitationByIdClient(Long id, User client) {
        Solicitation solicitation = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitation not found by id: " + id));

        if (!solicitation.getClient().getId().equals(client.getId())) {
            throw new ForbiddenException();
        }

        return new SolicitationDetailResponseDTO(solicitation);
    }

    // atualizacao da solicitação no step1
    public SolicitationResponseDTO saveStep1(Long id, StepOneRequestDTO oneRequestDTO, User client) {

        Solicitation solicitationStp1 = findAndValidateDraftOwnership(id, client);

        StepOneData stepOne = StepOneData.builder()
                .serviceType(oneRequestDTO.serviceType())
                .title(oneRequestDTO.title())
                .description(oneRequestDTO.description())
                .build();

        solicitationStp1.setStepOneData(stepOne);
        solicitationStp1.setCurrentStep(1);

        repository.save(solicitationStp1);

        return new SolicitationResponseDTO(solicitationStp1);
    }

    // atualização da solicitação com step2
    public SolicitationResponseDTO saveStep2(Long id, StepTwoRequestDTO twoRequestDTO, User client) {

        Solicitation solicitationStp2 = findAndValidateDraftOwnership(id, client);

        ViaCepResponseDTO addressByCep = cepClient.findAddressByCep(twoRequestDTO.cep());

        StepTwoData stepTwo = StepTwoData.builder()
                .cep(addressByCep.cep())
                .number(twoRequestDTO.number().strip())
                .complement(addressByCep.complement())
                .street(addressByCep.street())
                .neighborhood(addressByCep.neighborhood())
                .city(addressByCep.city())
                .state(addressByCep.state())
                .build();

        solicitationStp2.setStepTwoData(stepTwo);
        solicitationStp2.setCurrentStep(2);

        repository.save(solicitationStp2);

        return new SolicitationResponseDTO(solicitationStp2);
    }

    // atualização da solicitação com step3
    public SolicitationResponseDTO saveStep3(Long id, StepThreeRequestDTO threeRequestDTO, User client) {

        // metodo contendo todas as verificações
        Solicitation solicitationStp3 = findAndValidateDraftOwnership(id, client);

        StepThreeData stepThree = StepThreeData.builder()
                .priority(threeRequestDTO.priority())
                .preferredDate(threeRequestDTO.preferredDate())
                .estimatedValue(threeRequestDTO.estimatedValue())
                .termsAccepted(threeRequestDTO.termsAccepted())
                .build();

        if (stepThree.getPriority() == Priority.HIGH && stepThree.getEstimatedValue().compareTo(BigDecimal.valueOf(100)) < 0) {
            throw new InvalidOperationException("If the priority is High, the estimated minimum value is 100.0!");
        }

        solicitationStp3.setStepThreeData(stepThree);
        solicitationStp3.setCurrentStep(3);

        repository.save(solicitationStp3);

        return new SolicitationResponseDTO(solicitationStp3);
    }

    // enviando para análise(Submitted)
    public SolicitationResponseDTO submit(Long id, User client) {

        Solicitation solicitationSubmitted = findAndValidateDraftOwnership(id, client);

        // verificação dos campos necessários, com validação para os atributos essenciais, evitando o hibernate entregar um objeto com campos nulos
        validateStepsComplete(solicitationSubmitted);

        solicitationSubmitted.setStatus(StatusSolicitation.SUBMITTED); // nao pode mais alterar
        solicitationSubmitted.setSubmittedAt(LocalDateTime.now());

        repository.save(solicitationSubmitted);

        return new SolicitationResponseDTO(solicitationSubmitted);
    }

    private Solicitation findAndValidateDraftOwnership(Long id, User client) {
        // buscando a solicitação no banco
        Solicitation solicitation = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitation not found by id: " + id));

        // verficando se e o mesmo cliente
        if (!solicitation.getClient().getId().equals(client.getId())) {
            throw new ForbiddenException();
        }

        // verificando se está com status DRAFT/Step anterior
        if (solicitation.getStatus() != StatusSolicitation.DRAFT) {
            throw new InvalidOperationException("Solicitation can only be edited when status is DRAFT");
        }

        return solicitation;
    }

    private void validateStepsComplete(Solicitation solicitation) {
        List<String> erros = new ArrayList<>();

        if (solicitation.getStepOneData() == null || solicitation.getStepOneData().getServiceType() == null) {
            erros.add("Step 1 is not complete");
        }

        if (solicitation.getStepTwoData() == null || solicitation.getStepTwoData().getCep() == null) {
            erros.add("Step 2 is not complete");
        }

        if (solicitation.getStepThreeData() == null || solicitation.getStepThreeData().getPriority() == null) {
            erros.add("Step 3 is not complete");
        } else if (solicitation.getStepThreeData().getTermsAccepted() == null || !solicitation.getStepThreeData().getTermsAccepted()) {
            erros.add("Terms must be accepted to submit the solicitation");
        }

        if (!erros.isEmpty()) {
            throw new IncompleteSubmissionException(erros);
        }
    }
}
