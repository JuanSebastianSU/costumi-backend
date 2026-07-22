package com.costumi.backend.notificaciones.dominio;

import java.util.List;
import java.util.UUID;

/** Puerto de salida: persistencia de Notificaciones (scoped por tenant). */
public interface NotificacionRepository {

	Notificacion guardar(Notificacion notificacion);

	List<Notificacion> listarPorEmpresa(UUID empresaId);

	/** Página de notificaciones (más recientes primero), con búsqueda opcional en el mensaje. */
	com.costumi.backend.compartido.Pagina<Notificacion> listarPorEmpresa(UUID empresaId, String buscar, com.costumi.backend.compartido.SolicitudDePagina pagina);
}
