package com.costumi.backend.devoluciones.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface DevolucionJpaRepository extends JpaRepository<DevolucionJpaEntity, UUID> {

	List<DevolucionJpaEntity> findByEmpresaId(UUID empresaId);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<DevolucionJpaEntity> findFirstById(UUID id);
}
