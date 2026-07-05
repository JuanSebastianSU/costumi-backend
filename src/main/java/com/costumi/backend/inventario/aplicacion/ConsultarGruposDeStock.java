package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.GrupoDeStock;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista los Grupos de stock de una Prenda (scoped por tenant). */
public interface ConsultarGruposDeStock {

	List<GrupoDeStock> dePrenda(UUID empresaId, UUID prendaId);
}
