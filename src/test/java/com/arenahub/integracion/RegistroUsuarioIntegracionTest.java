package com.arenahub.integracion;

import com.arenahub.web.UsuarioController.RegistroRequest;
import com.arenahub.web.UsuarioController.UsuarioResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/** HU1 — Registro de usuario (criterios de docs/historias-usuario.md). */
class RegistroUsuarioIntegracionTest extends PruebaIntegracionBase {

    @Test
    void correoNuevoCreaUsuarioConIdYContrasenaHasheada() {
        String email = unico("ana") + "@example.com";

        ResponseEntity<UsuarioResponse> respuesta = http.postForEntity("/api/usuarios",
                new RegistroRequest("Ana Sofía", email, "secreta123"), UsuarioResponse.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody().id()).isNotNull();

        String hash = jdbc.queryForObject("SELECT contrasena_hash FROM usuario WHERE email = ?",
                String.class, email);
        assertThat(hash).isNotEqualTo("secreta123").startsWith("$2");
    }

    @Test
    void correoYaRegistradoSeRechazaYNoPersisteNada() {
        String email = unico("andres") + "@example.com";
        http.postForEntity("/api/usuarios", new RegistroRequest("Andrés", email, "secreta123"), String.class);

        ResponseEntity<String> repetido = http.postForEntity("/api/usuarios",
                new RegistroRequest("Otro", email.toUpperCase(), "otraClave123"), String.class);

        assertThat(repetido.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Integer cantidad = jdbc.queryForObject("SELECT count(*) FROM usuario WHERE email = ?",
                Integer.class, email);
        assertThat(cantidad).isEqualTo(1);
    }
}
