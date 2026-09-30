package com.arenahub.aplicacion;

import com.arenahub.dominio.Usuario;
import com.arenahub.persistencia.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** HU1 — Registro de usuario. */
@Service
public class RegistroUsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public RegistroUsuarioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario registrar(String nombre, String email, String contrasena) {
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByEmail(emailNormalizado)) {
            throw new ConflictoException("El correo ya está registrado");
        }
        Usuario usuario = new Usuario(nombre.trim(), emailNormalizado, passwordEncoder.encode(contrasena));
        try {
            return usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultáneos con el mismo correo: la restricción UNIQUE lo impide
            throw new ConflictoException("El correo ya está registrado");
        }
    }
}
