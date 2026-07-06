package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.AltaDeEmpleado;
import com.costumi.backend.identidad.dominio.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/** Empleados de una empresa (RF-8), acotados al tenant del token. Alta por DUENO/ENCARGADO. */
@RestController
@RequestMapping("/api/v1/empleados")
class EmpleadoController {

	private final AltaDeEmpleado altaDeEmpleado;

	EmpleadoController(AltaDeEmpleado altaDeEmpleado) {
		this.altaDeEmpleado = altaDeEmpleado;
	}

	@PostMapping
	ResponseEntity<EmpleadoResponse> crear(@Valid @RequestBody AltaDeEmpleadoRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		Usuario empleado = altaDeEmpleado.ejecutar(new AltaDeEmpleado.AltaDeEmpleadoComando(
				empresaId, request.email(), request.password(), request.rol()));
		URI location = uriBuilder.path("/api/v1/empleados/{id}").buildAndExpand(empleado.id()).toUri();
		return ResponseEntity.created(location).body(EmpleadoResponse.desde(empleado));
	}
}
