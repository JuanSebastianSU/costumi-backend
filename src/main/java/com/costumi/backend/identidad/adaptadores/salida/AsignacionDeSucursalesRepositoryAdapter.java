package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.AsignacionDeSucursalesRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Adaptador de salida JPA de la asignación de sucursales por empleado (RF-1.2). */
@Repository
class AsignacionDeSucursalesRepositoryAdapter implements AsignacionDeSucursalesRepository {

	private final UsuarioSucursalJpaRepository jpa;

	AsignacionDeSucursalesRepositoryAdapter(UsuarioSucursalJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	@Transactional
	public void reemplazar(UUID empresaId, UUID usuarioId, Set<UUID> sucursalIds) {
		jpa.deleteByUsuarioId(usuarioId);
		for (UUID sucursalId : sucursalIds) {
			jpa.save(new UsuarioSucursalJpaEntity(UUID.randomUUID(), empresaId, usuarioId, sucursalId));
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<UUID> sucursalesDe(UUID usuarioId) {
		return jpa.findByUsuarioId(usuarioId).stream().map(UsuarioSucursalJpaEntity::getSucursalId).toList();
	}
}
