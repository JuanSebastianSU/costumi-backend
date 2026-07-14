package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.clientes.ResolucionDeClientes;
import com.costumi.backend.configuracion.ConsultaDeConfiguracion;
import com.costumi.backend.devoluciones.DevolucionRegistrada;
import com.costumi.backend.notificaciones.dominio.CanalNotificacion;
import com.costumi.backend.notificaciones.dominio.PlantillaDeNotificacion;
import com.costumi.backend.notificaciones.dominio.TipoDeEvento;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Disparador por evento (RF-11.1, §5.5): cuando una devolución genera una <b>multa</b> automática
 * (evento {@link DevolucionRegistrada} de Devoluciones), notifica al cliente con la <b>plantilla</b>
 * configurable de la empresa (tipo {@link TipoDeEvento#MULTA_GENERADA}) — salvo que el módulo de
 * multas esté apagado (RF-6.6) o la plantilla esté desactivada. El envío real lo hace el canal.
 */
@Component
class DisparadorDeMultas {

	private final EnviarNotificacion enviarNotificacion;
	private final ConsultaDeConfiguracion configuracion;
	private final PlantillaDeEvento plantillas;
	private final ResolucionDeClientes clientes;

	DisparadorDeMultas(EnviarNotificacion enviarNotificacion, ConsultaDeConfiguracion configuracion,
			PlantillaDeEvento plantillas, ResolucionDeClientes clientes) {
		this.enviarNotificacion = enviarNotificacion;
		this.configuracion = configuracion;
		this.plantillas = plantillas;
		this.clientes = clientes;
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
		PlantillaDeNotificacion plantilla = plantillas.para(evento.empresaId(), TipoDeEvento.MULTA_GENERADA);
		if (!plantilla.activa()) {
			return; // la empresa apagó esta automatización.
		}
		String cliente = clientes.nombreDeCliente(evento.empresaId(), evento.clienteId()).orElse("cliente");
		String mensaje = plantilla.render(Map.of(
				"cliente", cliente,
				"monto", dinero(evento.multa())));
		enviarNotificacion.ejecutar(new EnviarNotificacionComando(evento.empresaId(), evento.clienteId(),
				CanalNotificacion.WHATSAPP, mensaje));
	}

	private static String dinero(java.math.BigDecimal monto) {
		return "$" + monto.stripTrailingZeros().toPlainString();
	}
}
