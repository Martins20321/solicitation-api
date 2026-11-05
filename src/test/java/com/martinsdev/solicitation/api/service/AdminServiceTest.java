package com.martinsdev.solicitation.api.service;

import com.martinsdev.solicitation.api.dto.AdminCreateUserRequestDTO;
import com.martinsdev.solicitation.api.dto.UserResponseDTO;
import com.martinsdev.solicitation.api.infra.exception.EmailAlreadyExistsException;
import com.martinsdev.solicitation.api.infra.exception.InvalidOperationException;
import com.martinsdev.solicitation.api.infra.exception.InvalidRoleException;
import com.martinsdev.solicitation.api.infra.exception.ResourceNotFoundException;
import com.martinsdev.solicitation.api.model.AnalystCoverage;
import com.martinsdev.solicitation.api.model.User;
import com.martinsdev.solicitation.api.model.enums.RoleUser;
import com.martinsdev.solicitation.api.repository.AnalystCoverageRepository;
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

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks
    private AdminService service;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AnalystCoverageRepository coverageRepository;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<AnalystCoverage> coverageCaptor;

    // ------------------------------------------------ CREATE USER ------------------------------------------------

    @Test
    @DisplayName("It should save an ADMIN with the password encoded and without coverage")
    void verificationSucessAdminCreateAdminUser() {

        //ARRANGE
        AdminCreateUserRequestDTO userDTO = new AdminCreateUserRequestDTO(" Ana Silva ", " Ana@Test.com ", "123456", RoleUser.ADMIN, null);

        when(userRepository.existsByEmail("ana@test.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("HASH");

        //ACT
        UserResponseDTO result = service.createUser(userDTO);

        //ASSERT
        then(userRepository).should().save(userCaptor.capture());
        User saved = userCaptor.getValue();

        Assertions.assertEquals("Ana Silva", saved.getName());
        Assertions.assertEquals("ana@test.com", saved.getEmail());
        Assertions.assertEquals("HASH", saved.getPasswordHash());
        Assertions.assertEquals(RoleUser.ADMIN, saved.getRole());
        Assertions.assertTrue(saved.isEnabled());
        Assertions.assertEquals(RoleUser.ADMIN, result.role());
        then(coverageRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("It should save an ANALYST together with the coverage of states")
    void verificationSucessAdminCreateAnalystUser() {

        //ARRANGE
        AdminCreateUserRequestDTO userDTO = new AdminCreateUserRequestDTO("Bob", "bob@test.com", "123456", RoleUser.ANALYST, Set.of("SP", "RJ"));

        when(userRepository.existsByEmail("bob@test.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("HASH");

        //ACT
        service.createUser(userDTO);

        //ASSERT
        then(userRepository).should().save(userCaptor.capture());
        then(coverageRepository).should().save(coverageCaptor.capture());

        Assertions.assertEquals(RoleUser.ANALYST, userCaptor.getValue().getRole());
        Assertions.assertEquals(Set.of("SP", "RJ"), coverageCaptor.getValue().getStates());
        Assertions.assertEquals(userCaptor.getValue(), coverageCaptor.getValue().getUser());
    }

    @Test
    @DisplayName("It should strip and upper-case the states before saving the coverage")
    void verificationSucessAdminCreateAnalystUserNormalizesStates() {

        //ARRANGE
        AdminCreateUserRequestDTO userDTO = new AdminCreateUserRequestDTO("Bob", "bob@test.com", "123456", RoleUser.ANALYST, Set.of(" sp ", "rj"));

        when(userRepository.existsByEmail("bob@test.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("HASH");

        //ACT
        service.createUser(userDTO);

        //ASSERT
        then(coverageRepository).should().save(coverageCaptor.capture());
        Assertions.assertEquals(Set.of("SP", "RJ"), coverageCaptor.getValue().getStates());
    }

    @Test
    @DisplayName("It should not save the user when the email already exists")
    void verificationErrorAdminCreateUserEmailAlreadyExists() {

        //ARRANGE
        AdminCreateUserRequestDTO userDTO = new AdminCreateUserRequestDTO("Ana", "ana@test.com", "123456", RoleUser.ADMIN, null);

        when(userRepository.existsByEmail("ana@test.com")).thenReturn(true);

        //ASSERT + ACT
        EmailAlreadyExistsException exception = Assertions.assertThrows(EmailAlreadyExistsException.class,
                () -> service.createUser(userDTO));
        then(userRepository).should(Mockito.never()).save(any());
        Assertions.assertEquals("This email already exists: ana@test.com", exception.getMessage());
    }

    @Test
    @DisplayName("It should not save the user when the role is CLIENT")
    void verificationErrorAdminCreateUserClientRole() {

        //ARRANGE
        AdminCreateUserRequestDTO userDTO = new AdminCreateUserRequestDTO("Carl", "carl@test.com", "123456", RoleUser.CLIENT, null);

        when(userRepository.existsByEmail("carl@test.com")).thenReturn(false);

        //ASSERT + ACT
        InvalidRoleException exception = Assertions.assertThrows(InvalidRoleException.class, () -> service.createUser(userDTO));
        then(userRepository).should(Mockito.never()).save(any());
        Assertions.assertEquals("Role must be ANALYST or ADMIN. CLIENT role cannot be assigned by admin", exception.getMessage());
    }

    @Test
    @DisplayName("It should not save the coverage when the ANALYST has no states (null)")
    void verificationErrorAdminCreateAnalystUserWithoutStates() {

        //ARRANGE
        AdminCreateUserRequestDTO userDTO = new AdminCreateUserRequestDTO("Bob", "bob@test.com", "123456", RoleUser.ANALYST, null);

        when(userRepository.existsByEmail("bob@test.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("HASH");

        //ASSERT + ACT
        InvalidOperationException exception = Assertions.assertThrows(InvalidOperationException.class, () -> service.createUser(userDTO));
        then(coverageRepository).should(Mockito.never()).save(any());
        Assertions.assertEquals("An ANALYST must be assigned at least one state of coverage", exception.getMessage());
    }

    @Test
    @DisplayName("It should not save the coverage when the ANALYST has no states (empty)")
    void verificationErrorAdminCreateAnalystUserWithEmptyStates() {

        //ARRANGE
        AdminCreateUserRequestDTO userDTO = new AdminCreateUserRequestDTO("Bob", "bob@test.com", "123456", RoleUser.ANALYST, Set.of());

        when(userRepository.existsByEmail("bob@test.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("HASH");

        //ASSERT + ACT
        Assertions.assertThrows(InvalidOperationException.class, () -> service.createUser(userDTO));
        then(coverageRepository).should(Mockito.never()).save(any());
    }

    @Test
    @DisplayName("It should not save the coverage when one of the states is not a valid UF")
    void verificationErrorAdminCreateAnalystUserInvalidState() {

        //ARRANGE
        AdminCreateUserRequestDTO userDTO = new AdminCreateUserRequestDTO("Bob", "bob@test.com", "123456", RoleUser.ANALYST, Set.of("XX"));

        when(userRepository.existsByEmail("bob@test.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("HASH");

        //ASSERT + ACT
        InvalidOperationException exception = Assertions.assertThrows(InvalidOperationException.class, () -> service.createUser(userDTO));
        then(coverageRepository).should(Mockito.never()).save(any());
        Assertions.assertEquals("Invalid state: 'XX'. Must be a valid Brazilian UF.", exception.getMessage());
    }

    // ------------------------------------------------ GET USER ------------------------------------------------

    @Test
    @DisplayName("It should return the user without states when the user is not an ANALYST")
    void verificationSucessAdminGetUserByIdNotAnalyst() {

        //ARRANGE
        User admin = User.builder().id(1L).name("Ana").email("ana@test.com").role(RoleUser.ADMIN).enabled(true).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        //ACT
        UserResponseDTO result = service.getUserById(1L);

        //ASSERT
        Assertions.assertEquals(1L, result.id());
        Assertions.assertNull(result.states());
        then(coverageRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("It should return the states of the coverage when the user is an ANALYST")
    void verificationSucessAdminGetUserByIdAnalyst() {

        //ARRANGE
        User analyst = User.builder().id(2L).name("Bob").email("bob@test.com").role(RoleUser.ANALYST).enabled(true).build();
        AnalystCoverage coverage = AnalystCoverage.builder().user(analyst).states(Set.of("SP")).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(analyst));
        when(coverageRepository.findByUser(analyst)).thenReturn(Optional.of(coverage));

        //ACT
        UserResponseDTO result = service.getUserById(2L);

        //ASSERT
        Assertions.assertEquals(Set.of("SP"), result.states());
    }

    @Test
    @DisplayName("It should throw an exception when the user does not exist")
    void verificationErrorAdminGetUserByIdNotFound() {

        //ARRANGE
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        //ASSERT + ACT
        ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class, () -> service.getUserById(10L));
        Assertions.assertEquals("User not found by id '10'", exception.getMessage());
    }

    @Test
    @DisplayName("It should throw an exception when the ANALYST has no coverage registered")
    void verificationErrorAdminGetUserByIdAnalystWithoutCoverage() {

        //ARRANGE
        User analyst = User.builder().id(2L).role(RoleUser.ANALYST).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(analyst));
        when(coverageRepository.findByUser(analyst)).thenReturn(Optional.empty());

        //ASSERT + ACT
        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.getUserById(2L));
    }
}
