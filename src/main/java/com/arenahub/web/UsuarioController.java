package com.arenahub.web;

import com.arenahub.aplicacion.RegistroUsuarioService;
import com.arenahub.dominio.RolUsuario;
import com.arenahub.dominio.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** HU1 — Registro de usuario. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final RegistroUsuarioService registro;

    public UsuarioController(RegistroUsuarioService registro) {
        this.registro = registro;
    }

    public record RegistroRequest(
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72) String contrasena) {
    }

    /** Nunca se devuelve la contraseña ni su hash. */
    public record UsuarioResponse(Long id, String nombre, String email, RolUsuario rol) {
        static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest request) {
        return UsuarioResponse.de(registro.registrar(request.nombre(), request.email(), request.contrasena()));
    }
}
