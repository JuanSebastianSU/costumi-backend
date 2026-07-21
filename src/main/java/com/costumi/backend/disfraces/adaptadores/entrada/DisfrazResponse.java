package com.costumi.backend.disfraces.adaptadores.entrada;

import com.costumi.backend.disfraces.dominio.Disfraz;
import com.costumi.backend.disfraces.dominio.Slot;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO de salida del Disfraz con sus slots (Capa 3). Trae {@code precioRentaSugerido} (la suma del precio
 * de renta de sus prendas, RF-2.10) para mostrárselo al dueño, y {@code precioRentaGeneral} (el override
 * que fija el dueño, o null si cobra por la suma). La disponibilidad se consulta aparte (derivada).
 */
public record DisfrazResponse(UUID id, UUID empresaId, String nombre, UUID categoriaId, boolean activo,
		BigDecimal precioRentaGeneral, BigDecimal precioRentaSugerido, BigDecimal precioRentaSugeridoMax,
		BigDecimal precioVentaSugerido, BigDecimal precioVentaSugeridoMax, String fotoUrl, List<SlotDto> slots) {

	/** Sin precios sugeridos calculados (usos internos). */
	static DisfrazResponse desde(Disfraz d) {
		return desde(d, null, null, null, null);
	}

	/** Con los precios "desde" (mínimo); sin el tope del rango (usos de vitrina). */
	static DisfrazResponse desde(Disfraz d, BigDecimal precioRentaSugerido, BigDecimal precioVentaSugerido) {
		return desde(d, precioRentaSugerido, null, precioVentaSugerido, null);
	}

	/** Con el rango sugerido completo (mínimo–máximo) de renta y de venta, para el armado del dueño. */
	static DisfrazResponse desde(Disfraz d, BigDecimal precioRentaSugerido, BigDecimal precioRentaSugeridoMax,
			BigDecimal precioVentaSugerido, BigDecimal precioVentaSugeridoMax) {
		List<SlotDto> slots = d.slots().stream().map(DisfrazResponse::aSlotDto).toList();
		return new DisfrazResponse(d.id(), d.empresaId(), d.nombre(), d.categoriaId(), d.activo(),
				d.precioRentaGeneral(), precioRentaSugerido, precioRentaSugeridoMax, precioVentaSugerido,
				precioVentaSugeridoMax, d.fotoUrl(), slots);
	}

	private static SlotDto aSlotDto(Slot s) {
		PoolDto pool = null;
		if (s.pool() != null) {
			List<EtiquetaPermitidaDto> etiquetas = s.pool().etiquetasPermitidas().entrySet().stream()
					.map(e -> new EtiquetaPermitidaDto(e.getKey(), List.copyOf(e.getValue())))
					.toList();
			pool = new PoolDto(s.pool().categoriaId(), etiquetas);
		}
		return new SlotDto(s.orden(), s.nombre(), s.ejePrenda(), s.prendaFijaId(), pool,
				List.copyOf(s.prendasOpcion()), s.opcional());
	}
}
