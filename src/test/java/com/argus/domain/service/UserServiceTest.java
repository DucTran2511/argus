package com.argus.domain.service;

import com.argus.domain.model.User;
import com.argus.domain.port.persistence.UserPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserPersistencePort userPersistencePort;

    @InjectMocks
    private UserService userService;

    private final String SUB = "test-uid";
    private final String EMAIL = "test@example.com";
    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(UUID.randomUUID())
                .supabaseUid(SUB)
                .email(EMAIL)
                .build();
    }

    @Test
    void getOrCreateUser_shouldReturnExistingUser() {
        when(userPersistencePort.findBySupabaseUid(SUB)).thenReturn(Optional.of(existingUser));

        User result = userService.getOrCreateUser(SUB, EMAIL);

        assertEquals(existingUser, result);
        verify(userPersistencePort, never()).save(any());
    }

    @Test
    void getOrCreateUser_shouldProvideNewUser() {
        when(userPersistencePort.findBySupabaseUid(SUB)).thenReturn(Optional.empty());
        when(userPersistencePort.save(any())).thenReturn(existingUser);

        User result = userService.getOrCreateUser(SUB, EMAIL);

        assertEquals(existingUser, result);
        verify(userPersistencePort).save(any());
    }

    @Test
    void getOrCreateUser_shouldHandleRaceCondition() {
        when(userPersistencePort.findBySupabaseUid(SUB))
                .thenReturn(Optional.empty()) // First check
                .thenReturn(Optional.of(existingUser)); // Recovery check

        when(userPersistencePort.save(any())).thenThrow(new DataIntegrityViolationException("Duplicate"));

        User result = userService.getOrCreateUser(SUB, EMAIL);

        assertEquals(existingUser, result);
        verify(userPersistencePort, times(2)).findBySupabaseUid(SUB);
    }
}
