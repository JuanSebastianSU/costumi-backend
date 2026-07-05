package com.costumi.backend.notificaciones.adaptadores.salida;

import com.costumi.backend.notificaciones.dominio.Notificacion;
import com.costumi.backend.notificaciones.dominio.NotificacionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link NotificacionRepository} con JPA. */
@Repository
class NotificacionRepositoryAdapter implements NotificacionRepository {

	private final NotificacionJpaRepository jpa;

	NotificacionRepositoryAdapter(NotificacionJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Notificacion guardar(Notificacion notificacion) {
		return aDominio(jpa.save(aEntidad(notificacion)));
	}

	@Override
	public List<Notificacion> listarPorEmpresa(UUID empresaId) {
		return jpa.findByEmpresaId(empresaId).stream().map(NotificacionRepositoryAdapter::aDominio).toList();
	}

	private static NotificacionJpaEntity aEntidad(Notificacion n) {
		return new NotificacionJpaEntity(n.id(), n.empresaId(), n.clienteId(), n.canal(), n.mensaje(), n.estado(),
				n.fecha());
	}

	private static Notificacion aDominio(NotificacionJpaEntity e) {
		return Notificacion.rehidratar(e.getId(), e.getEmpresaId(), e.getClienteId(), e.getCanal(), e.getMensaje(),
				e.getEstado(), e.getFecha());
	}
}
