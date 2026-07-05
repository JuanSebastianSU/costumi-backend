package com.costumi.backend.rentas.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Datos para crear una Renta en la empresa del usuario autenticado (RF-3.1/3.3). */
public record CrearRentaComando(UUID empresaId, UUID sucursalId, UUID clienteId, UUID prendaId,
		LocalDate fechaRetiro, LocalDate fechaDevolucion, BigDecimal precioPorDia, BigDecimal deposito) {
}
