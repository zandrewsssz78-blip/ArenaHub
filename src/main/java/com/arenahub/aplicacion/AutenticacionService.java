package com.arenahub.aplicacion;

import com.arenahub.dominio.Usuario;
import com.arenahub.persistencia.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/** HU2 — Inicio de sesión / Autenticación. */
@Service
public class AutenticacionService {

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
        Usuario usuario = usuarios.findByEmail(emailNormalizado)
                .orElseThrow(() -> new AutenticacionException("Credenciales inválidas"));

        if (!usuario.isActivo() || contrasena == null || !passwordEncoder.matches(contrasena, usuario.getContrasenaHash())) {
            throw new AutenticacionException("Credenciales inválidas");
        }

        String token = UUID.randomUUID().toString();
        return new SesionUsuario(token, usuario);
    }
}
