package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.GestionarPermisosDeEmpleado;
import com.costumi.backend.identidad.dominio.AccionDePermiso;
import com.costumi.backend.identidad.dominio.Seccion;
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

/** Editor de permisos granular por empleado (RF-1.5), acotado al tenant del dueño/encargado. */
@RestController
@RequestMapping("/api/v1/empleados/{usuarioId}/permisos")
class PermisosEmpleadoController {

	private final GestionarPermisosDeEmpleado permisos;

	PermisosEmpleadoController(GestionarPermisosDeEmpleado permisos) {
		this.permisos = permisos;
	}

	@GetMapping
	List<PermisoDto> matriz(@PathVariable UUID usuarioId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return permisos.matriz(empresaId, usuarioId).stream()
				.map(p -> new PermisoDto(p.seccion(), p.accion(), p.concedido()))
				.toList();
	}

	@PutMapping
	void establecer(@PathVariable UUID usuarioId, @RequestBody EstablecerPermisoRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		permisos.establecer(empresaId, usuarioId, request.seccion(), request.accion(), request.concedido());
	}

	record PermisoDto(Seccion seccion, AccionDePermiso accion, boolean concedido) {
	}

	record EstablecerPermisoRequest(@NotNull Seccion seccion, @NotNull AccionDePermiso accion, boolean concedido) {
	}
}
