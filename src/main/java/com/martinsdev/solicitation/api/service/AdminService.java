package com.martinsdev.solicitation.api.service;

import com.martinsdev.solicitation.api.dto.AdminCreateUserRequestDTO;
import com.martinsdev.solicitation.api.dto.UserResponseDTO;
import com.martinsdev.solicitation.api.infra.aop.Audit;
import com.martinsdev.solicitation.api.infra.exception.EmailAlreadyExistsException;
import com.martinsdev.solicitation.api.infra.exception.InvalidOperationException;
import com.martinsdev.solicitation.api.infra.exception.InvalidRoleException;
import com.martinsdev.solicitation.api.infra.exception.ResourceNotFoundException;
import com.martinsdev.solicitation.api.model.AnalystCoverage;
import com.martinsdev.solicitation.api.model.User;
import com.martinsdev.solicitation.api.model.enums.RoleUser;
import com.martinsdev.solicitation.api.repository.AnalystCoverageRepository;
import com.martinsdev.solicitation.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AnalystCoverageRepository coverageRepository;

    @Audit(action = "CREATE_USER")
    @Transactional
    public UserResponseDTO createUser(AdminCreateUserRequestDTO userRequestDTO) {
        if (userRepository.existsByEmail(userRequestDTO.email())) {
            throw new EmailAlreadyExistsException(userRequestDTO.email());
        }

        if (userRequestDTO.role() == RoleUser.CLIENT) {
            throw new InvalidRoleException();
        }

        User user = User.builder()
                .name(userRequestDTO.name().strip())
                .email(userRequestDTO.email().strip())
                .passwordHash(passwordEncoder.encode(userRequestDTO.password()))
                .role(userRequestDTO.role())
                .enabled(true)
                .build();
        userRepository.save(user);

        if (userRequestDTO.role() == RoleUser.ANALYST && (userRequestDTO.states() == null || userRequestDTO.states().isEmpty())) {
            throw new InvalidOperationException("An ANALYST must be assigned at least one state of coverage");
        } else if (userRequestDTO.role() == RoleUser.ANALYST) {
            AnalystCoverage coverage = AnalystCoverage.builder()
                    .user(user)
                    .states(userRequestDTO.states())
                    .build();
            coverageRepository.save(coverage);
        }

        return new UserResponseDTO(user);
    }

    public UserResponseDTO getUserById(Long id) {
        return new UserResponseDTO(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found by id '" + id + "'")));
    }
}
