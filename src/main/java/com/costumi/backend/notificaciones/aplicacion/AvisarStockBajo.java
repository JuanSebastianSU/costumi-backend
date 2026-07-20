package com.costumi.backend.notificaciones.aplicacion;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: aviso proactivo al dueño de que hay stock por debajo del umbral (RF-11.2). */
public interface AvisarStockBajo {

	/** Envía el aviso de stock bajo de la empresa (un resumen in-app) y devuelve cuántos se enviaron (0 o 1). */
	int ejecutar(UUID empresaId);

	/** Empresas con al menos un grupo de stock bajo el umbral: las que el job programado debe avisar. */
	List<UUID> empresasConStockBajo();
}
