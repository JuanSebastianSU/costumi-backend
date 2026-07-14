package com.costumi.backend.notificaciones.adaptadores.entrada;

import com.costumi.backend.notificaciones.aplicacion.RecordarProximas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Dispara automáticamente el recordatorio ANTICIPADO de devolución (RF-11.1): a las rentas que vencen
 * dentro de la ventana configurada. Adaptador de entrada "por tiempo" (lo activa el reloj). Recorre las
 * empresas con rentas próximas y llama al caso de uso por empresa; un fallo en una no frena a las demás.
 *
 * <p>Corre sin token: el caso de uso recibe el {@code empresaId} explícito y las consultas van por JDBC
 * con {@code empresa_id}, así que el aislamiento multi-tenant (§5.4) se mantiene.
 */
@Component
class RecordatorioDeProximasScheduler {

	private static final Logger log = LoggerFactory.getLogger(RecordatorioDeProximasScheduler.class);

	private final RecordarProximas recordarProximas;

	RecordatorioDeProximasScheduler(RecordarProximas recordarProximas) {
		this.recordarProximas = recordarProximas;
	}

	/** Diario, configurable (por defecto 09:00, zona horaria del servidor). */
	@Scheduled(cron = "${costumi.notificaciones.recordatorio-proximas.cron:0 0 9 * * *}")
	void dispararRecordatorios() {
		int empresas = 0;
		int enviadas = 0;
		for (UUID empresaId : recordarProximas.empresasConProximas()) {
			try {
				enviadas += recordarProximas.ejecutar(empresaId);
				empresas++;
			}
			catch (RuntimeException e) {
				log.warn("Recordatorio anticipado falló para la empresa {}: {}", empresaId, e.getMessage());
			}
		}
		if (empresas > 0) {
			log.info("Recordatorio anticipado: {} avisos enviados en {} empresas", enviadas, empresas);
		}
	}
}
