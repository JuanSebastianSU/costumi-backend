package com.costumi.backend.disfraces;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

	/**
	 * Resumen mínimo de un disfraz para pintar el carrito/pedido: nombre, foto y qué operaciones admite
	 * (el dueño decide si es de renta, de venta o de ambos). Con {@code permiteRenta}/{@code permiteVenta}
	 * el carrito sabe ANTES de valorizar si esa línea sigue siendo válida, sin provocar un error.
	 */
	record ResumenDeDisfraz(UUID disfrazId, String nombre, String fotoUrl, boolean permiteRenta,
			boolean permiteVenta) {
	}

	/**
	 * Resumen (nombre + foto) de varios disfraces de la empresa, indexado por id. Sirve para que el carrito
	 * muestre QUÉ disfraz se agregó, con imagen, sin conocer las clases internas de Disfraces. Ignora ids que
	 * no existan o no sean de la empresa (aislamiento por tenant, §5.4).
	 */
	Map<UUID, ResumenDeDisfraz> resumenDeDisfraces(UUID empresaId, Collection<UUID> disfrazIds);
}
