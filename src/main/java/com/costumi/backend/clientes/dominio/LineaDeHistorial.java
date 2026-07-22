package com.costumi.backend.clientes.dominio;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Un artículo dentro de una operación del historial (RF-7.2/18.9): qué prenda, con su nombre y foto,
 * en qué cantidad y a qué precio. Sirve para que "Mis Pedidos" y el detalle muestren QUÉ se rentó/compró.
 *
 * <p>Si la línea salió de armar un disfraz, {@code disfrazNombre} dice cuál y {@code disfrazGrupo}
 * identifica esa instancia concreta. Sin esto el cliente ve las PIEZAS ("Capa Real") en vez del disfraz
 * que compró, que es justamente lo que se arregló al hacer que el disfraz sobreviva al cobro.
 */
public record LineaDeHistorial(UUID prendaId, String nombre, String fotoUrl, int cantidad,
		BigDecimal precioUnitario, UUID disfrazId, String disfrazNombre, UUID disfrazGrupo,
		Integer disfrazCantidad) {
}
