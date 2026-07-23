package com.costumi.backend.clientes.adaptadores.entrada;

import com.costumi.backend.clientes.dominio.DeudaEnTienda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lo que el cliente debe por una renta, y en qué tienda (RF-7/11.5).
 *
 * <p>Lleva el <b>desglose</b> (importe, daños, retraso, depósito) y no solo el total, porque una multa
 * sin explicación es justo lo que el cliente va a venir a reclamar: {@code multa} es lo que los cargos
 * superaron al depósito, y {@code saldo} lo que todavía falta pagar.
 */
public record MiDeudaResponse(UUID empresaId, String empresaNombre, UUID rentaId, String codigoRetiro,
		String estado, LocalDate fechaRetiro, LocalDate fechaDevolucion, BigDecimal importe,
		BigDecimal cargoPorDanos, BigDecimal cargoPorRetraso, BigDecimal deposito, BigDecimal multa,
		BigDecimal pagado, BigDecimal saldo) {

	static MiDeudaResponse desde(DeudaEnTienda d) {
		var l = d.linea();
		return new MiDeudaResponse(d.empresaId(), d.empresaNombre(), l.rentaId(), l.codigoRetiro(), l.estado(),
				l.fechaRetiro(), l.fechaDevolucion(), l.importe(), l.cargoPorDanos(), l.cargoPorRetraso(),
				l.deposito(), l.multa(), l.pagado(), l.saldo());
	}
}
