package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.GestionarPermisosDeEmpleado;
import com.costumi.backend.identidad.dominio.Rol;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Las capacidades del PROPIO usuario autenticado (Fase B, paso 5): la app arma la navegación a partir de esto
 * —qué puede hacer— en vez de deducirlo del rol. Cualquier usuario ve las suyas (por el token, nunca por un
 * id del request).
 */
@RestController
@RequestMapping("/api/v1/empleados/me/permisos")
class MisPermisosController {

	private final GestionarPermisosDeEmpleado permisos;

	MisPermisosController(GestionarPermisosDeEmpleado permisos) {
		this.permisos = permisos;
	}

	@GetMapping
	List<CapacidadDto> mias(@AuthenticationPrincipal Jwt jwt) {
		UUID usuarioId = UUID.fromString(jwt.getSubject());
		Rol rol = Rol.valueOf(jwt.getClaimAsString("rol"));
		return permisos.mias(usuarioId, rol).stream()
				.map(c -> new CapacidadDto(c.seccion().name(), c.name(), c.descripcion()))
				.toList();
	}

	/** Una capacidad concedida: su sección, su clave y qué habilita. Lo que no está, no lo tiene. */
	record CapacidadDto(String seccion, String capacidad, String descripcion) {
	}
}
