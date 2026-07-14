package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.clientes.ResolucionDeClientes;
import com.costumi.backend.notificaciones.dominio.CanalNotificacion;
import com.costumi.backend.notificaciones.dominio.PlantillaDeNotificacion;
import com.costumi.backend.notificaciones.dominio.TipoDeEvento;
import com.costumi.backend.pagos.SaldoSaldado;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Disparador por evento (RF-11.1, §5.5): cuando un cliente termina de saldar la deuda de una multa
 * ({@link SaldoSaldado} de Pagos), le avisa que ya no adeuda nada con la plantilla configurable
 * ({@link TipoDeEvento#DEUDA_SALDADA}). No envía si la plantilla está desactivada.
 */
@Component
class DisparadorDeDeudaSaldada {

	private final EnviarNotificacion enviarNotificacion;
	private final PlantillaDeEvento plantillas;
	private final ResolucionDeClientes clientes;

	DisparadorDeDeudaSaldada(EnviarNotificacion enviarNotificacion, PlantillaDeEvento plantillas,
			ResolucionDeClientes clientes) {
		this.enviarNotificacion = enviarNotificacion;
		this.plantillas = plantillas;
		this.clientes = clientes;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void alSaldarseLaDeuda(SaldoSaldado evento) {
		PlantillaDeNotificacion plantilla = plantillas.para(evento.empresaId(), TipoDeEvento.DEUDA_SALDADA);
		if (!plantilla.activa()) {
			return;
		}
		String cliente = clientes.nombreDeCliente(evento.empresaId(), evento.clienteId()).orElse("cliente");
		String mensaje = plantilla.render(Map.of(
				"cliente", cliente,
				"monto", dinero(evento.monto())));
		enviarNotificacion.ejecutar(new EnviarNotificacionComando(evento.empresaId(), evento.clienteId(),
				CanalNotificacion.WHATSAPP, mensaje));
	}

	private static String dinero(java.math.BigDecimal monto) {
		return monto == null ? "$0" : "$" + monto.stripTrailingZeros().toPlainString();
	}
}
