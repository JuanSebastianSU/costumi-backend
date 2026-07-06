package com.costumi.backend.caja.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TurnoJpaRepository extends JpaRepository<TurnoJpaEntity, UUID> {

	List<TurnoJpaEntity> findByEmpresaId(UUID empresaId);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<TurnoJpaEntity> findFirstById(UUID id);
}
