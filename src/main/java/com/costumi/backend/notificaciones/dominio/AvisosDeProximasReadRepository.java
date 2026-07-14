package com.costumi.backend.notificaciones.dominio;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Puerto de salida: rentas ACTIVAS que vencen en una fecha objetivo (recordatorio anticipado, RF-11.1). */
public interface AvisosDeProximasReadRepository {

	/** Rentas ACTIVAS de la empresa cuya devolución cae exactamente en {@code fechaObjetivo}. */
	List<RentaProximaAviso> proximas(UUID empresaId, LocalDate fechaObjetivo);

	/**
	 * Empresas con al menos una renta ACTIVA que vence en {@code fechaObjetivo}. Consulta a nivel
	 * plataforma (sin filtro por tenant) para que el job programado sepa a qué empresas recordar.
	 */
	List<UUID> empresasConProximas(LocalDate fechaObjetivo);
}
