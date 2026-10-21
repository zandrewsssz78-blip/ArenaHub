package com.arenahub.integracion;

import com.arenahub.dominio.TipoEscenario;
import com.arenahub.web.EscenarioController.CrearEscenarioRequest;
import com.arenahub.web.ReservaController.CancelarReservaRequest;
import com.arenahub.web.ReservaController.CrearReservaRequest;
import com.arenahub.web.ReservaController.ReservaResponse;
import com.arenahub.web.UsuarioController.LoginRequest;
import com.arenahub.web.UsuarioController.LoginResponse;
import com.arenahub.web.UsuarioController.RegistroRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Casos borde de las historias de usuario (integración HTTP + PostgreSQL).
 * Cada prueba documenta el límite que cubre y la historia a la que pertenece.
 */
class CasosBordeIntegracionTest extends PruebaIntegracionBase {

    // ---------- HU4: crear reserva ----------

    /** Defecto corregido: antes se aceptaban reservas en fechas pasadas. */
    @Test
    void hu4_reservaEnFechaPasadaSeRechazaYNoSePersiste() {
        Long escenario = crearEscenario("ACTIVO");
        LocalDate ayer = enDias(-1);

        ResponseEntity<String> respuesta = http.postForEntity("/api/reservas",
                new CrearReservaRequest(crearUsuario(), escenario, ayer, franjaDeLas(10)), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(respuesta.getBody()).contains("fecha pasada");
        assertThat(reservasDe(escenario)).isZero();
    }

    /** Límite inferior válido: el mismo día sí se puede reservar. */
    @Test
    void hu4_reservaParaHoySeAcepta() {
        ResponseEntity<String> respuesta = http.postForEntity("/api/reservas",
                new CrearReservaRequest(crearUsuario(), crearEscenario("ACTIVO"), enDias(0), franjaDeLas(21)),
                String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    /** El conflicto es solo misma cancha + fecha + franja: otra franja no choca. */
    @Test
    void hu4_mismoEscenarioYFechaEnOtraFranjaNoEsConflicto() {
        Long escenario = crearEscenario("ACTIVO");
        Long usuario = crearUsuario();
        LocalDate fecha = enDias(30);

        HttpStatus primera = reservar(usuario, escenario, fecha, franjaDeLas(14));
        HttpStatus segunda = reservar(usuario, escenario, fecha, franjaDeLas(15));

        assertThat(primera).isEqualTo(HttpStatus.CREATED);
        assertThat(segunda).isEqualTo(HttpStatus.CREATED);
    }

    /** La misma franja y fecha en otra cancha tampoco es conflicto. */
    @Test
    void hu4_mismaFranjaYFechaEnOtroEscenarioNoEsConflicto() {
        LocalDate fecha = enDias(31);
        Long franja = franjaDeLas(16);

        HttpStatus enCancha1 = reservar(crearUsuario(), crearEscenario("ACTIVO"), fecha, franja);
        HttpStatus enCancha2 = reservar(crearUsuario(), crearEscenario("ACTIVO"), fecha, franja);

        assertThat(enCancha1).isEqualTo(HttpStatus.CREATED);
        assertThat(enCancha2).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void hu4_reservaConFranjaInexistenteResponde404() {
        HttpStatus status = reservar(crearUsuario(), crearEscenario("ACTIVO"), enDias(32), 999_999L);

        assertThat(status).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void hu4_reservaConUsuarioInexistenteResponde404() {
        HttpStatus status = reservar(999_999L, crearEscenario("ACTIVO"), enDias(33), franjaDeLas(10));

        assertThat(status).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void hu4_reservaSinFechaResponde400() {
        HttpStatus status = reservar(crearUsuario(), crearEscenario("ACTIVO"), null, franjaDeLas(10));

        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---------- HU5: cancelar reserva ----------

    @Test
    void hu5_cancelarDosVecesLaMismaReservaSeRechazaLaSegunda() {
        Long usuario = crearUsuario();
        ResponseEntity<ReservaResponse> creada = http.postForEntity("/api/reservas",
                new CrearReservaRequest(usuario, crearEscenario("ACTIVO"), enDias(34), franjaDeLas(12)),
                ReservaResponse.class);
        Long id = creada.getBody().id();

        HttpStatus primera = cancelar(id, usuario);
        HttpStatus segunda = cancelar(id, usuario);

        assertThat(primera).isEqualTo(HttpStatus.OK);
        assertThat(segunda).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // ---------- HU7: registrar escenario ----------

    /**
     * Defecto corregido: con capacidad 0 la validación lo dejaba pasar, la base de
     * datos lo rechazaba (chk_escenario_capacidad) y la API respondía un 409
     * engañoso de "nombre duplicado". Ahora es un 400 de validación.
     */
    @Test
    void hu7_escenarioConCapacidadCeroResponde400() {
        CrearEscenarioRequest request = new CrearEscenarioRequest(
                unico("Cancha"), TipoEscenario.VOLEIBOL, "Sede Sur", 0, null);

        ResponseEntity<String> respuesta = http.postForEntity("/api/escenarios", request, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).contains("capacidad");
    }

    /** El nombre repetido se detecta sin importar mayúsculas. */
    @Test
    void hu7_nombreRepetidoConOtrasMayusculasEsConflicto() {
        String nombre = unico("Coliseo");
        http.postForEntity("/api/escenarios",
                new CrearEscenarioRequest(nombre, TipoEscenario.BALONCESTO, "Sede Norte", 20, null), String.class);

        ResponseEntity<String> repetido = http.postForEntity("/api/escenarios",
                new CrearEscenarioRequest(nombre.toUpperCase(), TipoEscenario.BALONCESTO, "Sede Norte", 20, null),
                String.class);

        assertThat(repetido.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ---------- HU1 / HU2: registro e inicio de sesión ----------

    @Test
    void hu1_contrasenaDeMenosDe8CaracteresResponde400() {
        ResponseEntity<String> respuesta = http.postForEntity("/api/usuarios",
                new RegistroRequest("Ana", unico("corta") + "@example.com", "1234567"), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /** El correo se normaliza: registrarse y entrar con distintas mayúsculas funciona. */
    @Test
    void hu2_loginIgnoraMayusculasDelCorreo() {
        String correo = unico("Ana.Mayus") + "@Example.com";
        http.postForEntity("/api/usuarios", new RegistroRequest("Ana", correo, "secreta123"), String.class);

        ResponseEntity<LoginResponse> respuesta = http.postForEntity("/api/usuarios/login",
                new LoginRequest(correo.toUpperCase(), "secreta123"), LoginResponse.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().token()).isNotBlank();
    }

    // ---------- utilidades ----------

    private HttpStatus reservar(Long usuario, Long escenario, LocalDate fecha, Long franja) {
        return HttpStatus.valueOf(http.postForEntity("/api/reservas",
                new CrearReservaRequest(usuario, escenario, fecha, franja), String.class).getStatusCode().value());
    }

    private HttpStatus cancelar(Long reservaId, Long usuarioId) {
        return HttpStatus.valueOf(http.exchange("/api/reservas/" + reservaId + "/cancelar", HttpMethod.PATCH,
                new HttpEntity<>(new CancelarReservaRequest(usuarioId)), String.class).getStatusCode().value());
    }

    private int reservasDe(Long escenario) {
        return jdbc.queryForObject("SELECT count(*) FROM reserva WHERE escenario_id = ?", Integer.class, escenario);
    }
}
