package com.costumi.backend.notificaciones.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de plantillas de notificación por empresa. */
public interface PlantillaDeNotificacionRepository {

	Optional<PlantillaDeNotificacion> buscar(UUID empresaId, TipoDeEvento tipo);

	List<PlantillaDeNotificacion> listarPorEmpresa(UUID empresaId);

	PlantillaDeNotificacion guardar(PlantillaDeNotificacion plantilla);
}
