package com.costumi.backend.notificaciones.adaptadores.salida;

import com.costumi.backend.notificaciones.dominio.PlantillaDeNotificacion;
import com.costumi.backend.notificaciones.dominio.PlantillaDeNotificacionRepository;
import com.costumi.backend.notificaciones.dominio.TipoDeEvento;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa {@link PlantillaDeNotificacionRepository} con JPA. */
@Repository
class PlantillaRepositoryAdapter implements PlantillaDeNotificacionRepository {

	private final PlantillaJpaRepository jpa;

	PlantillaRepositoryAdapter(PlantillaJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Optional<PlantillaDeNotificacion> buscar(UUID empresaId, TipoDeEvento tipo) {
		return jpa.findFirstByEmpresaIdAndTipo(empresaId, tipo).map(PlantillaRepositoryAdapter::aDominio);
	}

	@Override
	public List<PlantillaDeNotificacion> listarPorEmpresa(UUID empresaId) {
		return jpa.findByEmpresaId(empresaId).stream().map(PlantillaRepositoryAdapter::aDominio).toList();
	}

	@Override
	public PlantillaDeNotificacion guardar(PlantillaDeNotificacion plantilla) {
		return aDominio(jpa.save(aEntidad(plantilla)));
	}

	private static PlantillaJpaEntity aEntidad(PlantillaDeNotificacion p) {
		return new PlantillaJpaEntity(p.id(), p.empresaId(), p.tipo(), p.texto(), p.activa());
	}

	private static PlantillaDeNotificacion aDominio(PlantillaJpaEntity e) {
		return PlantillaDeNotificacion.rehidratar(e.getId(), e.getEmpresaId(), e.getTipo(), e.getTexto(),
				e.isActiva());
	}
}
