package com.costumi.backend.clientes.dominio;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Un artículo dentro de una operación del historial (RF-7.2/18.9): qué prenda, con su nombre y foto,
 * en qué cantidad y a qué precio. Sirve para que "Mis Pedidos" y el detalle muestren QUÉ se rentó/compró.
 */
public record LineaDeHistorial(UUID prendaId, String nombre, String fotoUrl, int cantidad,
		BigDecimal precioUnitario) {
}
