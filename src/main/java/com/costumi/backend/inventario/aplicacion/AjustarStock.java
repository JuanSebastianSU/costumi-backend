package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.EstadoUnidad;
import com.costumi.backend.inventario.dominio.GrupoDeStock;

import java.util.UUID;

/** Puerto de entrada: ajuste de inventario con motivo (RF-10), auditado por evento. */
public interface AjustarStock {

	GrupoDeStock ejecutar(AjustarStockComando comando);

	/** Datos del ajuste: qué grupo, qué estado, cuánto (±) y por qué. */
	record AjustarStockComando(UUID empresaId, UUID grupoId, EstadoUnidad estado, int delta, String motivo) {
	}
}
