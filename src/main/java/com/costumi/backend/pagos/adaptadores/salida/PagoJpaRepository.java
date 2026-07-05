package com.costumi.backend.pagos.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PagoJpaRepository extends JpaRepository<PagoJpaEntity, UUID> {

	List<PagoJpaEntity> findByEmpresaIdAndConceptoId(UUID empresaId, UUID conceptoId);

	Optional<PagoJpaEntity> findByEmpresaIdAndClaveIdempotencia(UUID empresaId, String claveIdempotencia);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<PagoJpaEntity> findFirstById(UUID id);
}
