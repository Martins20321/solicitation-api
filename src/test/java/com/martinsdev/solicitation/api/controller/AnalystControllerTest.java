package com.martinsdev.solicitation.api.controller;

import com.martinsdev.solicitation.api.config.IntegrationTestBase;
import com.martinsdev.solicitation.api.dto.DecisionRequestDTO;
import com.martinsdev.solicitation.api.model.AnalystCoverage;
import com.martinsdev.solicitation.api.model.Solicitation;
import com.martinsdev.solicitation.api.model.User;
import com.martinsdev.solicitation.api.model.document.SolicitationDocument;
import com.martinsdev.solicitation.api.model.embedded.StepOneData;
import com.martinsdev.solicitation.api.model.embedded.StepTwoData;
import com.martinsdev.solicitation.api.model.enums.Decision;
import com.martinsdev.solicitation.api.model.enums.RoleUser;
import com.martinsdev.solicitation.api.model.enums.ServiceType;
import com.martinsdev.solicitation.api.model.enums.StatusSolicitation;
import com.martinsdev.solicitation.api.repository.AnalystCoverageRepository;
import com.martinsdev.solicitation.api.repository.SolicitationRepository;
import com.martinsdev.solicitation.api.repository.SolicitationSearchRepository;
import com.martinsdev.solicitation.api.repository.UserRepository;
import com.martinsdev.solicitation.api.service.SolicitationIndexService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
class AnalystControllerTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SolicitationRepository repository;

    @Autowired
    private AnalystCoverageRepository coverageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SolicitationSearchRepository searchRepository;

    @Autowired
    private SolicitationIndexService indexService;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private JacksonTester<DecisionRequestDTO> decisionJacksonTester;

    private User client;
    private User analystSp;
    private User analystRj;
    private Long existingId;

    @BeforeEach
    void initialization() {
        repository.deleteAll();
        coverageRepository.deleteAll();
        userRepository.deleteAll();
        searchRepository.deleteAll();

        this.client = userRepository.save(User.builder().name("Client").email("client@test.com")
                .passwordHash("hash").role(RoleUser.CLIENT).enabled(true).build());

        this.analystSp = userRepository.save(User.builder().name("Analyst SP").email("analyst.sp@test.com")
                .passwordHash("hash").role(RoleUser.ANALYST).enabled(true).build());
        coverageRepository.save(AnalystCoverage.builder().user(analystSp).states(Set.of("SP")).build());

        this.analystRj = userRepository.save(User.builder().name("Analyst RJ").email("analyst.rj@test.com")
                .passwordHash("hash").role(RoleUser.ANALYST).enabled(true).build());
        coverageRepository.save(AnalystCoverage.builder().user(analystRj).states(Set.of("RJ")).build());

        // a solicitation already in review, with the address in SP
        Solicitation inReview = Solicitation.builder().client(client).status(StatusSolicitation.IN_REVIEW).currentStep(3)
                .stepOneData(StepOneData.builder().serviceType(ServiceType.INSTALLATION).title("Boiler installation")
                        .description("Installation of a boiler on the second floor").build())
                .stepTwoData(StepTwoData.builder().cep("01001-000").number("12A").state("SP").build())
                .build();
        this.existingId = repository.save(inReview).getId();
    }

    @Test
    @DisplayName("It should return code 200 (Sucess) and approve the solicitation when the analyst covers the state")
    void verificationOfSucessWhenDecide() throws Exception {

        //ARRANGE
        DecisionRequestDTO decisionDTO = new DecisionRequestDTO(Decision.APPROVE, "Everything is in order, approved");

        //ACT
        var response = mockMvc.perform(
                post("/analyst/solicitations/{id}/decide", existingId)
                        .with(user(analystSp))
                        .content(decisionJacksonTester.write(decisionDTO).getJson())
                        .contentType(MediaType.APPLICATION_JSON)
        ).andReturn().getResponse();

        //ASSERT
        Assertions.assertEquals(200, response.getStatus());
        Solicitation saved = repository.findById(existingId).orElseThrow();
        Assertions.assertEquals(StatusSolicitation.APPROVED, saved.getStatus());
        Assertions.assertEquals(analystSp.getId(), saved.getAnalyzedBy());
    }

    @Test
    @DisplayName("It should return code 403 (Forbidden) when the state is outside the coverage of the analyst")
    void verificationOfCode403WhenOutsideCoverage() throws Exception {

        //ARRANGE

        //ACT
        var response = mockMvc.perform(
                get("/analyst/solicitations/{id}", existingId)
                        .with(user(analystRj))
        ).andReturn().getResponse();

        //ASSERT
        Assertions.assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("It should return code 403 (Forbidden) when a CLIENT accesses an analyst route")
    void verificationOfCode403WhenClientAccessesAnalystRoute() throws Exception {

        //ARRANGE

        //ACT
        var response = mockMvc.perform(
                get("/analyst/solicitations/{id}", existingId)
                        .with(user(client))
        ).andReturn().getResponse();

        //ASSERT
        Assertions.assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("It should return only the solicitations of the states covered by the analyst when searching")
    void verificationOfSearchRestrictedToTheCoverage() throws Exception {

        //ARRANGE
        indexService.index(repository.findById(existingId).orElseThrow());
        elasticsearchOperations.indexOps(SolicitationDocument.class).refresh();

        //ACT
        var responseSp = mockMvc.perform(
                get("/analyst/solicitations/search")
                        .with(user(analystSp))
        ).andReturn().getResponse();

        var responseRj = mockMvc.perform(
                get("/analyst/solicitations/search")
                        .with(user(analystRj))
        ).andReturn().getResponse();

        //ASSERT
        Assertions.assertEquals(200, responseSp.getStatus());
        Assertions.assertTrue(responseSp.getContentAsString().contains("\"total\":1"));
        Assertions.assertEquals(200, responseRj.getStatus());
        Assertions.assertTrue(responseRj.getContentAsString().contains("\"total\":0"));
    }
}
