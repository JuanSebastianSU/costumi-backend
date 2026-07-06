package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.configuracion.ConsultaDeConfiguracion;
import com.costumi.backend.devoluciones.DevolucionRegistrada;
import com.costumi.backend.notificaciones.dominio.CanalNotificacion;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Disparador por evento (RF-11.1, §5.5): cuando una devolución genera una <b>multa</b> automática
 * (evento {@link DevolucionRegistrada} de Devoluciones), notifica al cliente — <b>salvo</b> que el
 * módulo de multas esté apagado en la configuración de la empresa (RF-12.4/6.6). Reacciona de forma
 * síncrona dentro de la transacción; el envío real es responsabilidad del canal (hoy, log).
 */
@Component
class DisparadorDeMultas {

	private final EnviarNotificacion enviarNotificacion;
	private final ConsultaDeConfiguracion configuracion;

	DisparadorDeMultas(EnviarNotificacion enviarNotificacion, ConsultaDeConfiguracion configuracion) {
		this.enviarNotificacion = enviarNotificacion;
		this.configuracion = configuracion;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void alRegistrarseUnaDevolucion(DevolucionRegistrada evento) {
		if (evento.clienteId() == null || evento.multa().signum() <= 0) {
			return;
		}
		if (!configuracion.multasActivas(evento.empresaId())) {
			return; // el switch de multas está apagado (RF-6.6): no se notifica.
		}
		enviarNotificacion.ejecutar(new EnviarNotificacionComando(evento.empresaId(), evento.clienteId(),
				CanalNotificacion.EMAIL, "Se registró una multa de " + evento.multa() + " en tu devolución."));
	}
}
