package com.costumi.backend.inventario.adaptadores.entrada;

import com.costumi.backend.inventario.dominio.Prenda;
import com.costumi.backend.inventario.dominio.TipoArticulo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** DTO de salida de la Prenda, con sus valores de etiqueta de clasificación (RF-2.7). */
public record PrendaResponse(UUID id, UUID empresaId, UUID categoriaId, String nombre, TipoArticulo tipoArticulo,
		BigDecimal precioRenta, BigDecimal precioVenta, BigDecimal costoAdquisicion, BigDecimal depositoSugerido,
		List<EtiquetaSeleccionadaDto> etiquetas, boolean archivada) {

	static PrendaResponse desde(Prenda p) {
		List<EtiquetaSeleccionadaDto> etiquetas = p.etiquetas().valores().entrySet().stream()
				.map(e -> new EtiquetaSeleccionadaDto(e.getKey(), e.getValue()))
				.toList();
		return new PrendaResponse(p.id(), p.empresaId(), p.categoriaId(), p.nombre(), p.tipoArticulo(),
				p.precioRenta(), p.precioVenta(), p.costoAdquisicion(), p.depositoSugerido(), etiquetas, p.archivada());
	}
}
