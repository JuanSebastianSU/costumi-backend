package com.costumi.backend.disfraces.adaptadores.entrada;

import com.costumi.backend.disfraces.aplicacion.ConsultarOpcionesDeSlot;
import com.costumi.backend.disfraces.dominio.EjeDePrenda;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** DTO de salida de la "ruleta" de un slot: sus opciones concretas disponibles (RF-2.3). */
public record SlotOpcionesResponse(int orden, String nombre, EjeDePrenda ejePrenda, boolean opcional,
		List<OpcionDto> opciones) {

	/** Una opción concreta: prenda, su foto, precio de renta, stock y sus etiquetas (con nombre) para comparar. */
	public record OpcionDto(UUID prendaId, String nombre, String fotoUrl, BigDecimal precioRenta,
			int unidadesDisponibles, List<EtiquetaValorDto> etiquetas) {
	}

	/** Una etiqueta de la opción con tipo y valor legibles: {@code tipoNombre}="Talla", {@code valorNombre}="M". */
	public record EtiquetaValorDto(UUID tipoEtiquetaId, String tipoNombre, UUID valorEtiquetaId, String valorNombre) {
	}

	static SlotOpcionesResponse desde(ConsultarOpcionesDeSlot.OpcionesDeSlot o) {
		List<OpcionDto> opciones = o.opciones().stream()
				.map(op -> new OpcionDto(op.prendaId(), op.nombre(), op.fotoUrl(), op.precioRenta(),
						op.unidadesDisponibles(),
						op.etiquetas().stream()
								.map(e -> new EtiquetaValorDto(e.tipoEtiquetaId(), e.tipoNombre(),
										e.valorEtiquetaId(), e.valorNombre()))
								.toList()))
				.toList();
		return new SlotOpcionesResponse(o.orden(), o.nombre(), o.ejePrenda(), o.opcional(), opciones);
	}
}
