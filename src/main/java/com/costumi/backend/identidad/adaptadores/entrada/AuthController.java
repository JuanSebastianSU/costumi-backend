package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.AutenticarUsuario;
import com.costumi.backend.identidad.aplicacion.Credenciales;
import com.costumi.backend.identidad.aplicacion.RefrescarToken;
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
	private final RefrescarToken refrescarToken;

	AuthController(AutenticarUsuario autenticarUsuario, RefrescarToken refrescarToken) {
		this.autenticarUsuario = autenticarUsuario;
		this.refrescarToken = refrescarToken;
	}

	/** Login: email + contraseña → token de acceso + token de refresco. */
	@PostMapping("/login")
	TokenResponse login(@Valid @RequestBody LoginRequest request) {
		Credenciales credenciales = autenticarUsuario.autenticar(request.email(), request.password());
		return new TokenResponse(credenciales.accessToken(), credenciales.refreshToken(), "Bearer");
	}

	/** Refresh: token de refresco → nuevo par de tokens (RF-1.1). */
	@PostMapping("/refresh")
	TokenResponse refrescar(@Valid @RequestBody RefreshRequest request) {
		Credenciales credenciales = refrescarToken.ejecutar(request.refreshToken());
		return new TokenResponse(credenciales.accessToken(), credenciales.refreshToken(), "Bearer");
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
