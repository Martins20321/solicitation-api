package com.martinsdev.solicitation.api.service;

import com.martinsdev.solicitation.api.dto.SolicitationDocumentDTO;
import com.martinsdev.solicitation.api.dto.SolicitationSearchRequestDTO;
import com.martinsdev.solicitation.api.dto.SolicitationSearchResponseDTO;
import com.martinsdev.solicitation.api.infra.exception.ForbiddenException;
import com.martinsdev.solicitation.api.infra.exception.InvalidOperationException;
import com.martinsdev.solicitation.api.infra.exception.ResourceNotFoundException;
import com.martinsdev.solicitation.api.model.AnalystCoverage;
import com.martinsdev.solicitation.api.model.User;
import com.martinsdev.solicitation.api.model.document.SolicitationDocument;
import com.martinsdev.solicitation.api.model.enums.RoleUser;
import com.martinsdev.solicitation.api.repository.AnalystCoverageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SolicitationSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final AnalystCoverageRepository coverageRepository;

    public SolicitationSearchResponseDTO search(SolicitationSearchRequestDTO searchRequestDTO, User user) {
        Criteria criteria = new Criteria();

        if (searchRequestDTO.q() != null && !searchRequestDTO.q().isBlank()) {
            Criteria qCriteria = new Criteria("title").matches(searchRequestDTO.q()).or("description").matches(searchRequestDTO.q());
            criteria = criteria.and(qCriteria);
        }

        if (searchRequestDTO.status() != null && !searchRequestDTO.status().isBlank()) {
            criteria = criteria.and("status").is(searchRequestDTO.status().strip().toUpperCase());
        }

        if (searchRequestDTO.serviceType() != null && !searchRequestDTO.serviceType().isBlank()) {
            criteria = criteria.and("serviceType").is(searchRequestDTO.serviceType().strip().toUpperCase());
        }

        if (searchRequestDTO.priority() != null && !searchRequestDTO.priority().isBlank()) {
            criteria = criteria.and("priority").is(searchRequestDTO.priority().strip().toUpperCase());
        }

        // Role = ANALYST - força o filtro de UF ao coverage dele
        if (searchRequestDTO.state() != null && !searchRequestDTO.state().isBlank()) { // quando manda o state
            if (user.getRole() == RoleUser.ANALYST) {
                AnalystCoverage analystFound = coverageRepository.findByUser(user)
                        .orElseThrow(() -> new ResourceNotFoundException("AnalystCoverage not found"));
                if (!analystFound.getStates().contains(searchRequestDTO.state().strip().toUpperCase())) {
                    throw new ForbiddenException();
                }
            }
            criteria = criteria.and("state").is(searchRequestDTO.state().strip().toUpperCase());
        } else { // quando nao manda o state
            if (user.getRole() == RoleUser.ANALYST) {
                AnalystCoverage analystFound = coverageRepository.findByUser(user)
                        .orElseThrow(() -> new ResourceNotFoundException("AnalystCoverage not found"));
                criteria = criteria.and("state").in(analystFound.getStates());
            }
        }

        if (searchRequestDTO.dateFrom() != null) {
            criteria = criteria.and("createdAt").greaterThanEqual(searchRequestDTO.dateFrom());
        }

        if (searchRequestDTO.dateTo() != null) {
            criteria = criteria.and("createdAt").lessThanEqual(searchRequestDTO.dateTo());
        }

        Sort sort;
        if (searchRequestDTO.sort() != null) {
            sort = buildSort(searchRequestDTO.sort());
        } else {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        PageRequest pageRequest = PageRequest.of(searchRequestDTO.page(), searchRequestDTO.size(), sort);
        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria).setPageable(pageRequest);
        SearchHits<SolicitationDocument> results = elasticsearchOperations.search(criteriaQuery, SolicitationDocument.class);

        List<SolicitationDocumentDTO> items = results.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(SolicitationDocumentDTO::new)
                .toList();

        return new SolicitationSearchResponseDTO(items,
                pageRequest.getPageNumber(), pageRequest.getPageSize(), results.getTotalHits());
    }

    private Sort buildSort(String sort) {
        String sortClean = sort.strip();
        String[] split = sortClean.split(",");
        if (split.length != 2) {
            throw new InvalidOperationException("Invalid sort format. Expected 'field,direction'. Ex: ('createdAt', ASC)");
        }
        String field = split[0];
        String direction = split[1];
        try {
            return Sort.by(Sort.Direction.valueOf(direction.toUpperCase().strip()), field.strip());
        } catch (IllegalArgumentException exception) {
            throw new InvalidOperationException("Invalid sort direction '" + direction + "'. Use 'ASC' or 'DESC', please!");
        }
    }
}
