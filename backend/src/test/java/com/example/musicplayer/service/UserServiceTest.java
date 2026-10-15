package com.example.musicplayer.service;

import com.example.musicplayer.exception.UserOperationException;
import com.example.musicplayer.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UserService service = new UserService(users, encoder);

    @Test
    void rejectsInvalidRegistrationBeforeHashingOrSaving() {
        for (String username : new String[]{null, " ", "x".repeat(65), "bad\nname"}) {
            assertThatThrownBy(() -> service.register(username, "password"))
                    .isInstanceOf(UserOperationException.class);
        }
        for (String password : new String[]{null, " ", "x".repeat(73), "密".repeat(25)}) {
            assertThatThrownBy(() -> service.register("listener", password))
                    .isInstanceOf(UserOperationException.class);
        }
        verifyNoInteractions(users, encoder);
    }

    @Test
    void normalizesUsernameAndKeepsPasswordUnmodified() {
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
        when(encoder.encode(" password ")).thenReturn("hash");
        var user = service.register(" listener ", " password ");
        assertThat(user.getUsername()).isEqualTo("listener");
        assertThat(user.getPassword()).isEqualTo("hash");
    }
}
