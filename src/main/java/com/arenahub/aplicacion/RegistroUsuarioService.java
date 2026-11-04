package com.arenahub.aplicacion;

import com.arenahub.dominio.Usuario;
import com.arenahub.persistencia.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * HU1 — Registro de usuario.
 * Regla 2 (BCrypt), Regla 4 (Datos Personales) & SEG-10 (Logs sin secretos).
 */
@Service
public class RegistroUsuarioService {

    private static final Logger log = LoggerFactory.getLogger(RegistroUsuarioService.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public RegistroUsuarioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario registrar(String nombre, String email, String contrasena) {
        String emailNormalizado = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
        log.info("Procesando solicitud de registro para email: {}", emailNormalizado);

        if (usuarios.existsByEmail(emailNormalizado)) {
            log.warn("Registro rechazado: el correo {} ya existe", emailNormalizado);
            throw new ConflictoException("El correo ya está registrado");
        }

        // SEG-02: La contraseña NUNCA se persiste en texto plano ni se envía a logs. Se utiliza BCrypt con sal automática.
        Usuario usuario = new Usuario(nombre != null ? nombre.trim() : "", emailNormalizado, passwordEncoder.encode(contrasena));
        try {
            Usuario guardado = usuarios.saveAndFlush(usuario);
            log.info("Usuario registrado exitosamente con id: {}", guardado.getId());
            return guardado;
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultáneos con el mismo correo: la restricción UNIQUE lo impide
            log.warn("Conflicto de concurrencia al registrar correo: {}", emailNormalizado);
            throw new ConflictoException("El correo ya está registrado");
        }
    }
}
