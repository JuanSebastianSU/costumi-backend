package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.notificaciones.dominio.CanalDeNotificacion;
import com.costumi.backend.notificaciones.dominio.Notificacion;
import com.costumi.backend.notificaciones.dominio.NotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Crea la notificación, la despacha por el canal y guarda el resultado. */
@Service
class NotificacionService implements EnviarNotificacion, ConsultarNotificaciones {

	private final NotificacionRepository notificaciones;
	private final CanalDeNotificacion canal;

	NotificacionService(NotificacionRepository notificaciones, CanalDeNotificacion canal) {
		this.notificaciones = notificaciones;
		this.canal = canal;
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
}
