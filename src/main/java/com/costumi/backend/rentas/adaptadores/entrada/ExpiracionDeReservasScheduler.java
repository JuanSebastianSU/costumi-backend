package com.costumi.backend.rentas.adaptadores.entrada;

import com.costumi.backend.rentas.aplicacion.ExpirarReservas;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Expira periódicamente las reservas no pagadas (RF-3.5). Adaptador de entrada "por tiempo": el reloj
 * dispara el caso de uso, que cancela las reservas que pasaron las 24 h sin pagar. Corre sin token: la
 * consulta y la cancelación van acotadas por {@code empresa_id}, así el aislamiento multi-tenant se mantiene.
 */
@Component
class ExpiracionDeReservasScheduler {

	private final ExpirarReservas expirarReservas;

	ExpiracionDeReservasScheduler(ExpirarReservas expirarReservas) {
		this.expirarReservas = expirarReservas;
	}

	/** Cada hora (configurable): es responsivo para el límite de 24 h sin ser costoso. */
	@Scheduled(cron = "${costumi.rentas.expiracion-reserva-cron:0 0 * * * *}")
	void expirar() {
		expirarReservas.ejecutar();
	}
}
