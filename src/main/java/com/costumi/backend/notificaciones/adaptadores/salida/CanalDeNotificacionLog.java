package com.costumi.backend.notificaciones.adaptadores.salida;

import com.costumi.backend.notificaciones.dominio.CanalDeNotificacion;
import com.costumi.backend.notificaciones.dominio.Notificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador de canal por defecto: registra la notificación en el log. Los canales reales
 * (WhatsApp/FCM) son integraciones externas que se enchufan aquí sin tocar el dominio.
 */
@Component
class CanalDeNotificacionLog implements CanalDeNotificacion {

	private static final Logger log = LoggerFactory.getLogger(CanalDeNotificacionLog.class);

	@Override
	public boolean enviar(Notificacion notificacion) {
		log.info("Notificación {} por {} a cliente {}: {}", notificacion.id(), notificacion.canal(),
				notificacion.clienteId(), notificacion.mensaje());
		return true;
	}
}
