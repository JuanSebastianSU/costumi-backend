package com.costumi.backend.clientes.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Una línea del estado de cuenta de un cliente (RF-7/11.5): una de sus rentas con el desglose que
 * explica cuánto debe y por qué. {@code saldo} = max(0, importe + multa − pagado) y solo cuenta para
 * rentas ACTIVA/DEVUELTA (igual que el saldo agregado); {@code multa} = daños + retraso que superaron
 * el depósito. Modelo de lectura, consistente con {@link CargaDeCliente}.
 */
public record LineaDeEstadoDeCuenta(UUID rentaId, String codigoRetiro, String estado, LocalDate fechaRetiro,
		LocalDate fechaDevolucion, BigDecimal importe, BigDecimal cargoPorDanos, BigDecimal cargoPorRetraso,
		BigDecimal deposito, BigDecimal multa, BigDecimal pagado, BigDecimal saldo) {
}
