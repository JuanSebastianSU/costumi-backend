package com.costumi.backend.rentas;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API pública del módulo Rentas para otros módulos (§5.5): p. ej. Devoluciones necesita saber qué
 * prenda ampara una renta para actualizar el inventario al cerrar el ciclo (RF-5.4), sin conocer las
 * clases internas de Rentas.
 */
public interface ConsultaDeRentas {

	/** Una línea de la renta vista desde fuera: la prenda y su cantidad. */
	record LineaDeRentaVista(UUID prendaId, int cantidad) {
	}

	/** Prenda que ampara la renta (artículo principal), si la renta existe y pertenece a la empresa (tenant). */
	Optional<UUID> prendaDeRenta(UUID empresaId, UUID rentaId);

	/** Líneas (artículos) de la renta con su cantidad; vacío si no existe o no es de la empresa (RF-5.5). */
	List<LineaDeRentaVista> lineasDeRenta(UUID empresaId, UUID rentaId);

	/** Sucursal de la renta (para actualizar el stock por sucursal al devolver, RF-18.2). */
	Optional<UUID> sucursalDeRenta(UUID empresaId, UUID rentaId);

	/**
	 * Cuántas rentas <b>vigentes</b> (no cerradas ni canceladas: RESERVADA/ACTIVA/DEVUELTA) tiene la sucursal.
	 * Sirve para impedir archivar una sucursal con obligaciones abiertas (RF-15.1) y reportar cuántas hay.
	 */
	int contarRentasVigentesEnSucursal(UUID empresaId, UUID sucursalId);

	/** Cliente de la renta, si existe y pertenece a la empresa (para notificar multas, RF-11.1). */
	Optional<UUID> clienteDeRenta(UUID empresaId, UUID rentaId);

	/** Importe a cobrar de la renta (RF-3.3), si existe y pertenece a la empresa. Para el saldo (RF-6.1). */
	Optional<BigDecimal> importeDeRenta(UUID empresaId, UUID rentaId);

	/** Fecha de devolución pactada de la renta, si existe y es de la empresa (para el recargo por retraso, RF-5.2). */
	Optional<java.time.LocalDate> fechaDevolucionDeRenta(UUID empresaId, UUID rentaId);

	/**
	 * Marca la renta como DEVUELTA al registrarse su devolución (RF-5.1, "checklist conectado"). Exige
	 * que la renta esté ACTIVA; si no, falla la transición (la devolución completa se revierte).
	 */
	void marcarDevuelta(UUID empresaId, UUID rentaId);
}
