package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.Credenciales;
import com.costumi.backend.identidad.aplicacion.GestionarMembresias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Membresías del usuario y cambio de contexto (Fase B): a qué tiendas pertenece y cómo "entrar" a una de
 * ellas (obtener un token con esa empresa+rol). Todo por el usuario del token, nunca por un id del request.
 */
@RestController
@RequestMapping("/api/v1/auth")
class MembresiaController {

	private final GestionarMembresias membresias;

	MembresiaController(GestionarMembresias membresias) {
		this.membresias = membresias;
	}

	/** Las tiendas del usuario con su rol en cada una (para la pantalla "elegir tienda"). */
	@GetMapping("/me/membresias")
	List<MembresiaResponse> mias(@AuthenticationPrincipal Jwt jwt) {
		UUID usuarioId = UUID.fromString(jwt.getSubject());
		return membresias.deUsuario(usuarioId).stream()
				.map(m -> new MembresiaResponse(m.empresaId(), m.empresaNombre(), m.rol().name(), m.estado().name()))
				.toList();
	}

	/** Cambia el contexto activo a una tienda: devuelve un token nuevo con la empresa+rol de esa membresía. */
	@PostMapping("/contexto")
	TokenResponse cambiarContexto(@Valid @RequestBody CambiarContextoRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID usuarioId = UUID.fromString(jwt.getSubject());
		Credenciales cred = membresias.cambiarContexto(usuarioId, request.empresaId());
		return new TokenResponse(cred.accessToken(), cred.refreshToken(), "Bearer");
	}

	record MembresiaResponse(UUID empresaId, String empresaNombre, String rol, String estado) {
	}

	record CambiarContextoRequest(@NotNull UUID empresaId) {
	}
}
