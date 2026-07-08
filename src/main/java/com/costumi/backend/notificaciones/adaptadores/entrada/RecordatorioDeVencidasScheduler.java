package com.costumi.backend.notificaciones.adaptadores.entrada;

import com.costumi.backend.notificaciones.aplicacion.RecordarVencidas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Dispara automáticamente los recordatorios de rentas vencidas (RF-3.5/11.1). Adaptador de entrada
 * "por tiempo" (análogo a un controller, pero lo activa el reloj, no HTTP). Recorre las empresas con
 * vencidas y llama al caso de uso por empresa; un fallo en una empresa no frena a las demás.
 *
 * <p>Corre en un hilo del scheduler (sin token): el caso de uso recibe el {@code empresaId} explícito
 * y las consultas van por JDBC con {@code empresa_id}, así que el aislamiento multi-tenant (§5.4) se
 * mantiene. Cada empresa se procesa en su propia transacción (llamada al puerto = proxy de Spring).
 */
@Component
class RecordatorioDeVencidasScheduler {

	private static final Logger log = LoggerFactory.getLogger(RecordatorioDeVencidasScheduler.class);

	private final RecordarVencidas recordarVencidas;

	RecordatorioDeVencidasScheduler(RecordarVencidas recordarVencidas) {
		this.recordarVencidas = recordarVencidas;
	}

	/** Diario, configurable (por defecto 08:00, zona horaria del servidor). */
	@Scheduled(cron = "${costumi.notificaciones.recordatorio-vencidas.cron:0 0 8 * * *}")
	void dispararRecordatorios() {
		int empresas = 0;
		int enviadas = 0;
		for (UUID empresaId : recordarVencidas.empresasConVencidas()) {
			try {
				enviadas += recordarVencidas.ejecutar(empresaId);
				empresas++;
			}
			catch (RuntimeException e) {
				log.warn("Recordatorio de vencidas falló para la empresa {}: {}", empresaId, e.getMessage());
			}
		}
		if (empresas > 0) {
			log.info("Recordatorio de vencidas: {} avisos enviados en {} empresas", enviadas, empresas);
		}
	}
}
