package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.notificaciones.dominio.AvisosDeStockBajoReadRepository;
import com.costumi.backend.notificaciones.dominio.CanalNotificacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Aviso proactivo de stock bajo al dueño (RF-11.2): recorre las empresas con grupos de stock por debajo
 * del umbral configurable y le deja un resumen in-app ("Hay N grupos de stock con pocas unidades").
 * El umbral ({@code costumi.notificaciones.stock-bajo.umbral}) &le; 0 apaga el aviso.
 */
@Service
class AvisoDeStockBajoService implements AvisarStockBajo {

	private final AvisosDeStockBajoReadRepository stockBajo;
	private final EnviarNotificacion enviarNotificacion;
	private final int umbral;

	AvisoDeStockBajoService(AvisosDeStockBajoReadRepository stockBajo, EnviarNotificacion enviarNotificacion,
			@Value("${costumi.notificaciones.stock-bajo.umbral:1}") int umbral) {
		this.stockBajo = stockBajo;
		this.enviarNotificacion = enviarNotificacion;
		this.umbral = umbral;
	}

	@Override
	@Transactional(readOnly = true)
	public List<UUID> empresasConStockBajo() {
		if (umbral <= 0) {
			return List.of(); // aviso de stock bajo apagado.
		}
		return stockBajo.empresasConStockBajo(umbral);
	}

	@Override
	@Transactional
	public int ejecutar(UUID empresaId) {
		if (umbral <= 0) {
			return 0;
		}
		int grupos = stockBajo.conteoStockBajo(empresaId, umbral);
		if (grupos <= 0) {
			return 0;
		}
		String resumen = grupos == 1
				? "Hay 1 grupo de stock con pocas unidades disponibles. Revisá tu inventario."
				: "Hay " + grupos + " grupos de stock con pocas unidades disponibles. Revisá tu inventario.";
		enviarNotificacion.ejecutar(new EnviarNotificacionComando(empresaId, null, CanalNotificacion.IN_APP, resumen));
		return 1;
	}
}
