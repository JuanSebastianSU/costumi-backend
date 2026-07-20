package com.costumi.backend.rentas.adaptadores.entrada;

import com.costumi.backend.inventario.ConsultaDeInventario.ResumenDePrenda;
import com.costumi.backend.rentas.dominio.Renta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO de salida de la Renta. Expone el detalle multi-artículo en {@code lineas} (cada una con nombre y
 * foto de la prenda) y, por compatibilidad, el artículo principal en {@code prendaId}/{@code precioPorDia}.
 */
public record RentaResponse(UUID id, UUID sucursalId, UUID clienteId, UUID empleadoId, List<LineaRentaResponse> lineas,
		UUID prendaId, LocalDate fechaRetiro, LocalDate fechaDevolucion, BigDecimal precioPorDia, BigDecimal deposito,
		BigDecimal importe, String estado) {

	/** Sin resumen de prendas: las líneas van sin nombre/foto (para PDF u otros usos internos). */
	static RentaResponse desde(Renta r) {
		return desde(r, Map.of());
	}

	/** Enriquecida: cada línea toma nombre y foto de {@code resumenes} (por prendaId), para el desglose visual. */
	static RentaResponse desde(Renta r, Map<UUID, ResumenDePrenda> resumenes) {
		List<LineaRentaResponse> lineas = r.lineas().stream()
				.map(l -> LineaRentaResponse.desde(l, resumenes.get(l.prendaId())))
				.toList();
		return new RentaResponse(r.id(), r.sucursalId(), r.clienteId(), r.empleadoId(), lineas, r.prendaId(),
				r.fechaRetiro(), r.fechaDevolucion(), r.precioPorDia(), r.deposito(), r.importe(), r.estado().name());
	}
}
