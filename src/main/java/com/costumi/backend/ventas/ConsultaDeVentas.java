package com.costumi.backend.ventas;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * API pública de Ventas para consulta desde otros módulos (§5.5): p. ej. Pagos necesita el total de
 * la venta para validar el saldo pendiente de un cobro (RF-6.1), sin conocer las clases internas.
 */
public interface ConsultaDeVentas {

	/** Actividad de ventas de un empleado (RF-8.2/1.4): cuántas ventas confirmó y por qué monto. */
	record ActividadDeEmpleado(long ventas, BigDecimal totalVendido) {
	}

	/** Total a cobrar de la venta (subtotal − descuento, RF-4.3), si existe y pertenece a la empresa. */
	Optional<BigDecimal> totalDeVenta(UUID empresaId, UUID ventaId);

	/**
	 * ¿La venta tiene mercancía ya devuelta? (DEVUELTA o PARCIALMENTE_DEVUELTA, RF-4.5). La usa Pagos para no
	 * aprobar un reembolso antes de recuperar el disfraz. {@code false} si no existe o no es de la empresa.
	 */
	boolean estaDevuelta(UUID empresaId, UUID ventaId);

	/** Resumen de la actividad de ventas confirmadas del empleado en la empresa (RF-8.2). */
	ActividadDeEmpleado actividadDeEmpleado(UUID empresaId, UUID empleadoId);
}
