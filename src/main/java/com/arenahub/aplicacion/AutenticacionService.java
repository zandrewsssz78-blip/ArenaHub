package com.arenahub.aplicacion;

import com.arenahub.dominio.Usuario;
import com.arenahub.persistencia.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * HU2 — Inicio de sesión / Autenticación.
 * Regla 2 (BCrypt matches), Regla 4 (Mínima exposición) & SEG-08 (Prevención de enumeración de usuarios).
 */
@Service
public class AutenticacionService {

    private static final Logger log = LoggerFactory.getLogger(AutenticacionService.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public AutenticacionService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    public record SesionUsuario(String token, Usuario usuario) {
    }

    @Transactional(readOnly = true)
    public SesionUsuario autenticar(String email, String contrasena) {
        String emailNormalizado = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
        log.info("Intento de autenticación recibido para email: {}", emailNormalizado);

        Usuario usuario = usuarios.findByEmail(emailNormalizado)
                .orElse(null);

        // SEG-08: Mensaje genérico de error que previene enumeración de usuarios.
        // Se ejecuta verificación constante o se rechaza con el mismo mensaje si el usuario no existe.
        if (usuario == null || !usuario.isActivo() || contrasena == null || !passwordEncoder.matches(contrasena, usuario.getContrasenaHash())) {
            log.warn("Autenticación rechazada para email: {}", emailNormalizado);
            throw new AutenticacionException("Credenciales inválidas");
        }

        log.info("Autenticación exitosa para usuario id: {}", usuario.getId());
        String token = UUID.randomUUID().toString();
        return new SesionUsuario(token, usuario);
    }
}
