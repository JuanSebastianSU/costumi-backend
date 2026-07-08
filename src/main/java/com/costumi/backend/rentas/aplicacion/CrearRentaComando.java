package com.costumi.backend.rentas.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Datos para crear una Renta en la empresa del usuario autenticado (RF-3.1/3.3). Lleva una o más
 * líneas (multi-artículo); las fechas de retiro/devolución y el depósito son de la renta completa.
 */
public record CrearRentaComando(UUID empresaId, UUID sucursalId, UUID clienteId, List<LineaDeRentaComando> lineas,
		LocalDate fechaRetiro, LocalDate fechaDevolucion, BigDecimal deposito, String claveIdempotencia,
		UUID empleadoId) {
}
