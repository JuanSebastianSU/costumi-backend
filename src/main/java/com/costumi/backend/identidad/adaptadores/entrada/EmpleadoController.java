package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.AltaDeEmpleado;
import com.costumi.backend.identidad.aplicacion.AsignarSucursales;
import com.costumi.backend.identidad.dominio.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Empleados de una empresa (RF-8), acotados al tenant del token. Alta por DUENO/ENCARGADO. */
@RestController
@RequestMapping("/api/v1/empleados")
class EmpleadoController {

	private final AltaDeEmpleado altaDeEmpleado;
	private final AsignarSucursales asignarSucursales;

	EmpleadoController(AltaDeEmpleado altaDeEmpleado, AsignarSucursales asignarSucursales) {
		this.altaDeEmpleado = altaDeEmpleado;
		this.asignarSucursales = asignarSucursales;
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

	/** Asigna el empleado a una o varias sucursales (RF-1.2/8.1). */
	@PutMapping("/{usuarioId}/sucursales")
	List<UUID> asignarSucursales(@PathVariable UUID usuarioId, @Valid @RequestBody AsignarSucursalesRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		asignarSucursales.asignar(empresaId, usuarioId, Set.copyOf(request.sucursalIds()));
		return asignarSucursales.sucursalesDe(empresaId, usuarioId);
	}

	@GetMapping("/{usuarioId}/sucursales")
	List<UUID> sucursalesDe(@PathVariable UUID usuarioId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return asignarSucursales.sucursalesDe(empresaId, usuarioId);
	}

	record AsignarSucursalesRequest(@NotNull List<UUID> sucursalIds) {
	}
}
