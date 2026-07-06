package com.costumi.backend.ventas;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * API pública de Ventas para consulta desde otros módulos (§5.5): p. ej. Pagos necesita el total de
 * la venta para validar el saldo pendiente de un cobro (RF-6.1), sin conocer las clases internas.
 */
public interface ConsultaDeVentas {

	/** Total a cobrar de la venta (subtotal − descuento, RF-4.3), si existe y pertenece a la empresa. */
	Optional<BigDecimal> totalDeVenta(UUID empresaId, UUID ventaId);
}
