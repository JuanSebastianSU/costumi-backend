package com.costumi.backend.rentas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * API pública de Rentas para otros módulos (§5.5): permite registrar una renta multi-artículo desde
 * ítems ya resueltos (p. ej. el <b>checkout de renta</b> de un carrito, RF-16/18.6-7) sin conocer las
 * clases internas de Rentas. Todos los ítems comparten el mismo periodo (retiro/devolución).
 */
public interface RegistroDeRentas {

	/** Una línea de la renta ya resuelta: prenda, cantidad y precio por día. */
	record ItemDeRenta(UUID prendaId, int cantidad, BigDecimal precioPorDia, UUID disfrazId, UUID disfrazGrupo,
			Integer disfrazCantidad, String disfrazNombre) {

		/** Prenda suelta (sin disfraz de origen). */
		public ItemDeRenta(UUID prendaId, int cantidad, BigDecimal precioPorDia) {
			this(prendaId, cantidad, precioPorDia, null, null, null, null);
		}
	}

	/**
	 * Registra una renta con esos artículos y periodo (valida disponibilidad, RF-3.2) y devuelve su id.
	 * El depósito puede ser nulo (0).
	 */
	UUID registrar(UUID empresaId, UUID sucursalId, UUID clienteId, LocalDate fechaRetiro,
			LocalDate fechaDevolucion, BigDecimal deposito, List<ItemDeRenta> items, UUID empleadoId);
}
