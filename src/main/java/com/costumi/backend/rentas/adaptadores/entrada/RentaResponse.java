package com.costumi.backend.rentas.adaptadores.entrada;

import com.costumi.backend.rentas.dominio.Renta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO de salida de la Renta. Expone el detalle multi-artículo en {@code lineas} y, por compatibilidad,
 * el artículo principal en {@code prendaId}/{@code precioPorDia} (la primera línea).
 */
public record RentaResponse(UUID id, UUID sucursalId, UUID clienteId, List<LineaRentaResponse> lineas, UUID prendaId,
		LocalDate fechaRetiro, LocalDate fechaDevolucion, BigDecimal precioPorDia, BigDecimal deposito,
		BigDecimal importe, String estado) {

	static RentaResponse desde(Renta r) {
		List<LineaRentaResponse> lineas = r.lineas().stream().map(LineaRentaResponse::desde).toList();
		return new RentaResponse(r.id(), r.sucursalId(), r.clienteId(), lineas, r.prendaId(), r.fechaRetiro(),
				r.fechaDevolucion(), r.precioPorDia(), r.deposito(), r.importe(), r.estado().name());
	}
}
