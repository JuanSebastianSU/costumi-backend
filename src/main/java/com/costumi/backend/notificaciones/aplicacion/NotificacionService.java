package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.notificaciones.dominio.AvisosDeVencidasReadRepository;
import com.costumi.backend.notificaciones.dominio.CanalDeNotificacion;
import com.costumi.backend.notificaciones.dominio.CanalNotificacion;
import com.costumi.backend.notificaciones.dominio.Notificacion;
import com.costumi.backend.notificaciones.dominio.NotificacionRepository;
import com.costumi.backend.notificaciones.dominio.RentaVencidaAviso;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Crea la notificación, la despacha por el canal y guarda el resultado. */
@Service
class NotificacionService implements EnviarNotificacion, ConsultarNotificaciones, RecordarVencidas {

	private final NotificacionRepository notificaciones;
	private final CanalDeNotificacion canal;
	private final AvisosDeVencidasReadRepository vencidas;

	NotificacionService(NotificacionRepository notificaciones, CanalDeNotificacion canal,
			AvisosDeVencidasReadRepository vencidas) {
		this.notificaciones = notificaciones;
		this.canal = canal;
		this.vencidas = vencidas;
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
		int enviadas = 0;
		for (RentaVencidaAviso aviso : vencidas.vencidas(empresaId, LocalDate.now())) {
			if (aviso.clienteId() == null) {
				continue;
			}
			ejecutar(new EnviarNotificacionComando(empresaId, aviso.clienteId(), CanalNotificacion.EMAIL,
					"Recordatorio: tu renta " + aviso.rentaId() + " venció el " + aviso.fechaDevolucion()
							+ ". Por favor devuélvela."));
			enviadas++;
		}
		return enviadas;
	}
}
