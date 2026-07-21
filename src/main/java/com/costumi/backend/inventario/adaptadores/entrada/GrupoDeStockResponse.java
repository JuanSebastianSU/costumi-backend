package com.costumi.backend.inventario.adaptadores.entrada;

import com.costumi.backend.inventario.dominio.GrupoDeStock;

import java.util.List;
import java.util.UUID;

/** DTO de salida del Grupo de stock: su sucursal, combinación de variante, el desglose de estado y el total. */
public record GrupoDeStockResponse(UUID id, UUID empresaId, UUID sucursalId, UUID prendaId,
		List<SeleccionVarianteDto> combinacion,
		int disponibles, int rentadas, int danadas, int enLimpieza, int perdidas, int total) {

	static GrupoDeStockResponse desde(GrupoDeStock g) {
		List<SeleccionVarianteDto> combinacion = g.combinacion().valores().entrySet().stream()
				.map(e -> new SeleccionVarianteDto(e.getKey(), e.getValue()))
				.toList();
		return new GrupoDeStockResponse(g.id(), g.empresaId(), g.sucursalId(), g.prendaId(), combinacion,
				g.disponibles(), g.rentadas(), g.danadas(), g.enLimpieza(), g.perdidas(), g.total());
	}
}
