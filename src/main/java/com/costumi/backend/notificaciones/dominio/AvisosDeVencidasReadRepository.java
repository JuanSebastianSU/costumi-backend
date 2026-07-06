package com.costumi.backend.notificaciones.dominio;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Puerto de salida: rentas ACTIVAS ya vencidas de la empresa, para recordar la devolución (RF-11.1). */
public interface AvisosDeVencidasReadRepository {

	List<RentaVencidaAviso> vencidas(UUID empresaId, LocalDate hoy);
}
