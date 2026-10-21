package com.arenahub.web;

import com.arenahub.aplicacion.AutenticacionService;
import com.arenahub.aplicacion.RegistroUsuarioService;
import com.arenahub.dominio.RolUsuario;
import com.arenahub.dominio.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** HU1 — Registro de usuario & HU2 — Inicio de sesión. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final RegistroUsuarioService registro;
    private final AutenticacionService autenticacion;

    public UsuarioController(RegistroUsuarioService registro, AutenticacionService autenticacion) {
        this.registro = registro;
        this.autenticacion = autenticacion;
    }

    public record RegistroRequest(
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72) String contrasena) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank String contrasena) {
    }

    /** Nunca se devuelve la contraseña ni su hash. */
    public record UsuarioResponse(Long id, String nombre, String email, RolUsuario rol) {
        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol());
        }
    }

    public record LoginResponse(String token, UsuarioResponse usuario) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest request) {
        return UsuarioResponse.de(registro.registrar(request.nombre(), request.email(), request.contrasena()));
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var sesion = autenticacion.autenticar(request.email(), request.contrasena());
        return new LoginResponse(sesion.token(), UsuarioResponse.de(sesion.usuario()));
    }
}
