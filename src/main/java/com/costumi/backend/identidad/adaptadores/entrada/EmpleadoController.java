package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.AltaDeEmpleado;
import com.costumi.backend.identidad.aplicacion.AsignarSucursales;
import com.costumi.backend.identidad.aplicacion.GestionarEstadoDeEmpleado;
import com.costumi.backend.identidad.aplicacion.ListarEmpleados;
import com.costumi.backend.identidad.dominio.Rol;
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
	private final GestionarEstadoDeEmpleado gestionarEstadoDeEmpleado;
	private final ListarEmpleados listarEmpleados;

	EmpleadoController(AltaDeEmpleado altaDeEmpleado, AsignarSucursales asignarSucursales,
			GestionarEstadoDeEmpleado gestionarEstadoDeEmpleado, ListarEmpleados listarEmpleados) {
		this.altaDeEmpleado = altaDeEmpleado;
		this.asignarSucursales = asignarSucursales;
		this.gestionarEstadoDeEmpleado = gestionarEstadoDeEmpleado;
		this.listarEmpleados = listarEmpleados;
	}

	/**
	 * Lista el personal de la empresa (RF-8, G1). Con la guarda de pirámide (B3): el actor solo ve a quienes
	 * puede gestionar (estrictamente por debajo suyo). DUENO/ENCARGADO.
	 */
	@GetMapping
	List<EmpleadoDetalleResponse> listar(@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return listarEmpleados.delTenant(empresaId, actorRol(jwt)).stream()
				.map(EmpleadoDetalleResponse::desde)
				.toList();
	}

	/** Da de baja a un empleado (RF-8): no podrá autenticarse ni renovar sesión. DUENO/ENCARGADO. */
	@PostMapping("/{usuarioId}/desactivar")
	EmpleadoResponse desactivar(@PathVariable UUID usuarioId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		UUID actorId = UUID.fromString(jwt.getSubject());
		return EmpleadoResponse.desde(gestionarEstadoDeEmpleado.desactivar(empresaId, actorRol(jwt), actorId, usuarioId));
	}

	/** Reactiva a un empleado dado de baja. DUENO/ENCARGADO. */
	@PostMapping("/{usuarioId}/activar")
	EmpleadoResponse activar(@PathVariable UUID usuarioId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return EmpleadoResponse.desde(gestionarEstadoDeEmpleado.activar(empresaId, actorRol(jwt), usuarioId));
	}

	@PostMapping
	ResponseEntity<EmpleadoResponse> crear(@Valid @RequestBody AltaDeEmpleadoRequest request,
			@AuthenticationPrincipal Jwt jwt, UriComponentsBuilder uriBuilder) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		Usuario empleado = altaDeEmpleado.ejecutar(new AltaDeEmpleado.AltaDeEmpleadoComando(
				empresaId, actorRol(jwt), request.email(), request.password(), request.rol()));
		URI location = uriBuilder.path("/api/v1/empleados/{id}").buildAndExpand(empleado.id()).toUri();
		return ResponseEntity.created(location).body(EmpleadoResponse.desde(empleado));
	}

	/** Asigna el empleado a una o varias sucursales (RF-1.2/8.1). */
	@PutMapping("/{usuarioId}/sucursales")
	List<UUID> asignarSucursales(@PathVariable UUID usuarioId, @Valid @RequestBody AsignarSucursalesRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		asignarSucursales.asignar(empresaId, actorRol(jwt), usuarioId, Set.copyOf(request.sucursalIds()));
		return asignarSucursales.sucursalesDe(empresaId, actorRol(jwt), usuarioId);
	}

	@GetMapping("/{usuarioId}/sucursales")
	List<UUID> sucursalesDe(@PathVariable UUID usuarioId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return asignarSucursales.sucursalesDe(empresaId, actorRol(jwt), usuarioId);
	}

	private static Rol actorRol(Jwt jwt) {
		return Rol.valueOf(jwt.getClaimAsString("rol"));
	}

	record AsignarSucursalesRequest(@NotNull List<UUID> sucursalIds) {
	}
}
