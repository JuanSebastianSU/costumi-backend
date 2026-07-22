package com.costumi.backend.disfraces.adaptadores.entrada;

import com.costumi.backend.disfraces.aplicacion.ConsultarDisfraces;
import com.costumi.backend.disfraces.dominio.Disfraz;
import com.costumi.backend.disfraces.dominio.Slot;
import com.costumi.backend.disfraces.dominio.TipoDeDisfraz;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO de salida del Disfraz con sus slots (Capa 3). Trae {@code precioRentaSugerido} (la suma del precio
 * de renta de sus prendas, RF-2.10) para mostrárselo al dueño, y {@code precioRentaGeneral} (el override
 * que fija el dueño, o null si cobra por la suma). La disponibilidad se consulta aparte (derivada).
 *
 * <p>{@code categoria} es el NOMBRE de la categoría (además del {@code categoriaId}): quien consume la
 * vitrina —el cliente del marketplace— no puede leer la taxonomía de la empresa, así que sin el nombre no
 * podría ni mostrar ni filtrar por categoría. Es lo mismo que ya hace {@code PrendaVitrinaResponse}.
 */
public record DisfrazResponse(UUID id, UUID empresaId, String nombre, UUID categoriaId, String categoria,
		TipoDeDisfraz tipo, boolean activo,
		BigDecimal precioRentaGeneral, BigDecimal precioVentaGeneral, BigDecimal precioRentaSugerido,
		BigDecimal precioRentaSugeridoMax, BigDecimal precioVentaSugerido, BigDecimal precioVentaSugeridoMax,
		MultaSugeridaDto multaSugerida, String fotoUrl, List<SlotDto> slots) {

	/** Rango de multa sugerido por tipo (daño y reposición/pérdida), según los elementos del disfraz. */
	record MultaSugeridaDto(BigDecimal danoMin, BigDecimal danoMax, BigDecimal reposicionMin,
			BigDecimal reposicionMax) {
	}

	/** Sin precios ni multa sugeridos calculados (usos internos). */
	static DisfrazResponse desde(Disfraz d) {
		return desde(d, null, null, null, null, null, null);
	}

	/** Con el rango sugerido completo (mín–máx de renta/venta + multa), calculado en un solo paso. */
	static DisfrazResponse desde(Disfraz d, ConsultarDisfraces.Sugeridos s) {
		return desde(d, s, null);
	}

	/** Igual, más el NOMBRE de la categoría (el que la lista resolvió de una sola vez para todos). */
	static DisfrazResponse desde(Disfraz d, ConsultarDisfraces.Sugeridos s, String categoria) {
		if (s == null) {
			return desde(d, null, null, null, null, null, categoria);
		}
		return desde(d, s.rentaMin(), s.rentaMax(), s.ventaMin(), s.ventaMax(), s.multa(), categoria);
	}

	/** Con el rango sugerido completo (mín–máx) de renta y venta + multa por tipo, para el armado del dueño. */
	static DisfrazResponse desde(Disfraz d, BigDecimal precioRentaSugerido, BigDecimal precioRentaSugeridoMax,
			BigDecimal precioVentaSugerido, BigDecimal precioVentaSugeridoMax,
			ConsultarDisfraces.MultaSugerida multa, String categoria) {
		List<SlotDto> slots = d.slots().stream().map(DisfrazResponse::aSlotDto).toList();
		MultaSugeridaDto multaDto = multa == null ? null
				: new MultaSugeridaDto(multa.danoMin(), multa.danoMax(), multa.reposicionMin(), multa.reposicionMax());
		return new DisfrazResponse(d.id(), d.empresaId(), d.nombre(), d.categoriaId(), categoria, d.tipo(), d.activo(),
				d.precioRentaGeneral(), d.precioVentaGeneral(), precioRentaSugerido, precioRentaSugeridoMax,
				precioVentaSugerido, precioVentaSugeridoMax, multaDto, d.fotoUrl(), slots);
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
