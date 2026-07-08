package com.costumi.backend.identidad.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PermisoEmpleadoJpaRepository extends JpaRepository<PermisoEmpleadoJpaEntity, UUID> {

	List<PermisoEmpleadoJpaEntity> findByEmpresaIdAndUsuarioId(UUID empresaId, UUID usuarioId);

	Optional<PermisoEmpleadoJpaEntity> findByUsuarioIdAndSeccionAndAccion(UUID usuarioId, String seccion,
			String accion);
}
