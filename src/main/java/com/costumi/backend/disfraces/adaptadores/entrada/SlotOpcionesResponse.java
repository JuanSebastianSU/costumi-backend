package com.costumi.backend.disfraces.adaptadores.entrada;

import com.costumi.backend.disfraces.aplicacion.ConsultarOpcionesDeSlot;
import com.costumi.backend.disfraces.dominio.EjeDePrenda;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** DTO de salida de la "ruleta" de un slot: sus opciones concretas disponibles (RF-2.3). */
public record SlotOpcionesResponse(int orden, String nombre, EjeDePrenda ejePrenda, boolean opcional,
		List<OpcionDto> opciones) {

	/** Una opción concreta: prenda, su foto, precio de renta, stock y sus valores de etiqueta para filtrar. */
	public record OpcionDto(UUID prendaId, String nombre, String fotoUrl, BigDecimal precioRenta,
			int unidadesDisponibles, List<EtiquetaValorDto> etiquetas) {
	}

	public record EtiquetaValorDto(UUID tipoEtiquetaId, UUID valorEtiquetaId) {
	}

	static SlotOpcionesResponse desde(ConsultarOpcionesDeSlot.OpcionesDeSlot o) {
		List<OpcionDto> opciones = o.opciones().stream()
				.map(op -> new OpcionDto(op.prendaId(), op.nombre(), op.fotoUrl(), op.precioRenta(),
						op.unidadesDisponibles(),
						op.etiquetas().entrySet().stream()
								.map(e -> new EtiquetaValorDto(e.getKey(), e.getValue()))
								.toList()))
				.toList();
		return new SlotOpcionesResponse(o.orden(), o.nombre(), o.ejePrenda(), o.opcional(), opciones);
	}
}
