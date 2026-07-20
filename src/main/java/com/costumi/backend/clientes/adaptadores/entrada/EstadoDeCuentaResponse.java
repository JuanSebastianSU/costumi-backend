package com.costumi.backend.clientes.adaptadores.entrada;

import com.costumi.backend.clientes.dominio.LineaDeEstadoDeCuenta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Estado de cuenta del cliente (RF-7/11.5): los totales ({@code saldoTotal}/{@code multaTotal}) y una línea
 * por renta con su desglose, para que la app pueda explicar cuánto debe y por qué (no solo la cifra).
 */
public record EstadoDeCuentaResponse(UUID clienteId, BigDecimal saldoTotal, BigDecimal multaTotal,
		List<LineaResponse> lineas) {

	public record LineaResponse(UUID rentaId, String codigoRetiro, String estado, LocalDate fechaRetiro,
			LocalDate fechaDevolucion, BigDecimal importe, BigDecimal cargoPorDanos, BigDecimal cargoPorRetraso,
			BigDecimal deposito, BigDecimal multa, BigDecimal pagado, BigDecimal saldo) {
	}

	static EstadoDeCuentaResponse desde(UUID clienteId, List<LineaDeEstadoDeCuenta> lineas) {
		BigDecimal saldoTotal = lineas.stream().map(LineaDeEstadoDeCuenta::saldo)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal multaTotal = lineas.stream().map(LineaDeEstadoDeCuenta::multa)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		List<LineaResponse> ls = lineas.stream()
				.map(l -> new LineaResponse(l.rentaId(), l.codigoRetiro(), l.estado(), l.fechaRetiro(),
						l.fechaDevolucion(), l.importe(), l.cargoPorDanos(), l.cargoPorRetraso(), l.deposito(),
						l.multa(), l.pagado(), l.saldo()))
				.toList();
		return new EstadoDeCuentaResponse(clienteId, saldoTotal, multaTotal, ls);
	}
}
