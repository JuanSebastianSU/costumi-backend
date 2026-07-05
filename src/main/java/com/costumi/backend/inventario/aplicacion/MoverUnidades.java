package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.GrupoDeStock;

/** Puerto de entrada: mueve unidades entre estados de un Grupo de stock (RF-2.11). */
public interface MoverUnidades {

	GrupoDeStock ejecutar(MoverUnidadesComando comando);
}
