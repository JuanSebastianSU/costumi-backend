package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.GrupoDeStock;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: grupos de stock cuyas unidades disponibles están por debajo de un umbral (RF-10). */
public interface ConsultarStockBajo {

	List<GrupoDeStock> deEmpresa(UUID empresaId, int umbral);
}
