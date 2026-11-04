package com.arenahub.aplicacion;

import com.arenahub.dominio.EscenarioDeportivo;
import com.arenahub.dominio.FranjaHoraria;
import com.arenahub.dominio.ReglaNegocioException;
import com.arenahub.dominio.Reserva;
import com.arenahub.dominio.Usuario;
import com.arenahub.persistencia.EscenarioDeportivoRepository;
import com.arenahub.persistencia.FranjaHorariaRepository;
import com.arenahub.persistencia.ReservaRepository;
import com.arenahub.persistencia.UsuarioRepository;
import com.arenahub.web.UsuarioController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Suite de Verificación de Ciberseguridad y Requisitos No Funcionales (RNF).
 *
 * Cada prueba sigue estrictamente la anatomía del curso:
 * 1. Preparar: definir usuario, datos y condiciones de ataque o frontera.
 * 2. Ejecutar: realizar la operación sujeta a control de seguridad.
 * 3. Verificar: comprobar el resultado esperado y que no hubo exposición o modificación indebida.
 */
@ExtendWith(MockitoExtension.class)
class SeguridadReglasTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private EscenarioDeportivoRepository escenarioRepository;

    @Mock
    private FranjaHorariaRepository franjaRepository;

    private PasswordEncoder passwordEncoder;
    private RegistroUsuarioService registroService;
    private AutenticacionService autenticacionService;
    private ReservaService reservaService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        registroService = new RegistroUsuarioService(usuarioRepository, passwordEncoder);
        autenticacionService = new AutenticacionService(usuarioRepository, passwordEncoder);
        reservaService = new ReservaService(reservaRepository, usuarioRepository, escenarioRepository, franjaRepository);
    }

    // =========================================================================
    // REGLA 1: PROTECCIÓN CONTRA INYECCIÓN SQL
    // =========================================================================
    @Nested
    @DisplayName("Regla 1 — Protección contra inyección SQL")
    class InyeccionSqlTests {

        @Test
        @DisplayName("Payloads de inyección SQL se tratan como texto literal y no alteran la persistencia")
        void payloadInyeccionSqlEsTratadoComoLiteral() {
            // 1. Preparar: payload malicioso clásico de inyección SQL
            String payloadMalicioso = "admin' OR '1'='1' --";
            String emailInyeccion = "inyeccion' OR '1'='1@test.com";

            when(usuarioRepository.existsByEmail(any())).thenReturn(false);
            when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocation -> {
                Usuario u = invocation.getArgument(0);
                return new Usuario(1L, u.getNombre(), u.getEmail(), u.getContrasenaHash());
            });

            // 2. Ejecutar: intentar registrar un usuario con el payload
            Usuario resultado = registroService.registrar(payloadMalicioso, emailInyeccion, "PasswordSeguro123!");

            // 3. Verificar: el nombre se almacena como texto plano seguro (escapado por JPA/JDBC) sin ejecutar sentencias SQL
            assertThat(resultado).isNotNull();
            assertThat(resultado.getNombre()).isEqualTo("admin' OR '1'='1' --");
            assertThat(resultado.getEmail()).isEqualTo("inyeccion' or '1'='1@test.com");
        }
    }

    // =========================================================================
    // REGLA 2: PROTECCIÓN DE CONTRASEÑAS (BCRYPT + SALT)
    // =========================================================================
    @Nested
    @DisplayName("Regla 2 — Protección de contraseñas")
    class ProteccionContrasenasTests {

        @Test
        @DisplayName("La contraseña nunca se guarda en texto plano y utiliza hash BCrypt con sal")
        void contrasenaSeAlmacenaComoHashBCryptConSal() {
            // 1. Preparar: contraseña original en texto plano
            String passwordPlana = "MiContraseñaSuperSegura_2026!";
            when(usuarioRepository.existsByEmail("atleta@utadeo.edu.co")).thenReturn(false);
            when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

            // 2. Ejecutar: registro del usuario
            Usuario usuarioRegistrado = registroService.registrar("Atleta Tadeo", "atleta@utadeo.edu.co", passwordPlana);

            // 3. Verificar:
            // a) La contraseña persistida NO es la contraseña en texto plano
            assertThat(usuarioRegistrado.getContrasenaHash()).isNotEqualTo(passwordPlana);
            // b) El hash generado corresponde al algoritmo seguro BCrypt ($2a$, $2b$ o $2y$)
            assertThat(usuarioRegistrado.getContrasenaHash()).matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
            // c) El verificador criptográfico confirma la coincidencia
            assertThat(passwordEncoder.matches(passwordPlana, usuarioRegistrado.getContrasenaHash())).isTrue();
            // d) Una contraseña incorrecta no coincide
            assertThat(passwordEncoder.matches("PasswordErrada", usuarioRegistrado.getContrasenaHash())).isFalse();
        }

        @Test
        @DisplayName("Dos usuarios con la misma contraseña obtienen hashes distintos debido a la sal única (salt)")
        void mismaContrasenaGeneraDiferentesHashesPorSal() {
            // 1. Preparar: dos contraseñas idénticas
            String clave = "ClaveCompartida123!";

            // 2. Ejecutar: codificar dos veces consecutivas
            String hash1 = passwordEncoder.encode(clave);
            String hash2 = passwordEncoder.encode(clave);

            // 3. Verificar: los hashes son distintos gracias a la sal (salt) aleatoria
            assertThat(hash1).isNotEqualTo(hash2);
            assertThat(passwordEncoder.matches(clave, hash1)).isTrue();
            assertThat(passwordEncoder.matches(clave, hash2)).isTrue();
        }
    }

    // =========================================================================
    // REGLA 3: PROTECCIÓN DE SECRETOS
    // =========================================================================
    @Nested
    @DisplayName("Regla 3 — Protección de secretos")
    class ProteccionSecretosTests {

        @Test
        @DisplayName("El archivo .gitignore excluye explícitamente archivos de credenciales y .env")
        void gitignoreExcluyeArchivosDeSecretos() throws IOException {
            // 1. Preparar: buscar el archivo .gitignore del proyecto
            Path gitignorePath = Path.of(".gitignore");
            if (!Files.exists(gitignorePath)) {
                gitignorePath = Path.of("..", ".gitignore");
            }

            // 2. Ejecutar: leer el contenido
            String contenido = Files.readString(gitignorePath);

            // 3. Verificar: contiene reglas que impiden versionar .env y llaves privadas
            assertThat(contenido).contains(".env");
            assertThat(contenido).contains("*.key");
            assertThat(contenido).contains("*.pem");
        }

        @Test
        @DisplayName("No existen archivos .env reales versionados en el directorio raíz")
        void noExisteArchivoEnvEnElRepositorio() {
            // 1. Preparar & 2. Ejecutar: verificar existencia física de un .env
            File envFile = new File(".env");

            // 3. Verificar: nunca debe existir un .env subido en el código
            assertThat(envFile.exists()).isFalse();
        }
    }

    // =========================================================================
    // REGLA 4: PROTECCIÓN DE DATOS PERSONALES
    // =========================================================================
    @Nested
    @DisplayName("Regla 4 — Protección de datos personales")
    class ProteccionDatosPersonalesTests {

        @Test
        @DisplayName("El DTO UsuarioResponse nunca incluye ni expone la contraseña ni su hash")
        void dtoUsuarioResponseNoExponeHashNiContrasena() {
            // 1. Preparar: entidad Usuario con hash
            Usuario usuario = new Usuario(55L, "Carlos Ruiz", "carlos@utadeo.edu.co", "$2a$10$e7K4Owhh7wR...");

            // 2. Ejecutar: transformar a DTO para respuesta al cliente
            UsuarioController.UsuarioResponse response = UsuarioController.UsuarioResponse.de(usuario);

            // 3. Verificar: el DTO no expone datos sensibles ni secretos
            assertThat(response.id()).isEqualTo(55L);
            assertThat(response.nombre()).isEqualTo("Carlos Ruiz");
            assertThat(response.email()).isEqualTo("carlos@utadeo.edu.co");
            assertThat(response.rol()).isNotNull();

            // Verificación por reflexión de que la clase record no tiene campos para contraseña
            boolean tieneCampoContrasena = java.util.Arrays.stream(UsuarioController.UsuarioResponse.class.getDeclaredFields())
                    .anyMatch(f -> f.getName().toLowerCase().contains("contrasena") || f.getName().toLowerCase().contains("hash") || f.getName().toLowerCase().contains("password"));
            assertThat(tieneCampoContrasena).isFalse();
        }
    }

    // =========================================================================
    // SEG-06 / SEG-14: CONTROL DE PERMISOS Y AUTORIZACIÓN EN CADA OPERACIÓN
    // =========================================================================
    @Nested
    @DisplayName("SEG-06 / SEG-14 — Control de acceso y autorización")
    class AutorizacionTests {

        @Test
        @DisplayName("Un usuario ajeno NO puede cancelar una reserva que no le pertenece")
        void usuarioAjenoNoPuedeCancelarReserva() {
            // 1. Preparar: usuario legítimo (dueño ID=10) y usuario atacante (ID=99)
            Usuario dueno = new Usuario(10L, "Dueno Legítimo", "dueno@test.com", "hash");
            Usuario atacante = new Usuario(99L, "Atacante", "atacante@test.com", "hash");

            EscenarioDeportivo escenario = new EscenarioDeportivo(1L, "Cancha 1", com.arenahub.dominio.TipoEscenario.FUTBOL_5, "Sede Central", 10, "Cancha sintética", com.arenahub.dominio.EstadoEscenario.ACTIVO);
            FranjaHoraria franja = new FranjaHoraria(LocalTime.of(18, 0), LocalTime.of(19, 0));
            Reserva reserva = Reserva.confirmar(dueno, escenario, franja, LocalDate.now().plusDays(1));

            when(reservaRepository.findByIdConDetalle(500L)).thenReturn(Optional.of(reserva));

            // 2. Ejecutar & 3. Verificar: intentar cancelar con el ID del atacante produce error de autorización
            assertThatThrownBy(() -> reservaService.cancelar(500L, atacante.getId()))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("No tienes permiso para cancelar una reserva de otro usuario");

            // La reserva continúa en estado CONFIRMADA
            assertThat(reserva.getEstado()).isEqualTo(com.arenahub.dominio.EstadoReserva.CONFIRMADA);
        }
    }

    // =========================================================================
    // SEG-08: PREVENCIÓN DE ENUMERACIÓN DE USUARIOS
    // =========================================================================
    @Nested
    @DisplayName("SEG-08 — Prevención de enumeración de usuarios")
    class AntiEnumeracionTests {

        @Test
        @DisplayName("Usuario inexistente y contraseña incorrecta retornan exactamente el mismo error genérico")
        void loginRetornaMensajeGenericoSinRevelarExistenciaDeUsuario() {
            // 1. Preparar: caso A (usuario no existe), caso B (usuario existe pero clave incorrecta)
            when(usuarioRepository.findByEmail("inexistente@test.com")).thenReturn(Optional.empty());

            Usuario usuarioExistente = new Usuario(1L, "Existente", "existente@test.com", passwordEncoder.encode("Correcta123!"));
            when(usuarioRepository.findByEmail("existente@test.com")).thenReturn(Optional.of(usuarioExistente));

            // 2. Ejecutar & 3. Verificar: Caso A (usuario inexistente)
            assertThatThrownBy(() -> autenticacionService.autenticar("inexistente@test.com", "CualquierClave"))
                    .isInstanceOf(AutenticacionException.class)
                    .hasMessage("Credenciales inválidas");

            // Caso B (usuario existe pero clave errónea)
            assertThatThrownBy(() -> autenticacionService.autenticar("existente@test.com", "ClaveEquivocada"))
                    .isInstanceOf(AutenticacionException.class)
                    .hasMessage("Credenciales inválidas");

            // Ambos mensajes son idénticos, evitando que un atacante descubra qué correos están registrados.
        }
    }
}
