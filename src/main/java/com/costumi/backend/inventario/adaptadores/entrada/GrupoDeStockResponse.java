package com.costumi.backend.inventario.adaptadores.entrada;

import com.costumi.backend.inventario.dominio.GrupoDeStock;

import java.util.UUID;

/** DTO de salida del Grupo de stock, con el desglose de estado y el total. */
public record GrupoDeStockResponse(UUID id, UUID empresaId, UUID prendaId, String etiqueta,
		int disponibles, int danadas, int enLimpieza, int perdidas, int total) {

	static GrupoDeStockResponse desde(GrupoDeStock g) {
		return new GrupoDeStockResponse(g.id(), g.empresaId(), g.prendaId(), g.etiqueta(),
				g.disponibles(), g.danadas(), g.enLimpieza(), g.perdidas(), g.total());
	}
}
