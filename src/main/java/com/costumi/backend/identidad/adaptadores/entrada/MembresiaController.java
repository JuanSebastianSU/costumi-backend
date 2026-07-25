package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.Credenciales;
import com.costumi.backend.identidad.aplicacion.GestionarMembresias;
import com.costumi.backend.identidad.aplicacion.GestionarMembresiaDeEmpleado;
import com.costumi.backend.identidad.aplicacion.ModoDeSesion;
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
	private final GestionarMembresiaDeEmpleado desvinculacion;

	MembresiaController(GestionarMembresias membresias, GestionarMembresiaDeEmpleado desvinculacion) {
		this.membresias = membresias;
		this.desvinculacion = desvinculacion;
	}

	/** Las tiendas del usuario con su rol en cada una (historial de trabajo; a lo sumo una ACTIVA). */
	@GetMapping("/me/membresias")
	List<MembresiaResponse> mias(@AuthenticationPrincipal Jwt jwt) {
		UUID usuarioId = UUID.fromString(jwt.getSubject());
		return membresias.deUsuario(usuarioId).stream()
				.map(m -> new MembresiaResponse(m.empresaId(), m.empresaNombre(), m.rol().name(), m.estado().name()))
				.toList();
	}

	/**
	 * Alterna el contexto de la sesión (H1): {@code COMPRA} → token de cliente; {@code TRABAJO} → token con
	 * la empresa+rol de la membresía activa. Devuelve un token nuevo (400 si pide TRABAJO sin membresía activa).
	 */
	@PostMapping("/contexto")
	TokenResponse cambiarContexto(@Valid @RequestBody CambiarContextoRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID usuarioId = UUID.fromString(jwt.getSubject());
		Credenciales cred = membresias.cambiarContexto(usuarioId, request.modo());
		return new TokenResponse(cred.accessToken(), cred.refreshToken(), "Bearer");
	}

	/** El empleado se desvincula de su tienda (Fase B, paso 3): queda solo-cliente. Por su propio token. */
	@PostMapping("/me/desvincularme")
	MembresiaEstadoResponse desvincularme(@AuthenticationPrincipal Jwt jwt) {
		UUID usuarioId = UUID.fromString(jwt.getSubject());
		return MembresiaEstadoResponse.desde(desvinculacion.desvincularme(usuarioId));
	}

	record MembresiaResponse(UUID empresaId, String empresaNombre, String rol, String estado) {
	}

	record CambiarContextoRequest(@NotNull ModoDeSesion modo) {
	}
}
