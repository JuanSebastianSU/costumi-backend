package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.clientes.ResolucionDeClientes;
import com.costumi.backend.notificaciones.dominio.AvisosDeVencidasReadRepository;
import com.costumi.backend.notificaciones.dominio.CanalDeNotificacion;
import com.costumi.backend.notificaciones.dominio.CanalNotificacion;
import com.costumi.backend.notificaciones.dominio.Notificacion;
import com.costumi.backend.notificaciones.dominio.NotificacionRepository;
import com.costumi.backend.notificaciones.dominio.PlantillaDeNotificacion;
import com.costumi.backend.notificaciones.dominio.RentaVencidaAviso;
import com.costumi.backend.notificaciones.dominio.TipoDeEvento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Crea la notificación, la despacha por el canal y guarda el resultado. */
@Service
class NotificacionService implements EnviarNotificacion, ConsultarNotificaciones, RecordarVencidas {

	private final NotificacionRepository notificaciones;
	private final CanalDeNotificacion canal;
	private final AvisosDeVencidasReadRepository vencidas;
	private final PlantillaDeEvento plantillas;
	private final ResolucionDeClientes clientes;

	NotificacionService(NotificacionRepository notificaciones, CanalDeNotificacion canal,
			AvisosDeVencidasReadRepository vencidas, PlantillaDeEvento plantillas, ResolucionDeClientes clientes) {
		this.notificaciones = notificaciones;
		this.canal = canal;
		this.vencidas = vencidas;
		this.plantillas = plantillas;
		this.clientes = clientes;
	}

	@Override
	@Transactional
	public Notificacion ejecutar(EnviarNotificacionComando comando) {
		Notificacion notificacion = Notificacion.crear(comando.empresaId(), comando.clienteId(),
				comando.canal(), comando.mensaje());
		if (canal.enviar(notificacion)) {
			notificacion.marcarEnviada();
		} else {
			notificacion.marcarFallida();
		}
		return notificaciones.guardar(notificacion);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Notificacion> deEmpresa(UUID empresaId) {
		return notificaciones.listarPorEmpresa(empresaId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<UUID> empresasConVencidas() {
		return vencidas.empresasConVencidas(LocalDate.now());
	}

	@Override
	@Transactional
	public int ejecutar(UUID empresaId) {
		PlantillaDeNotificacion plantilla = plantillas.para(empresaId, TipoDeEvento.RENTA_VENCIDA);
		if (!plantilla.activa()) {
			return 0; // la empresa apagó el recordatorio de vencidas.
		}
		int enviadas = 0;
		for (RentaVencidaAviso aviso : vencidas.vencidas(empresaId, LocalDate.now())) {
			if (aviso.clienteId() == null) {
				continue;
			}
			String cliente = clientes.nombreDeCliente(empresaId, aviso.clienteId()).orElse("cliente");
			String mensaje = plantilla.render(Map.of(
					"cliente", cliente,
					"fecha_devolucion", String.valueOf(aviso.fechaDevolucion())));
			ejecutar(new EnviarNotificacionComando(empresaId, aviso.clienteId(), CanalNotificacion.WHATSAPP, mensaje));
			enviadas++;
		}
		return enviadas;
	}
}
