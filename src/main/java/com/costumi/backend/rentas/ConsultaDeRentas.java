package com.costumi.backend.rentas;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * API pública del módulo Rentas para otros módulos (§5.5): p. ej. Devoluciones necesita saber qué
 * prenda ampara una renta para actualizar el inventario al cerrar el ciclo (RF-5.4), sin conocer las
 * clases internas de Rentas.
 */
public interface ConsultaDeRentas {

	/** Prenda que ampara la renta, si la renta existe y pertenece a la empresa (tenant). */
	Optional<UUID> prendaDeRenta(UUID empresaId, UUID rentaId);

	/** Cliente de la renta, si existe y pertenece a la empresa (para notificar multas, RF-11.1). */
	Optional<UUID> clienteDeRenta(UUID empresaId, UUID rentaId);

	/** Importe a cobrar de la renta (RF-3.3), si existe y pertenece a la empresa. Para el saldo (RF-6.1). */
	Optional<BigDecimal> importeDeRenta(UUID empresaId, UUID rentaId);

	/**
	 * Marca la renta como DEVUELTA al registrarse su devolución (RF-5.1, "checklist conectado"). Exige
	 * que la renta esté ACTIVA; si no, falla la transición (la devolución completa se revierte).
	 */
	void marcarDevuelta(UUID empresaId, UUID rentaId);
}
