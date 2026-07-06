package com.costumi.backend.reportes.adaptadores.entrada;

import com.costumi.backend.reportes.aplicacion.ConsultarGanancia;
import com.costumi.backend.reportes.aplicacion.ConsultarIngresos;
import com.costumi.backend.reportes.aplicacion.ConsultarOperaciones;
import com.costumi.backend.reportes.dominio.IngresosPorMetodo;
import com.costumi.backend.reportes.dominio.ResumenDeGanancia;
import com.costumi.backend.reportes.dominio.ResumenDeIngresos;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Reportes (RF-9), solo lectura y acotados al tenant del token. */
@RestController
@RequestMapping("/api/v1/reportes")
class ReporteController {

	private final ConsultarIngresos consultarIngresos;
	private final ConsultarGanancia consultarGanancia;
	private final ConsultarOperaciones consultarOperaciones;

	ReporteController(ConsultarIngresos consultarIngresos, ConsultarGanancia consultarGanancia,
			ConsultarOperaciones consultarOperaciones) {
		this.consultarIngresos = consultarIngresos;
		this.consultarGanancia = consultarGanancia;
		this.consultarOperaciones = consultarOperaciones;
	}

	@GetMapping("/ingresos")
	IngresosResponse ingresos(@AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		ResumenDeIngresos resumen = (empresaId == null)
				? ResumenDeIngresos.de(BigDecimal.ZERO, BigDecimal.ZERO)
				: consultarIngresos.deEmpresa(UUID.fromString(empresaId));
		return IngresosResponse.desde(resumen);
	}

	@GetMapping("/ganancia")
	GananciaResponse ganancia(@AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		ResumenDeGanancia resumen = (empresaId == null)
				? ResumenDeGanancia.de(BigDecimal.ZERO, BigDecimal.ZERO)
				: consultarGanancia.gananciaDeEmpresa(UUID.fromString(empresaId));
		return GananciaResponse.desde(resumen);
	}

	@GetMapping("/rentas-vencidas")
	List<RentaVencidaResponse> rentasVencidas(@RequestParam(required = false) UUID sucursalId,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		LocalDate hoy = LocalDate.now();
		return consultarOperaciones.rentasVencidas(empresaId, sucursalId).stream()
				.map(r -> RentaVencidaResponse.desde(r, hoy)).toList();
	}

	@GetMapping("/depositos-activos")
	DepositosActivosResponse depositosActivos(@RequestParam(required = false) UUID sucursalId,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return new DepositosActivosResponse(consultarOperaciones.depositosActivos(empresaId, sucursalId));
	}

	@GetMapping("/ingresos-por-metodo")
	IngresosPorMetodo ingresosPorMetodo(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(required = false) UUID sucursalId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return consultarOperaciones.ingresosPorMetodo(empresaId, desde, hasta, sucursalId);
	}

	record DepositosActivosResponse(BigDecimal total) {
	}
}
