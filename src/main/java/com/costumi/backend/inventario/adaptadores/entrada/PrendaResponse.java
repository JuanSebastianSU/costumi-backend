package com.costumi.backend.inventario.adaptadores.entrada;

import com.costumi.backend.inventario.dominio.Prenda;
import com.costumi.backend.inventario.dominio.TipoArticulo;

import java.math.BigDecimal;
import java.util.UUID;

/** DTO de salida de la Prenda. */
public record PrendaResponse(UUID id, UUID empresaId, UUID categoriaId, String nombre, TipoArticulo tipoArticulo,
		BigDecimal precioRenta, BigDecimal precioVenta, boolean archivada) {

	static PrendaResponse desde(Prenda p) {
		return new PrendaResponse(p.id(), p.empresaId(), p.categoriaId(), p.nombre(), p.tipoArticulo(),
				p.precioRenta(), p.precioVenta(), p.archivada());
	}
}
