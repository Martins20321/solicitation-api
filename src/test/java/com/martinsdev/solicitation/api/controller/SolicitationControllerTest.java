package com.martinsdev.solicitation.api.controller;

import com.martinsdev.solicitation.api.config.IntegrationTestBase;
import com.martinsdev.solicitation.api.dto.StepTwoRequestDTO;
import com.martinsdev.solicitation.api.dto.ViaCepResponseDTO;
import com.martinsdev.solicitation.api.infra.client.ViaCepClient;
import com.martinsdev.solicitation.api.model.Solicitation;
import com.martinsdev.solicitation.api.model.User;
import com.martinsdev.solicitation.api.model.embedded.StepOneData;
import com.martinsdev.solicitation.api.model.embedded.StepThreeData;
import com.martinsdev.solicitation.api.model.embedded.StepTwoData;
import com.martinsdev.solicitation.api.model.enums.Priority;
import com.martinsdev.solicitation.api.model.enums.RoleUser;
import com.martinsdev.solicitation.api.model.enums.ServiceType;
import com.martinsdev.solicitation.api.model.enums.StatusSolicitation;
import com.martinsdev.solicitation.api.repository.AnalystCoverageRepository;
import com.martinsdev.solicitation.api.repository.SolicitationRepository;
import com.martinsdev.solicitation.api.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
class SolicitationControllerTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SolicitationRepository repository;

    @Autowired
    private AnalystCoverageRepository coverageRepository;

    @Autowired
    private UserRepository userRepository;

    // ViaCEP is an external service, so it is the only thing mocked here
    @MockitoBean
    private ViaCepClient cepClient;

    @Autowired
    private JacksonTester<StepTwoRequestDTO> stepTwoJacksonTester;

    private User client;
    private User otherClient;
    private Long existingId;

    @BeforeEach
    void initialization() {
        repository.deleteAll();
        coverageRepository.deleteAll();
        userRepository.deleteAll();

        this.client = userRepository.save(User.builder().name("Client").email("client@test.com")
                .passwordHash("hash").role(RoleUser.CLIENT).enabled(true).build());
        this.otherClient = userRepository.save(User.builder().name("Other Client").email("other@test.com")
                .passwordHash("hash").role(RoleUser.CLIENT).enabled(true).build());

        Solicitation draft = Solicitation.builder().client(client).status(StatusSolicitation.DRAFT).currentStep(1).build();
        this.existingId = repository.save(draft).getId();
    }

    @Test
    @DisplayName("It should return code 200 (Sucess) and fill the address with the ViaCEP data when saving the step 2")
    void verificationOfSucessWhenSavingStep2() throws Exception {

        //ARRANGE
        StepTwoRequestDTO stepTwoDTO = new StepTwoRequestDTO("01001000", "12A");
        when(cepClient.findAddressByCep("01001000")).thenReturn(
                new ViaCepResponseDTO("01001-000", "Praça da Sé", "lado ímpar", "Sé", "São Paulo", "SP", null));

        //ACT
        var response = mockMvc.perform(
                put("/solicitations/{id}/step2", existingId)
                        .with(user(client))
                        .content(stepTwoJacksonTester.write(stepTwoDTO).getJson())
                        .contentType(MediaType.APPLICATION_JSON)
        ).andReturn().getResponse();

        //ASSERT
        Assertions.assertEquals(200, response.getStatus());
        Solicitation saved = repository.findById(existingId).orElseThrow();
        Assertions.assertEquals("SP", saved.getStepTwoData().getState());
        Assertions.assertEquals(2, saved.getCurrentStep());
    }

    @Test
    @DisplayName("It should return code 200 (Sucess) and change the status to SUBMITTED when all the steps are complete")
    void verificationOfSucessWhenSubmit() throws Exception {

        //ARRANGE
        Solicitation complete = repository.findById(existingId).orElseThrow();
        complete.setStepOneData(StepOneData.builder().serviceType(ServiceType.INSTALLATION).title("Boiler installation")
                .description("Installation of a boiler on the second floor").build());
        complete.setStepTwoData(StepTwoData.builder().cep("01001-000").number("12A").state("SP").build());
        complete.setStepThreeData(StepThreeData.builder().priority(Priority.LOW).estimatedValue(BigDecimal.TEN)
                .preferredDate(LocalDate.now().plusDays(5)).termsAccepted(true).build());
        complete.setCurrentStep(3);
        repository.save(complete);

        //ACT
        var response = mockMvc.perform(
                post("/solicitations/{id}/submit", existingId)
                        .with(user(client))
        ).andReturn().getResponse();

        //ASSERT
        Assertions.assertEquals(200, response.getStatus());
        Solicitation saved = repository.findById(existingId).orElseThrow();
        Assertions.assertEquals(StatusSolicitation.SUBMITTED, saved.getStatus());
        Assertions.assertNotNull(saved.getSubmittedAt());
    }

    @Test
    @DisplayName("It should return code 400 (Bad Request) when submitting an incomplete solicitation")
    void verificationOfErrorWhenSubmitIncomplete() throws Exception {

        //ARRANGE

        //ACT
        var response = mockMvc.perform(
                post("/solicitations/{id}/submit", existingId)
                        .with(user(client))
        ).andReturn().getResponse();

        //ASSERT
        Assertions.assertEquals(400, response.getStatus());
        Assertions.assertTrue(response.getContentAsString().contains("Step 1 is not complete"));
        Assertions.assertEquals(StatusSolicitation.DRAFT, repository.findById(existingId).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("It should return code 403 (Forbidden) when the solicitation belongs to another client")
    void verificationOfCode403WhenFindByIdOfAnotherClient() throws Exception {

        //ARRANGE

        //ACT
        var response = mockMvc.perform(
                get("/solicitations/{id}", existingId)
                        .with(user(otherClient))
        ).andReturn().getResponse();

        //ASSERT
        Assertions.assertEquals(403, response.getStatus());
    }
}
