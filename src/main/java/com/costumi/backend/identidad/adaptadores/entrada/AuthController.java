package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.AutenticarUsuario;
import com.costumi.backend.identidad.aplicacion.Credenciales;
import com.costumi.backend.identidad.aplicacion.RecuperarContrasena;
import com.costumi.backend.identidad.aplicacion.RefrescarToken;
import com.costumi.backend.identidad.aplicacion.RegistrarCliente;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
	private final RegistrarCliente registrarCliente;
	private final RecuperarContrasena recuperarContrasena;

	AuthController(AutenticarUsuario autenticarUsuario, RefrescarToken refrescarToken,
			RegistrarCliente registrarCliente, RecuperarContrasena recuperarContrasena) {
		this.autenticarUsuario = autenticarUsuario;
		this.refrescarToken = refrescarToken;
		this.registrarCliente = registrarCliente;
		this.recuperarContrasena = recuperarContrasena;
	}

	/** Login: email + contraseña → token de acceso + token de refresco. */
	@PostMapping("/login")
	TokenResponse login(@Valid @RequestBody LoginRequest request) {
		Credenciales credenciales = autenticarUsuario.autenticar(request.email(), request.password());
		return new TokenResponse(credenciales.accessToken(), credenciales.refreshToken(), "Bearer");
	}

	/** Auto-registro de un cliente (usuario final): crea la cuenta y devuelve tokens (auto-login). */
	@PostMapping("/registro")
	TokenResponse registro(@Valid @RequestBody RegistroRequest request) {
		Credenciales credenciales = registrarCliente.ejecutar(request.email(), request.password());
		return new TokenResponse(credenciales.accessToken(), credenciales.refreshToken(), "Bearer");
	}

	/** Olvidé mi contraseña (RF-1.1): si el correo existe, envía un token por email. Siempre 204 (no revela). */
	@PostMapping("/olvide")
	ResponseEntity<Void> olvide(@Valid @RequestBody OlvideRequest request) {
		recuperarContrasena.solicitar(request.email());
		return ResponseEntity.noContent().build();
	}

	/** Restablecer contraseña (RF-1.1): valida el token y cambia la contraseña. */
	@PostMapping("/restablecer")
	ResponseEntity<Void> restablecer(@Valid @RequestBody RestablecerRequest request) {
		recuperarContrasena.restablecer(request.token(), request.password());
		return ResponseEntity.noContent().build();
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
