package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.notificaciones.dominio.PlantillaDeNotificacion;
import com.costumi.backend.notificaciones.dominio.TipoDeEvento;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: consulta y edición de las plantillas de mensajes automáticos de una empresa. */
public interface GestionarPlantillas {

	/** Las plantillas de la empresa para TODOS los tipos (usa el default si aún no fue personalizada). */
	List<PlantillaDeNotificacion> deEmpresa(UUID empresaId);

	/** Personaliza (upsert) la plantilla de un tipo: texto y switch on/off. */
	PlantillaDeNotificacion actualizar(UUID empresaId, TipoDeEvento tipo, String texto, boolean activa);
}
