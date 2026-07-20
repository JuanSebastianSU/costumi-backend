package com.costumi.backend.reportes.adaptadores.entrada;

import com.costumi.backend.reportes.aplicacion.ConsultarGanancia;
import com.costumi.backend.reportes.aplicacion.ConsultarIngresos;
import com.costumi.backend.reportes.aplicacion.ConsultarInventario;
import com.costumi.backend.reportes.aplicacion.ConsultarOperaciones;
import com.costumi.backend.reportes.aplicacion.ConsultarRankings;
import com.costumi.backend.reportes.dominio.ArticuloRanking;
import com.costumi.backend.reportes.dominio.EmpleadoVentas;
import com.costumi.backend.reportes.dominio.GrupoInventario;
import com.costumi.backend.reportes.dominio.IngresosPorMetodo;
import com.costumi.backend.reportes.dominio.ResumenDeGanancia;
import com.costumi.backend.reportes.dominio.ResumenDeIngresos;
import com.costumi.backend.reportes.dominio.ResumenInventario;
import com.costumi.backend.reportes.dominio.ValorEtiquetaRanking;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
	private final ConsultarRankings consultarRankings;
	private final ConsultarInventario consultarInventario;
	private final com.costumi.backend.compartido.GeneradorDePdf pdf;

	ReporteController(ConsultarIngresos consultarIngresos, ConsultarGanancia consultarGanancia,
			ConsultarOperaciones consultarOperaciones, ConsultarRankings consultarRankings,
			ConsultarInventario consultarInventario, com.costumi.backend.compartido.GeneradorDePdf pdf) {
		this.consultarIngresos = consultarIngresos;
		this.consultarGanancia = consultarGanancia;
		this.consultarOperaciones = consultarOperaciones;
		this.consultarRankings = consultarRankings;
		this.consultarInventario = consultarInventario;
		this.pdf = pdf;
	}

	@GetMapping("/ingresos")
	IngresosResponse ingresos(@RequestParam(required = false) UUID sucursalId, @AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		ResumenDeIngresos resumen = (empresaId == null)
				? ResumenDeIngresos.de(BigDecimal.ZERO, BigDecimal.ZERO)
				: consultarIngresos.deEmpresa(UUID.fromString(empresaId), sucursalId);
		return IngresosResponse.desde(resumen);
	}

	@GetMapping("/ganancia")
	GananciaResponse ganancia(@RequestParam(required = false) UUID sucursalId, @AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		ResumenDeGanancia resumen = (empresaId == null)
				? ResumenDeGanancia.de(BigDecimal.ZERO, BigDecimal.ZERO)
				: consultarGanancia.gananciaDeEmpresa(UUID.fromString(empresaId), sucursalId);
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

	@GetMapping("/mas-vendidos")
	List<ArticuloRanking> masVendidos(@RequestParam(required = false) UUID sucursalId,
			@RequestParam(defaultValue = "10") int limite, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return consultarRankings.masVendidos(empresaId, sucursalId, limite);
	}

	@GetMapping("/mas-rentados")
	List<ArticuloRanking> masRentados(@RequestParam(required = false) UUID sucursalId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(defaultValue = "10") int limite, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return consultarRankings.masRentados(empresaId, sucursalId, desde, hasta, limite);
	}

	@GetMapping("/ventas-por-empleado")
	List<EmpleadoVentas> ventasPorEmpleado(@RequestParam(required = false) UUID sucursalId,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return consultarRankings.ventasPorEmpleado(empresaId, sucursalId);
	}

	@GetMapping("/ventas-por-etiqueta")
	List<ValorEtiquetaRanking> ventasPorEtiqueta(@RequestParam UUID tipoEtiquetaId,
			@RequestParam(required = false) UUID sucursalId, @AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return consultarRankings.ventasPorEtiqueta(empresaId, tipoEtiquetaId, sucursalId);
	}

	@GetMapping("/inventario/tablero")
	List<GrupoInventario> tableroDeInventario(@RequestParam(required = false) UUID sucursalId,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return consultarInventario.tableroDeInventario(empresaId, sucursalId);
	}

	@GetMapping("/inventario/resumen")
	ResumenInventario resumenDeInventario(@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		return consultarInventario.resumenDeInventario(empresaId);
	}

	// --- Export CSV y PDF (RF-9.2). ---

	@GetMapping(value = "/export/rentas-vencidas.csv", produces = "text/csv")
	ResponseEntity<String> exportRentasVencidas(@RequestParam(required = false) UUID sucursalId,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		LocalDate hoy = LocalDate.now();
		StringBuilder csv = new StringBuilder("rentaId,clienteId,prendaId,fechaDevolucion,diasVencida,importe,deposito\n");
		for (var r : consultarOperaciones.rentasVencidas(empresaId, sucursalId)) {
			RentaVencidaResponse f = RentaVencidaResponse.desde(r, hoy);
			csv.append(f.rentaId()).append(',').append(f.clienteId()).append(',').append(f.prendaId()).append(',')
					.append(f.fechaDevolucion()).append(',').append(f.diasVencida()).append(',').append(f.importe())
					.append(',').append(f.deposito()).append('\n');
		}
		return csvAdjunto("rentas-vencidas.csv", csv.toString());
	}

	@GetMapping(value = "/export/inventario-tablero.csv", produces = "text/csv")
	ResponseEntity<String> exportInventarioTablero(@RequestParam(required = false) UUID sucursalId,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		StringBuilder csv = new StringBuilder("prendaId,prenda,disponibles,danadas,enLimpieza,perdidas\n");
		for (GrupoInventario g : consultarInventario.tableroDeInventario(empresaId, sucursalId)) {
			csv.append(g.prendaId()).append(',').append(campo(g.prendaNombre())).append(',').append(g.disponibles())
					.append(',').append(g.danadas()).append(',').append(g.enLimpieza()).append(',').append(g.perdidas())
					.append('\n');
		}
		return csvAdjunto("inventario-tablero.csv", csv.toString());
	}

	@GetMapping(value = "/export/rentas-vencidas.pdf", produces = "application/pdf")
	ResponseEntity<byte[]> exportRentasVencidasPdf(@RequestParam(required = false) UUID sucursalId,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		LocalDate hoy = LocalDate.now();
		String[] columnas = { "Renta", "Cliente", "Prenda", "Vence", "Días", "Importe", "Depósito" };
		java.util.List<String[]> filas = new java.util.ArrayList<>();
		for (var r : consultarOperaciones.rentasVencidas(empresaId, sucursalId)) {
			RentaVencidaResponse f = RentaVencidaResponse.desde(r, hoy);
			filas.add(new String[] { texto(f.rentaId()), texto(f.clienteId()), texto(f.prendaId()),
					texto(f.fechaDevolucion()), texto(f.diasVencida()), texto(f.importe()), texto(f.deposito()) });
		}
		return pdfAdjunto("rentas-vencidas.pdf", pdf.tabla("Rentas vencidas", columnas, filas));
	}

	@GetMapping(value = "/export/inventario-tablero.pdf", produces = "application/pdf")
	ResponseEntity<byte[]> exportInventarioTableroPdf(@RequestParam(required = false) UUID sucursalId,
			@AuthenticationPrincipal Jwt jwt) {
		UUID empresaId = UUID.fromString(jwt.getClaimAsString("empresa_id"));
		String[] columnas = { "Prenda", "Disponibles", "Dañadas", "En limpieza", "Perdidas" };
		java.util.List<String[]> filas = new java.util.ArrayList<>();
		for (GrupoInventario g : consultarInventario.tableroDeInventario(empresaId, sucursalId)) {
			filas.add(new String[] { g.prendaNombre(), texto(g.disponibles()), texto(g.danadas()),
					texto(g.enLimpieza()), texto(g.perdidas()) });
		}
		return pdfAdjunto("inventario-tablero.pdf", pdf.tabla("Tablero de inventario", columnas, filas));
	}

	private static String texto(Object valor) {
		return valor == null ? "" : valor.toString();
	}

	private static ResponseEntity<byte[]> pdfAdjunto(String archivo, byte[] cuerpo) {
		return ResponseEntity.ok()
				.header("Content-Disposition", "attachment; filename=" + archivo)
				.contentType(org.springframework.http.MediaType.APPLICATION_PDF)
				.body(cuerpo);
	}

	private static ResponseEntity<String> csvAdjunto(String archivo, String cuerpo) {
		return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=" + archivo).body(cuerpo);
	}

	/** Escapa un texto para CSV: entre comillas si trae coma/comilla/salto, doblando las comillas. */
	private static String campo(String valor) {
		if (valor == null) {
			return "";
		}
		if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
			return '"' + valor.replace("\"", "\"\"") + '"';
		}
		return valor;
	}

	record DepositosActivosResponse(BigDecimal total) {
	}
}
