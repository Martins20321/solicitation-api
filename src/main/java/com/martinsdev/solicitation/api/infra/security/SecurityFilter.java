package com.martinsdev.solicitation.api.infra.security;

import com.martinsdev.solicitation.api.infra.exception.ErrorResponse;
import com.martinsdev.solicitation.api.infra.exception.ForbiddenException;
import com.martinsdev.solicitation.api.infra.exception.InvalidTokenException;
import com.martinsdev.solicitation.api.service.AuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final AuthenticationService authService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = recoverToken(request);

            if (token != null) {
                var subject = tokenService.getSubject(token);

                var user = authService.loadUserByUsername(subject);

                if (!user.isEnabled()) {
                    throw new ForbiddenException("Your account has been disabled. Contact an administrator");
                }

                var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } catch (InvalidTokenException | UsernameNotFoundException exception) {
            HttpStatus status = HttpStatus.UNAUTHORIZED;
            ErrorResponse errorResponse = new ErrorResponse(Instant.now(), status.value(), "Invalid Token", exception.getMessage(), request.getRequestURI());
            String errorJson = objectMapper.writeValueAsString(errorResponse);
            response.setStatus(status.value());
            response.setContentType("application/json");
            response.getWriter().write(errorJson);
        } catch (ForbiddenException exception) {
            HttpStatus status = HttpStatus.FORBIDDEN;
            ErrorResponse errorResponse = new ErrorResponse(Instant.now(), status.value(), "Access denied", exception.getMessage(), request.getRequestURI());
            String errorJson = objectMapper.writeValueAsString(errorResponse);
            response.setStatus(status.value());
            response.setContentType("application/json");
            response.getWriter().write(errorJson);
        }
    }

    private String recoverToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null) {
            return authorizationHeader.replace("Bearer ", "").trim();
        }

        return null;
    }
}
