package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.notificaciones.dominio.PlantillaDeNotificacion;
import com.costumi.backend.notificaciones.dominio.TipoDeEvento;

import java.util.UUID;

/**
 * Puerto de entrada para los disparadores automáticos: la plantilla efectiva de la empresa para un
 * tipo (la personalizada o, si no existe, el default). El disparador decide enviar según
 * {@link PlantillaDeNotificacion#activa()}.
 */
public interface PlantillaDeEvento {

	PlantillaDeNotificacion para(UUID empresaId, TipoDeEvento tipo);
}
