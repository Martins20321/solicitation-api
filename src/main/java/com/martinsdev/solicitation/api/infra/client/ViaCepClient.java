package com.martinsdev.solicitation.api.infra.client;

import com.martinsdev.solicitation.api.dto.ViaCepResponseDTO;
import com.martinsdev.solicitation.api.infra.exception.ExternalServiceException;
import com.martinsdev.solicitation.api.infra.exception.InvalidCepException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class ViaCepClient {

    private final RestClient restClient;

    public ViaCepClient() {
        this.restClient = RestClient.create("https://viacep.com.br/ws");
    }

    public ViaCepResponseDTO findAddressByCep(String cep) {
        try {
            ViaCepResponseDTO viaCepResponseDTO = restClient.get()
                    .uri("/{cep}/json", cep)
                    .retrieve()
                    .body(ViaCepResponseDTO.class);

            if (viaCepResponseDTO == null || viaCepResponseDTO.cep() == null) {
                throw new InvalidCepException(cep);
            } else if (Boolean.TRUE.equals(viaCepResponseDTO.error())) {
                throw new InvalidCepException(cep);
            }

            return viaCepResponseDTO;
        } catch (HttpClientErrorException e) {
            // ViaCEP mal formatado
            throw new InvalidCepException(cep);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            throw new ExternalServiceException("ViaCEP service unavailable. Please try again later");
        }
    }
}
