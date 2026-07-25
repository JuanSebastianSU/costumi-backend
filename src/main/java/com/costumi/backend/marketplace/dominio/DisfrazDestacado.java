package com.costumi.backend.marketplace.dominio;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Un disfraz destacado del marketplace (carrusel «Para este fin de semana», C1): cruza tiendas ACTIVAS y
 * los ordena por movimiento (cuántos se vendieron/rentaron). Trae la tienda, foto y precios para la tarjeta.
 */
public record DisfrazDestacado(UUID disfrazId, UUID empresaId, String empresaNombre, String nombre, String fotoUrl,
		BigDecimal precioRenta, BigDecimal precioVenta) {
}
