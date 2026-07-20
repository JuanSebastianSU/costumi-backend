package com.costumi.backend.devoluciones.adaptadores.entrada;

import com.costumi.backend.devoluciones.dominio.Devolucion;
import com.costumi.backend.inventario.ConsultaDeInventario.ResumenDePrenda;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** DTO de salida de la Devolución con su liquidación y checklist (cada pieza con nombre y foto de la prenda). */
public record DevolucionResponse(UUID id, UUID rentaId, BigDecimal deposito, BigDecimal cargoPorDanos,
		BigDecimal cargoPorRetraso, BigDecimal remanente, BigDecimal multa, List<PiezaResponse> piezas) {

	public record PiezaResponse(UUID prendaId, String nombre, String fotoUrl, String descripcion, boolean llego,
			String estado, boolean perdidaCobrada, boolean resuelta) {
	}

	/** Sin resumen de prendas: las piezas van sin nombre/foto (usos internos, p.ej. al registrar). */
	static DevolucionResponse desde(Devolucion d) {
		return desde(d, Map.of());
	}

	/** Enriquecida: cada pieza toma nombre y foto de {@code resumenes} (por prendaId) para el listado/desglose. */
	static DevolucionResponse desde(Devolucion d, Map<UUID, ResumenDePrenda> resumenes) {
		List<PiezaResponse> piezas = d.piezas().stream()
				.map(p -> {
					ResumenDePrenda r = resumenes.get(p.prendaId());
					return new PiezaResponse(p.prendaId(), r == null ? null : r.nombre(),
							r == null ? null : r.fotoUrl(), p.descripcion(), p.llego(), p.estado().name(),
							p.perdidaCobrada(), p.estaResuelta());
				})
				.toList();
		return new DevolucionResponse(d.id(), d.rentaId(), d.deposito(), d.cargoPorDanos(), d.cargoPorRetraso(),
				d.remanente(), d.multa(), piezas);
	}
}
