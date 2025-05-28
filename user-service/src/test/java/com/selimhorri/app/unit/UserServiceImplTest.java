package com.selimhorri.app.unitarios;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User buildUser() {
        return User.builder()
                .userId(1)
                .firstName("Daniel")
                .lastName("Morales")
                .email("daniel@icesi.edu.co")
                .credential(new Credential()) // evitar NullPointer en mappers
                .addresses(new HashSet<>())   // evitar NullPointer en mappers
                .build();
    }

    private UserDto buildUserDto() {
        return UserDto.builder()
                .userId(1)
                .firstName("Daniel")
                .lastName("Morales")
                .email("daniel@icesi.edu.co")
                .credentialDto(CredentialDto.builder().build())
                .addressDtos(new HashSet<>())
                .build();
    }

    @Test
    void testFindById_existingId_returnsUserDto() {
        User user = buildUser();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        UserDto result = userService.findById(1);

        assertNotNull(result);
        assertEquals("Daniel", result.getFirstName());
        verify(userRepository, times(1)).findById(1);
    }

    @Test
    void testFindById_notFound_throwsException() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(UserObjectNotFoundException.class, () -> userService.findById(99));
    }

    @Test
    void testFindAll_returnsListOfUserDtos() {
        List<User> users = List.of(buildUser());
        when(userRepository.findAll()).thenReturn(users);

        List<UserDto> result = userService.findAll();

        assertEquals(1, result.size());
        assertEquals("Daniel", result.get(0).getFirstName());
    }

    @Test
    void testSave_returnsSavedUserDto() {
        User inputUser = buildUser();
        when(userRepository.save(any(User.class))).thenReturn(inputUser);

        UserDto saved = userService.save(buildUserDto());

        assertNotNull(saved);
        assertEquals("Daniel", saved.getFirstName());
    }

    @Test
    void testDeleteById_executesWithoutError() {
        doNothing().when(userRepository).deleteById(1);
        assertDoesNotThrow(() -> userService.deleteById(1));
        verify(userRepository).deleteById(1);
    }
}
