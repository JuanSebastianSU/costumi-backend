package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.notificaciones.dominio.PlantillaDeNotificacion;
import com.costumi.backend.notificaciones.dominio.PlantillaDeNotificacionRepository;
import com.costumi.backend.notificaciones.dominio.TipoDeEvento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gestiona las plantillas de mensajes automáticos. Las que no fueron personalizadas se responden con
 * su default (sin persistirlas), así el listado siempre trae los {@link TipoDeEvento} completos y la
 * empresa opera "out of the box".
 */
@Service
class PlantillaService implements GestionarPlantillas, PlantillaDeEvento {

	private final PlantillaDeNotificacionRepository plantillas;

	PlantillaService(PlantillaDeNotificacionRepository plantillas) {
		this.plantillas = plantillas;
	}

	@Override
	@Transactional(readOnly = true)
	public List<PlantillaDeNotificacion> deEmpresa(UUID empresaId) {
		Map<TipoDeEvento, PlantillaDeNotificacion> guardadas = plantillas.listarPorEmpresa(empresaId).stream()
				.collect(Collectors.toMap(PlantillaDeNotificacion::tipo, Function.identity()));
		return java.util.Arrays.stream(TipoDeEvento.values())
				.map(tipo -> guardadas.getOrDefault(tipo, PlantillaDeNotificacion.porDefecto(empresaId, tipo)))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public PlantillaDeNotificacion para(UUID empresaId, TipoDeEvento tipo) {
		return plantillas.buscar(empresaId, tipo)
				.orElseGet(() -> PlantillaDeNotificacion.porDefecto(empresaId, tipo));
	}

	@Override
	@Transactional
	public PlantillaDeNotificacion actualizar(UUID empresaId, TipoDeEvento tipo, String texto, boolean activa) {
		PlantillaDeNotificacion plantilla = plantillas.buscar(empresaId, tipo)
				.orElseGet(() -> PlantillaDeNotificacion.porDefecto(empresaId, tipo));
		plantilla.editar(texto, activa);
		return plantillas.guardar(plantilla);
	}
}
