package com.arenahub.web;

import com.arenahub.aplicacion.AutenticacionException;
import com.arenahub.aplicacion.AutenticacionService;
import com.arenahub.aplicacion.RegistroUsuarioService;
import com.arenahub.dominio.RolUsuario;
import com.arenahub.dominio.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de Ciberseguridad a nivel Web/HTTP:
 * - Regla 4 & SEG-12: Enmascaramiento de errores sin revelación de stack traces.
 * - SEG-05: Validación estricta de entradas en la frontera de la API.
 */
@ExtendWith(MockitoExtension.class)
class SeguridadWebTest {

    private MockMvc mockMvc;

    @Mock
    private RegistroUsuarioService registroService;

    @Mock
    private AutenticacionService autenticacionService;

    @BeforeEach
    void setUp() {
        UsuarioController controller = new UsuarioController(registroService, autenticacionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    // =========================================================================
    // SEG-05: VALIDACIÓN DE TODAS LAS ENTRADAS DEL USUARIO
    // =========================================================================
    @Test
    @DisplayName("SEG-05: Solicitud de registro con datos inválidos es rechazada con HTTP 400 sin procesar")
    void solicitudConEntradaInvalidaRetornaBadRequest() throws Exception {
        // 1. Preparar: JSON con correo mal formado y contraseña demasiado corta (< 8 caracteres)
        String jsonInvalido = """
            {
                "nombre": "",
                "email": "correo-invalido-sin-arroba",
                "contrasena": "123"
            }
            """;

        // 2. Ejecutar & 3. Verificar: la frontera rechaza con HTTP 400 y explica los campos sin filtrar trazas
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.mensaje", containsString("email")))
                .andExpect(jsonPath("$.mensaje", containsString("contrasena")));
    }

    // =========================================================================
    // REGLA 4 & SEG-12: EVITAR REVELAR DETALLES INTERNOS EN ERRORES
    // =========================================================================
    @Test
    @DisplayName("Regla 4 & SEG-12: Un fallo interno no controlado retorna HTTP 500 limpio sin exponer stack trace")
    void errorInternoNoRevelaStackTraceNiDetallesDeBaseDeDatos() throws Exception {
        // 1. Preparar: simular un error técnico imprevisto (ej. fallo de persistencia o null pointer)
        when(registroService.registrar(any(), any(), any()))
                .thenThrow(new RuntimeException("Error simulado de bajo nivel: Failed to connect to DB at 192.168.1.100"));

        String jsonValido = """
            {
                "nombre": "Usuario Prueba",
                "email": "prueba@utadeo.edu.co",
                "contrasena": "PasswordLargoYValido123"
            }
            """;

        // 2. Ejecutar & 3. Verificar: respuesta 500 que NO expone direcciones IP, nombres de tabla ni stack trace
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.mensaje", is("Ha ocurrido un error interno. Intente más tarde.")))
                // Garantizar que la respuesta NO expone la traza interna
                .andExpect(content().string(not(containsString("192.168.1.100"))))
                .andExpect(content().string(not(containsString("RuntimeException"))))
                .andExpect(content().string(not(containsString("at com.arenahub"))));
    }

    // =========================================================================
    // REGLA 4: NO EXPOSICIÓN DE CONTRASEÑA EN RESPUESTA HTTP
    // =========================================================================
    @Test
    @DisplayName("Regla 4: Respuesta exitosa de registro no contiene campos de contraseña ni hash")
    void respuestaRegistroNoContieneContrasenaNiHash() throws Exception {
        // 1. Preparar: usuario registrado con hash interno
        Usuario usuario = new Usuario(1L, "Atleta", "atleta@utadeo.edu.co", "$2a$10$abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
        when(registroService.registrar(any(), any(), any())).thenReturn(usuario);

        String jsonValido = """
            {
                "nombre": "Atleta",
                "email": "atleta@utadeo.edu.co",
                "contrasena": "Password123456"
            }
            """;

        // 2. Ejecutar & 3. Verificar: el JSON resultante solo contiene id, nombre, email y rol
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nombre", is("Atleta")))
                .andExpect(jsonPath("$.email", is("atleta@utadeo.edu.co")))
                .andExpect(jsonPath("$.contrasena").doesNotExist())
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
