package com.costumi.backend.disfraces.adaptadores.entrada;

import com.costumi.backend.disfraces.aplicacion.ConsultarOpcionesDeSlot;
import com.costumi.backend.disfraces.dominio.EjeDePrenda;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** DTO de salida de la "ruleta" de un slot: sus opciones concretas disponibles (RF-2.3). */
public record SlotOpcionesResponse(int orden, String nombre, EjeDePrenda ejePrenda, boolean opcional,
		List<OpcionDto> opciones, List<FacetaDto> facetas) {

	/** Una opción concreta: prenda, su foto, precio de renta, stock y sus etiquetas (con nombre) para comparar. */
	public record OpcionDto(UUID prendaId, String nombre, String fotoUrl, BigDecimal precioRenta,
			int unidadesDisponibles, List<EtiquetaValorDto> etiquetas) {
	}

	/** Una etiqueta de la opción con tipo y valor legibles: {@code tipoNombre}="Talla", {@code valorNombre}="M". */
	public record EtiquetaValorDto(UUID tipoEtiquetaId, String tipoNombre, UUID valorEtiquetaId, String valorNombre) {
	}

	/** Una dimensión por la que el cliente puede filtrar la ruleta, con sus valores disponibles (RF-2.7.2). */
	public record FacetaDto(UUID tipoEtiquetaId, String tipoNombre, List<ValorDeFacetaDto> valores) {
	}

	/** Un valor de una faceta y cuántas opciones lo tienen dado el resto de filtros (conteo dinámico). */
	public record ValorDeFacetaDto(UUID valorEtiquetaId, String valorNombre, int cantidad) {
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
		List<FacetaDto> facetas = o.facetas().stream()
				.map(f -> new FacetaDto(f.tipoEtiquetaId(), f.tipoNombre(),
						f.valores().stream()
								.map(v -> new ValorDeFacetaDto(v.valorEtiquetaId(), v.valorNombre(), v.cantidad()))
								.toList()))
				.toList();
		return new SlotOpcionesResponse(o.orden(), o.nombre(), o.ejePrenda(), o.opcional(), opciones, facetas);
	}
}
