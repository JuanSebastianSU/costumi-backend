package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.notificaciones.dominio.Notificacion;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista las Notificaciones de una empresa (scoped por tenant). */
public interface ConsultarNotificaciones {

	List<Notificacion> deEmpresa(UUID empresaId);

	/** Página de notificaciones, con búsqueda opcional en el mensaje. */
	com.costumi.backend.compartido.Pagina<Notificacion> deEmpresa(UUID empresaId, String buscar, com.costumi.backend.compartido.SolicitudDePagina pagina);
}
