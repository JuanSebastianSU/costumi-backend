package com.costumi.backend.disfraces;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * API pública del módulo Disfraces para <b>resolver</b> un disfraz (con la elección de prenda por slot) a
 * sus líneas ya valuadas, <b>sin registrar</b> renta ni venta. La usa el carrito del cliente (módulo
 * Pedidos) para valorizar el disfraz y para el checkout. Valida que el disfraz sea del tenant y esté
 * activo, y cada elección contra su slot. Vive en el paquete base del módulo (lo único que expone hacia
 * afuera), como {@code ConsultaDeInventario} o {@code RegistroDeRentas}.
 */
public interface ResolucionDeDisfraces {

	/** Elección de prenda para un slot personalizable, por su número de orden. */
	record SeleccionDeSlot(int orden, UUID prendaId) {
	}

	/**
	 * Una línea resuelta del disfraz: la prenda concreta, la cantidad y su precio (por día si es para renta,
	 * unitario si es para venta). Con precio general del disfraz, el precio ya viene repartido entre las líneas.
	 */
	record LineaResuelta(UUID prendaId, int cantidad, BigDecimal precio) {
	}

	/** Resuelve el disfraz a sus líneas de RENTA (precio por día), aplicando su precio general si lo tiene. */
	List<LineaResuelta> lineasDeRenta(UUID empresaId, UUID disfrazId, int cantidad, List<SeleccionDeSlot> selecciones);

	/** Resuelve el disfraz a sus líneas de VENTA (precio unitario), aplicando su precio general si lo tiene. */
	List<LineaResuelta> lineasDeVenta(UUID empresaId, UUID disfrazId, int cantidad, List<SeleccionDeSlot> selecciones);
}
