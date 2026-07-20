package com.costumi.backend.notificaciones.dominio;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de salida (modelo de lectura sobre el esquema compartido): empresas con stock por debajo de un
 * umbral y cuántos grupos de stock caen bajo ese umbral (RF-11.2). Lo usa el aviso proactivo al dueño.
 */
public interface AvisosDeStockBajoReadRepository {

	/** Empresas con al menos un grupo de stock cuyas unidades disponibles están por debajo del umbral. */
	List<UUID> empresasConStockBajo(int umbral);

	/** Cuántos grupos de stock de la empresa están por debajo del umbral (0 si ninguno). */
	int conteoStockBajo(UUID empresaId, int umbral);
}
