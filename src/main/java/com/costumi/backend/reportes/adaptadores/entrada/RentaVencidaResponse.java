package com.costumi.backend.reportes.adaptadores.entrada;

import com.costumi.backend.reportes.dominio.RentaVencida;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** DTO de salida de una renta vencida (RF-9.1), con los días de atraso ya calculados. */
public record RentaVencidaResponse(UUID rentaId, UUID clienteId, UUID prendaId, LocalDate fechaDevolucion,
		long diasVencida, BigDecimal importe, BigDecimal deposito) {

	static RentaVencidaResponse desde(RentaVencida r, LocalDate hoy) {
		long dias = ChronoUnit.DAYS.between(r.fechaDevolucion(), hoy);
		return new RentaVencidaResponse(r.rentaId(), r.clienteId(), r.prendaId(), r.fechaDevolucion(), dias,
				r.importe(), r.deposito());
	}
}
