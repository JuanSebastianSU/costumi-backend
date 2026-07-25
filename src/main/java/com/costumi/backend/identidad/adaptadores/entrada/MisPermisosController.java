package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.GestionarPermisosDeEmpleado;
import com.costumi.backend.identidad.dominio.AccionDePermiso;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Seccion;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Los permisos del PROPIO usuario autenticado (B-permisos): la app arma la navegación a partir de esto
 * —qué secciones ve y en cuáles puede actuar— en vez de deducirlo del rol. Cualquier usuario ve los suyos
 * (se resuelve por el token, nunca por un id del request).
 */
@RestController
@RequestMapping("/api/v1/empleados/me/permisos")
class MisPermisosController {

	private final GestionarPermisosDeEmpleado permisos;

	MisPermisosController(GestionarPermisosDeEmpleado permisos) {
		this.permisos = permisos;
	}

	@GetMapping
	List<PermisoDto> mios(@AuthenticationPrincipal Jwt jwt) {
		UUID usuarioId = UUID.fromString(jwt.getSubject());
		Rol rol = Rol.valueOf(jwt.getClaimAsString("rol"));
		return permisos.mios(usuarioId, rol).stream()
				.map(p -> new PermisoDto(p.seccion(), p.accion()))
				.toList();
	}

	/** Un permiso concedido: la sección y la acción (ver/actuar). Lo que no está, no lo tiene. */
	record PermisoDto(Seccion seccion, AccionDePermiso accion) {
	}
}
