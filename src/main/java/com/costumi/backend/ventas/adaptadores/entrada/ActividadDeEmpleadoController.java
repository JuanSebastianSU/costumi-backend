package com.costumi.backend.ventas.adaptadores.entrada;

import com.costumi.backend.ventas.ConsultaDeVentas;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Registro de actividad de ventas de un empleado (RF-8.2/1.4). Vive en el módulo de Ventas porque lee
 * su información; acotado al tenant del token.
 */
@RestController
class ActividadDeEmpleadoController {

	private final ConsultaDeVentas ventas;

	ActividadDeEmpleadoController(ConsultaDeVentas ventas) {
		this.ventas = ventas;
	}

	@GetMapping("/api/v1/empleados/{usuarioId}/actividad")
	ActividadResponse actividad(@PathVariable UUID usuarioId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		ConsultaDeVentas.ActividadDeEmpleado actividad = ventas.actividadDeEmpleado(empresaId, usuarioId);
		return new ActividadResponse(usuarioId, actividad.ventas(), actividad.totalVendido());
	}

	record ActividadResponse(UUID empleadoId, long ventas, BigDecimal totalVendido) {
	}
}
