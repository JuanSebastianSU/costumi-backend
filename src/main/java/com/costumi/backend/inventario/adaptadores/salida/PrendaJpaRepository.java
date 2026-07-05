package com.costumi.backend.inventario.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PrendaJpaRepository extends JpaRepository<PrendaJpaEntity, UUID> {

	List<PrendaJpaEntity> findByEmpresaId(UUID empresaId);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<PrendaJpaEntity> findFirstById(UUID id);
}
