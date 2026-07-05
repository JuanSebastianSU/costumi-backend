package com.costumi.backend.rentas.adaptadores.entrada;

import com.costumi.backend.rentas.dominio.Renta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** DTO de salida de la Renta. */
public record RentaResponse(UUID id, UUID sucursalId, UUID clienteId, UUID prendaId, LocalDate fechaRetiro,
		LocalDate fechaDevolucion, BigDecimal precioPorDia, BigDecimal deposito, BigDecimal importe, String estado) {

	static RentaResponse desde(Renta r) {
		return new RentaResponse(r.id(), r.sucursalId(), r.clienteId(), r.prendaId(), r.fechaRetiro(),
				r.fechaDevolucion(), r.precioPorDia(), r.deposito(), r.importe(), r.estado().name());
	}
}
