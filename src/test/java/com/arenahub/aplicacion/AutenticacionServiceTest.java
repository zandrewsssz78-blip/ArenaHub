package com.arenahub.aplicacion;

import com.arenahub.dominio.Usuario;
import com.arenahub.persistencia.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticacionServiceTest {

    @Mock
    private UsuarioRepository usuarios;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AutenticacionService service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario(1L, "Juan Perez", "juan@example.com", "hash_seguro_123");
    }

    @Test
    void hu2_autenticacionExitosaRetornaTokenYUsuario() {
        when(usuarios.findByEmail("juan@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password123", "hash_seguro_123")).thenReturn(true);

        AutenticacionService.SesionUsuario sesion = service.autenticar("juan@example.com", "password123");

        assertThat(sesion).isNotNull();
        assertThat(sesion.token()).isNotBlank();
        assertThat(sesion.usuario().getId()).isEqualTo(1L);
        assertThat(sesion.usuario().getEmail()).isEqualTo("juan@example.com");
    }

    @Test
    void hu2_autenticacionFallaSiEmailNoExisteSinRevelarCausa() {
        when(usuarios.findByEmail("noexiste@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.autenticar("noexiste@example.com", "password123"))
                .isInstanceOf(AutenticacionException.class)
                .hasMessage("Credenciales inválidas");

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void hu2_autenticacionFallaSiContrasenaIncorrectaSinRevelarCausa() {
        when(usuarios.findByEmail("juan@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password_erroneo", "hash_seguro_123")).thenReturn(false);

        assertThatThrownBy(() -> service.autenticar("juan@example.com", "password_erroneo"))
                .isInstanceOf(AutenticacionException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    void hu2_autenticacionFallaSiUsuarioInactivo() {
        ReflectionTestUtils.setField(usuario, "activo", false);
        when(usuarios.findByEmail("juan@example.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.autenticar("juan@example.com", "password123"))
                .isInstanceOf(AutenticacionException.class)
                .hasMessage("Credenciales inválidas");
    }
}
