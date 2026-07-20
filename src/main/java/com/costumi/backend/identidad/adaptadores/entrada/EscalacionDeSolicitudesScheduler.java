package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.EscalarSolicitudesVencidas;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Escalación periódica de solicitudes de empresa vencidas (RF-15.4). Adaptador de entrada "por tiempo":
 * el reloj (no HTTP) dispara el caso de uso, que emite la alerta. Es una consulta de plataforma (sin
 * tenant), por eso corre sin token.
 */
@Component
class EscalacionDeSolicitudesScheduler {

	private final EscalarSolicitudesVencidas escalarSolicitudesVencidas;

	EscalacionDeSolicitudesScheduler(EscalarSolicitudesVencidas escalarSolicitudesVencidas) {
		this.escalarSolicitudesVencidas = escalarSolicitudesVencidas;
	}

	/** Diario, configurable (por defecto 09:00, zona horaria del servidor). */
	@Scheduled(cron = "${costumi.empresa.escalacion-cron:0 0 9 * * *}")
	void escalar() {
		escalarSolicitudesVencidas.ejecutar();
	}
}
