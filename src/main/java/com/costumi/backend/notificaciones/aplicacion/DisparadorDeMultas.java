package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.devoluciones.DevolucionRegistrada;
import com.costumi.backend.notificaciones.dominio.CanalNotificacion;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Disparador por evento (RF-11.1, §5.5): cuando una devolución genera una <b>multa</b> automática
 * (evento {@link DevolucionRegistrada} de Devoluciones), notifica al cliente. Reacciona de forma
 * síncrona dentro de la transacción; el envío real es responsabilidad del canal (hoy, log).
 */
@Component
class DisparadorDeMultas {

	private final EnviarNotificacion enviarNotificacion;

	DisparadorDeMultas(EnviarNotificacion enviarNotificacion) {
		this.enviarNotificacion = enviarNotificacion;
	}

	@EventListener
	void alRegistrarseUnaDevolucion(DevolucionRegistrada evento) {
		if (evento.clienteId() == null || evento.multa().signum() <= 0) {
			return;
		}
		enviarNotificacion.ejecutar(new EnviarNotificacionComando(evento.empresaId(), evento.clienteId(),
				CanalNotificacion.EMAIL, "Se registró una multa de " + evento.multa() + " en tu devolución."));
	}
}
