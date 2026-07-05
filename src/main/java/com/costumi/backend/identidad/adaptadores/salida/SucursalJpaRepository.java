package com.costumi.backend.identidad.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SucursalJpaRepository extends JpaRepository<SucursalJpaEntity, UUID> {

	List<SucursalJpaEntity> findByEmpresaId(UUID empresaId);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<SucursalJpaEntity> findFirstById(UUID id);
}
