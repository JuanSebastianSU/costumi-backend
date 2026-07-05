package com.costumi.backend.rentas.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface RentaJpaRepository extends JpaRepository<RentaJpaEntity, UUID> {

	List<RentaJpaEntity> findByEmpresaId(UUID empresaId);

	List<RentaJpaEntity> findByEmpresaIdAndClienteId(UUID empresaId, UUID clienteId);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<RentaJpaEntity> findFirstById(UUID id);
}
