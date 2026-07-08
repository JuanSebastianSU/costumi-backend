package com.costumi.backend.notificaciones.dominio;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Puerto de salida: rentas ACTIVAS ya vencidas de la empresa, para recordar la devolución (RF-11.1). */
public interface AvisosDeVencidasReadRepository {

	List<RentaVencidaAviso> vencidas(UUID empresaId, LocalDate hoy);

	/**
	 * Empresas que tienen al menos una renta ACTIVA vencida a esta fecha (RF-3.5/11.1). Consulta a nivel
	 * plataforma (sin filtro por tenant) para que el job programado sepa a qué empresas recordar.
	 */
	List<UUID> empresasConVencidas(LocalDate hoy);
}
