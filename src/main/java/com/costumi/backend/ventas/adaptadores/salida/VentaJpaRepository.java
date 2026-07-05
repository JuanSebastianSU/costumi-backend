package com.costumi.backend.ventas.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface VentaJpaRepository extends JpaRepository<VentaJpaEntity, UUID> {

	List<VentaJpaEntity> findByEmpresaId(UUID empresaId);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<VentaJpaEntity> findFirstById(UUID id);
}
