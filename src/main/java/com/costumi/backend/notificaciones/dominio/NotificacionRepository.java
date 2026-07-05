package com.costumi.backend.notificaciones.dominio;

import java.util.List;
import java.util.UUID;

/** Puerto de salida: persistencia de Notificaciones (scoped por tenant). */
public interface NotificacionRepository {

	Notificacion guardar(Notificacion notificacion);

	List<Notificacion> listarPorEmpresa(UUID empresaId);
}
