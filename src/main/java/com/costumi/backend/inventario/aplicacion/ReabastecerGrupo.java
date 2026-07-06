package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.GrupoDeStock;

import java.util.UUID;

/** Puerto de entrada: entrada de mercancía a un grupo de stock (RF-10). */
public interface ReabastecerGrupo {

	GrupoDeStock ejecutar(UUID empresaId, UUID grupoId, int cantidad);
}
