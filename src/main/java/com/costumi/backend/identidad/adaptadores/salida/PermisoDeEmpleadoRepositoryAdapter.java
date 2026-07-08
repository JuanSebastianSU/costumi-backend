package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.AccionDePermiso;
import com.costumi.backend.identidad.dominio.Permiso;
import com.costumi.backend.identidad.dominio.PermisoDeEmpleadoRepository;
import com.costumi.backend.identidad.dominio.Seccion;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida JPA del puerto de overrides de permisos por empleado (RF-1.5). */
@Repository
class PermisoDeEmpleadoRepositoryAdapter implements PermisoDeEmpleadoRepository {

	private final PermisoEmpleadoJpaRepository jpa;

	PermisoDeEmpleadoRepositoryAdapter(PermisoEmpleadoJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Optional<Boolean> valor(UUID usuarioId, Seccion seccion, AccionDePermiso accion) {
		return jpa.findByUsuarioIdAndSeccionAndAccion(usuarioId, seccion.name(), accion.name())
				.map(PermisoEmpleadoJpaEntity::isConcedido);
	}

	@Override
	public List<OverrideDePermiso> listar(UUID empresaId, UUID usuarioId) {
		return jpa.findByEmpresaIdAndUsuarioId(empresaId, usuarioId).stream()
				.map(e -> new OverrideDePermiso(
						new Permiso(Seccion.valueOf(e.getSeccion()), AccionDePermiso.valueOf(e.getAccion())),
						e.isConcedido()))
				.toList();
	}

	@Override
	public void establecer(UUID empresaId, UUID usuarioId, Seccion seccion, AccionDePermiso accion,
			boolean concedido) {
		PermisoEmpleadoJpaEntity entidad = jpa
				.findByUsuarioIdAndSeccionAndAccion(usuarioId, seccion.name(), accion.name())
				.orElse(null);
		if (entidad == null) {
			jpa.save(new PermisoEmpleadoJpaEntity(UUID.randomUUID(), empresaId, usuarioId, seccion.name(),
					accion.name(), concedido));
		} else {
			entidad.setConcedido(concedido);
			jpa.save(entidad);
		}
	}
}
