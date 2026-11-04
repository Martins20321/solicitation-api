package com.martinsdev.solicitation.api.infra.exception;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        HttpStatus status = HttpStatus.FORBIDDEN;
        String error = "Access denied";
        ErrorResponse errorResponse = new ErrorResponse(Instant.now(), status.value(), error, accessDeniedException.getMessage(), request.getRequestURI());

        String errorJson = objectMapper.writeValueAsString(errorResponse);
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write(errorJson);
    }
}
