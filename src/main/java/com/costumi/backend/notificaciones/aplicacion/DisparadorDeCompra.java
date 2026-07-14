package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.clientes.ResolucionDeClientes;
import com.costumi.backend.identidad.ConsultaDeSucursales;
import com.costumi.backend.notificaciones.dominio.CanalNotificacion;
import com.costumi.backend.notificaciones.dominio.PlantillaDeNotificacion;
import com.costumi.backend.notificaciones.dominio.TipoDeEvento;
import com.costumi.backend.ventas.VentaConfirmada;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Disparador por evento (RF-11.1, §5.5): al confirmarse una venta a un cliente ({@link VentaConfirmada}),
 * envía el agradecimiento por compra con la plantilla configurable ({@link TipoDeEvento#COMPRA_REALIZADA}),
 * resolviendo cliente y la ubicación de la sucursal. No envía si la plantilla está desactivada.
 */
@Component
class DisparadorDeCompra {

	private final EnviarNotificacion enviarNotificacion;
	private final PlantillaDeEvento plantillas;
	private final ResolucionDeClientes clientes;
	private final ConsultaDeSucursales sucursales;

	DisparadorDeCompra(EnviarNotificacion enviarNotificacion, PlantillaDeEvento plantillas,
			ResolucionDeClientes clientes, ConsultaDeSucursales sucursales) {
		this.enviarNotificacion = enviarNotificacion;
		this.plantillas = plantillas;
		this.clientes = clientes;
		this.sucursales = sucursales;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void alConfirmarseUnaVenta(VentaConfirmada evento) {
		PlantillaDeNotificacion plantilla = plantillas.para(evento.empresaId(), TipoDeEvento.COMPRA_REALIZADA);
		if (!plantilla.activa()) {
			return;
		}
		Map<String, String> variables = new HashMap<>();
		variables.put("cliente", clientes.nombreDeCliente(evento.empresaId(), evento.clienteId()).orElse("cliente"));
		VariablesDeSucursal.agregar(variables, sucursales, evento.empresaId(), evento.sucursalId());
		enviarNotificacion.ejecutar(new EnviarNotificacionComando(evento.empresaId(), evento.clienteId(),
				CanalNotificacion.WHATSAPP, plantilla.render(variables)));
	}
}
