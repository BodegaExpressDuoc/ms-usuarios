package cl.duoc.bodegaexpress.usuarios.controller;

import cl.duoc.bodegaexpress.usuarios.dto.LoginRequest;
import cl.duoc.bodegaexpress.usuarios.dto.LoginResponse;
import cl.duoc.bodegaexpress.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return new LoginResponse(usuarioService.iniciarSesion(request.email(), request.password()));
    }
}
