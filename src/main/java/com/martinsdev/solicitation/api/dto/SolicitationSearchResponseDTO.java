package com.martinsdev.solicitation.api.dto;

import java.util.List;

public record SolicitationSearchResponseDTO(List<SolicitationDocumentDTO> items,
                                            int page,
                                            int size,
                                            Long total) {
}
