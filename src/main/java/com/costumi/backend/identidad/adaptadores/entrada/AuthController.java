package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.AutenticarUsuario;
import com.costumi.backend.identidad.aplicacion.Credenciales;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Puerta de entrada de autenticación (RF-1.1, RF-17.4). */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

	private final AutenticarUsuario autenticarUsuario;

	AuthController(AutenticarUsuario autenticarUsuario) {
		this.autenticarUsuario = autenticarUsuario;
	}

	/** Login: email + contraseña → token de acceso. */
	@PostMapping("/login")
	TokenResponse login(@Valid @RequestBody LoginRequest request) {
		Credenciales credenciales = autenticarUsuario.autenticar(request.email(), request.password());
		return new TokenResponse(credenciales.accessToken(), "Bearer");
	}

	/** Identidad del usuario autenticado (requiere token válido). */
	@GetMapping("/me")
	UsuarioActualResponse me(@AuthenticationPrincipal Jwt jwt) {
		return new UsuarioActualResponse(
				jwt.getSubject(),
				jwt.getClaimAsString("email"),
				jwt.getClaimAsString("rol"),
				jwt.getClaimAsString("empresa_id"));
	}
}
