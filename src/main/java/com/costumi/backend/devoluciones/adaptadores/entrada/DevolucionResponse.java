package com.costumi.backend.devoluciones.adaptadores.entrada;

import com.costumi.backend.devoluciones.dominio.Devolucion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** DTO de salida de la Devolución con su liquidación y checklist. */
public record DevolucionResponse(UUID id, UUID rentaId, BigDecimal deposito, BigDecimal cargoPorDanos,
		BigDecimal cargoPorRetraso, BigDecimal remanente, BigDecimal multa, List<PiezaResponse> piezas) {

	public record PiezaResponse(String descripcion, boolean llego, String estado) {
	}

	static DevolucionResponse desde(Devolucion d) {
		List<PiezaResponse> piezas = d.piezas().stream()
				.map(p -> new PiezaResponse(p.descripcion(), p.llego(), p.estado().name()))
				.toList();
		return new DevolucionResponse(d.id(), d.rentaId(), d.deposito(), d.cargoPorDanos(), d.cargoPorRetraso(),
				d.remanente(), d.multa(), piezas);
	}
}
