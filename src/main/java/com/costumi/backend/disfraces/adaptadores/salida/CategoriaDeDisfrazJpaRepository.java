package com.costumi.backend.disfraces.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CategoriaDeDisfrazJpaRepository extends JpaRepository<CategoriaDeDisfrazJpaEntity, UUID> {

	List<CategoriaDeDisfrazJpaEntity> findByEmpresaId(UUID empresaId);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<CategoriaDeDisfrazJpaEntity> findFirstById(UUID id);
}
