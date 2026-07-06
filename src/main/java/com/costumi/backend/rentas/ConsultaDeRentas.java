package com.costumi.backend.rentas;

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
}
