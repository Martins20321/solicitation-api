package com.martinsdev.solicitation.api.service;

import com.martinsdev.solicitation.api.dto.RegisterRequestDTO;
import com.martinsdev.solicitation.api.infra.exception.EmailAlreadyExistsException;
import com.martinsdev.solicitation.api.model.User;
import com.martinsdev.solicitation.api.model.enums.RoleUser;
import com.martinsdev.solicitation.api.repository.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService service;

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    @DisplayName("It should save a CLIENT with the password encoded when registering")
    void verificationSucessUserRegistration() {

        //ARRANGE
        RegisterRequestDTO registerDTO = new RegisterRequestDTO("Carl", "carl@test.com", "123456");

        when(repository.existsByEmail("carl@test.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("HASH");

        //ACT
        User result = service.register(registerDTO);

        //ASSERT
        then(repository).should().save(userCaptor.capture());
        User saved = userCaptor.getValue();

        Assertions.assertEquals("Carl", saved.getName());
        Assertions.assertEquals("carl@test.com", saved.getEmail());
        Assertions.assertEquals("HASH", saved.getPasswordHash());
        Assertions.assertEquals(RoleUser.CLIENT, saved.getRole());
        Assertions.assertTrue(saved.isEnabled());
        Assertions.assertEquals(saved, result);
    }

    @Test
    @DisplayName("It should not save the user when registering. Ex: email already exists")
    void verificationErrorUserRegistration() {

        //ARRANGE
        RegisterRequestDTO registerDTO = new RegisterRequestDTO("Carl", "carl@test.com", "123456");

        when(repository.existsByEmail("carl@test.com")).thenReturn(true);

        //ASSERT + ACT
        EmailAlreadyExistsException exception = Assertions.assertThrows(EmailAlreadyExistsException.class,
                () -> service.register(registerDTO));
        then(repository).should(Mockito.never()).save(any());
        then(passwordEncoder).shouldHaveNoInteractions();
        Assertions.assertEquals("This email already exists: carl@test.com", exception.getMessage());
    }
}
