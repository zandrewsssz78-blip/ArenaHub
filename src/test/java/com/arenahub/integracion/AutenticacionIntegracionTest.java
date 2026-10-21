package com.arenahub.integracion;

import com.arenahub.web.UsuarioController.LoginRequest;
import com.arenahub.web.UsuarioController.LoginResponse;
import com.arenahub.web.UsuarioController.RegistroRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/** HU2 — Inicio de sesión / Autenticación (criterios de docs/historias-usuario.md). */
class AutenticacionIntegracionTest extends PruebaIntegracionBase {

    @Test
    void credencialesValidasRetornaTokenYUsuario() {
        String email = unico("login") + "@example.com";
        http.postForEntity("/api/usuarios", new RegistroRequest("Usuario Valido", email, "claveSegura123"), String.class);

        ResponseEntity<LoginResponse> respuesta = http.postForEntity(
                "/api/usuarios/login",
                new LoginRequest(email, "claveSegura123"),
                LoginResponse.class
        );

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().token()).isNotBlank();
        assertThat(respuesta.getBody().usuario().email()).isEqualTo(email.toLowerCase());
    }

    @Test
    void credencialesIncorrectasRetorna401SinRevelarCampo() {
        String email = unico("login_bad") + "@example.com";
        http.postForEntity("/api/usuarios", new RegistroRequest("Usuario Valido", email, "claveSegura123"), String.class);

        ResponseEntity<String> respuesta = http.postForEntity(
                "/api/usuarios/login",
                new LoginRequest(email, "claveErronea"),
                String.class
        );

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody()).contains("Credenciales inválidas");
    }

    @Test
    void correoNoRegistradoRetorna401SinRevelarCampo() {
        ResponseEntity<String> respuesta = http.postForEntity(
                "/api/usuarios/login",
                new LoginRequest("noexiste_" + unico("user") + "@example.com", "cualquiera123"),
                String.class
        );

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody()).contains("Credenciales inválidas");
    }
}
