package com.costumi.backend.catalogo.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CategoriaJpaRepository extends JpaRepository<CategoriaJpaEntity, UUID> {

	List<CategoriaJpaEntity> findByEmpresaId(UUID empresaId);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<CategoriaJpaEntity> findFirstById(UUID id);
}
