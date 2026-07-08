package com.costumi.backend.pedidos.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada: checkout del carrito de RENTA (RF-16.4/18.6-7). Agrupa las líneas por su periodo
 * (retiro/devolución) y crea una renta multi-artículo por cada periodo distinto; confirma el carrito.
 */
public interface HacerCheckoutDeRenta {

	/** Devuelve los ids de las rentas creadas (una por periodo distinto del carrito). */
	List<UUID> ejecutar(UUID empresaId, UUID sucursalId, UUID clienteId);
}
