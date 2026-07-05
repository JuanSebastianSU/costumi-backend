package com.costumi.backend.inventario.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface GrupoDeStockJpaRepository extends JpaRepository<GrupoDeStockJpaEntity, UUID> {

	List<GrupoDeStockJpaEntity> findByPrendaId(UUID prendaId);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<GrupoDeStockJpaEntity> findFirstById(UUID id);
}
