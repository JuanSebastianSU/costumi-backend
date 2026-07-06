package com.costumi.backend.caja.adaptadores.entrada;

import com.costumi.backend.caja.dominio.EstadoTurno;
import com.costumi.backend.caja.dominio.MetodoDePago;
import com.costumi.backend.caja.dominio.Turno;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** DTO de salida del Turno: cabecera + corte por método + cuadre de efectivo (si está cerrado). */
public record TurnoResponse(UUID id, UUID sucursalId, UUID empleadoId, BigDecimal fondoInicial, String estado,
		BigDecimal efectivoContado, Map<String, BigDecimal> corte, BigDecimal diferenciaEfectivo,
		List<MovimientoResponse> movimientos) {

	public record MovimientoResponse(String tipo, String concepto, BigDecimal monto, String metodo) {
	}

	static TurnoResponse desde(Turno t) {
		Map<String, BigDecimal> corte = new LinkedHashMap<>();
		for (MetodoDePago metodo : MetodoDePago.values()) {
			corte.put(metodo.name(), t.totalPorMetodo(metodo));
		}
		BigDecimal diferencia = (t.estado() == EstadoTurno.CERRADO) ? t.diferenciaDeEfectivo() : null;
		List<MovimientoResponse> movimientos = t.movimientos().stream()
				.map(m -> new MovimientoResponse(m.tipo().name(), m.concepto(), m.monto(), m.metodo().name()))
				.toList();
		return new TurnoResponse(t.id(), t.sucursalId(), t.empleadoId(), t.fondoInicial(), t.estado().name(),
				t.efectivoContado(), corte, diferencia, movimientos);
	}
}
