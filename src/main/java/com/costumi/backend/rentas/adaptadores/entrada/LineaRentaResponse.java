package com.costumi.backend.rentas.adaptadores.entrada;

import com.costumi.backend.inventario.ConsultaDeInventario.ResumenDePrenda;
import com.costumi.backend.rentas.dominio.RentaLinea;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de salida de una línea de renta (un artículo), enriquecida con el nombre y la foto de la prenda.
 * Si la línea salió de armar un disfraz, {@code disfrazId}/{@code disfrazNombre} dicen cuál y
 * {@code disfrazGrupo} identifica esa instancia concreta: así el cliente ve QUÉ rentó y no sus piezas.
 */
public record LineaRentaResponse(UUID prendaId, String nombre, String fotoUrl, int cantidad,
		BigDecimal precioPorDia, UUID disfrazId, String disfrazNombre, UUID disfrazGrupo, Integer disfrazCantidad) {

	static LineaRentaResponse desde(RentaLinea l, ResumenDePrenda resumen) {
		String nombre = resumen == null ? null : resumen.nombre();
		String fotoUrl = resumen == null ? null : resumen.fotoUrl();
		com.costumi.backend.rentas.dominio.OrigenDisfraz o = l.origenDisfraz();
		return new LineaRentaResponse(l.prendaId(), nombre, fotoUrl, l.cantidad(), l.precioPorDia(),
				o == null ? null : o.disfrazId(),
				o == null ? null : o.nombre(),
				o == null ? null : o.grupo(),
				o == null ? null : o.cantidad());
	}
}
