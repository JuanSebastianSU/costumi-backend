package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.Capacidad;
import com.costumi.backend.identidad.dominio.PermisoDeEmpleadoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida JPA del puerto de overrides de capacidades por empleado (Fase B, paso 5). */
@Repository
class PermisoDeEmpleadoRepositoryAdapter implements PermisoDeEmpleadoRepository {

	private final PermisoEmpleadoJpaRepository jpa;

	PermisoDeEmpleadoRepositoryAdapter(PermisoEmpleadoJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Optional<Boolean> valor(UUID usuarioId, Capacidad capacidad) {
		return jpa.findByUsuarioIdAndCapacidad(usuarioId, capacidad.name())
				.map(PermisoEmpleadoJpaEntity::isConcedido);
	}

	@Override
	public List<OverrideDeCapacidad> listar(UUID empresaId, UUID usuarioId) {
		return jpa.findByEmpresaIdAndUsuarioId(empresaId, usuarioId).stream()
				.map(e -> new OverrideDeCapacidad(Capacidad.valueOf(e.getCapacidad()), e.isConcedido()))
				.toList();
	}

	@Override
	public void establecer(UUID empresaId, UUID usuarioId, Capacidad capacidad, boolean concedido) {
		PermisoEmpleadoJpaEntity entidad = jpa.findByUsuarioIdAndCapacidad(usuarioId, capacidad.name()).orElse(null);
		if (entidad == null) {
			jpa.save(new PermisoEmpleadoJpaEntity(UUID.randomUUID(), empresaId, usuarioId, capacidad.name(), concedido));
		} else {
			entidad.setConcedido(concedido);
			jpa.save(entidad);
		}
	}
}
