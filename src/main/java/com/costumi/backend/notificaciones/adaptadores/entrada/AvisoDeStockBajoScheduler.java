package com.costumi.backend.notificaciones.adaptadores.entrada;

import com.costumi.backend.notificaciones.aplicacion.AvisarStockBajo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Aviso proactivo de stock bajo al dueño (RF-11.2). Adaptador de entrada "por tiempo": recorre las
 * empresas con stock bajo el umbral y deja un resumen in-app por empresa; un fallo en una empresa no
 * frena a las demás. Corre sin token: el caso de uso recibe el {@code empresaId} explícito y las
 * consultas van por JDBC con {@code empresa_id}, así que el aislamiento multi-tenant (§5.4) se mantiene.
 */
@Component
class AvisoDeStockBajoScheduler {

	private static final Logger log = LoggerFactory.getLogger(AvisoDeStockBajoScheduler.class);

	private final AvisarStockBajo avisarStockBajo;

	AvisoDeStockBajoScheduler(AvisarStockBajo avisarStockBajo) {
		this.avisarStockBajo = avisarStockBajo;
	}

	/** Diario, configurable (por defecto 08:00, zona horaria del servidor). */
	@Scheduled(cron = "${costumi.notificaciones.stock-bajo.cron:0 0 8 * * *}")
	void dispararAvisos() {
		int empresas = 0;
		int enviadas = 0;
		for (UUID empresaId : avisarStockBajo.empresasConStockBajo()) {
			try {
				enviadas += avisarStockBajo.ejecutar(empresaId);
				empresas++;
			}
			catch (RuntimeException e) {
				log.warn("Aviso de stock bajo falló para la empresa {}: {}", empresaId, e.getMessage());
			}
		}
		if (empresas > 0) {
			log.info("Aviso de stock bajo: {} avisos enviados en {} empresas", enviadas, empresas);
		}
	}
}
