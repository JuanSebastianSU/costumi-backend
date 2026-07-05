package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.notificaciones.dominio.Notificacion;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista las Notificaciones de una empresa (scoped por tenant). */
public interface ConsultarNotificaciones {

	List<Notificacion> deEmpresa(UUID empresaId);
}
