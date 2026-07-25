package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.compartido.ContextoDeTenant;
import com.costumi.backend.identidad.aplicacion.GestionarPermisosDeEmpleado;
import com.costumi.backend.identidad.dominio.Capacidad;
import com.costumi.backend.identidad.dominio.Rol;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Editor de la matriz de capacidades por empleado (Fase B, paso 5), acotado al tenant y la pirámide. La matriz
 * trae TODAS las capacidades agrupables por sección (con su descripción), para la pantalla rediseñada.
 */
@RestController
@RequestMapping("/api/v1/empleados/{usuarioId}/permisos")
class PermisosEmpleadoController {

	private final GestionarPermisosDeEmpleado permisos;
	private final ContextoDeTenant tenant;

	PermisosEmpleadoController(GestionarPermisosDeEmpleado permisos, ContextoDeTenant tenant) {
		this.permisos = permisos;
		this.tenant = tenant;
	}

	@GetMapping
	List<CapacidadDto> matriz(@PathVariable UUID usuarioId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		Rol actorRol = Rol.valueOf(jwt.getClaimAsString("rol"));
		return permisos.matriz(empresaId, actorRol, usuarioId).stream()
				.map(c -> new CapacidadDto(c.capacidad().seccion().name(), c.capacidad().name(),
						c.capacidad().descripcion(), c.concedido()))
				.toList();
	}

	@PutMapping
	void establecer(@PathVariable UUID usuarioId, @RequestBody EstablecerPermisoRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = tenant.empresaIdRequerida();
		Rol actorRol = Rol.valueOf(jwt.getClaimAsString("rol"));
		UUID actorId = UUID.fromString(jwt.getSubject());
		permisos.establecer(empresaId, actorRol, actorId, usuarioId, request.capacidad(), request.concedido());
	}

	record CapacidadDto(String seccion, String capacidad, String descripcion, boolean concedido) {
	}

	record EstablecerPermisoRequest(@NotNull Capacidad capacidad, boolean concedido) {
	}
}
