package com.costumi.backend.pagos.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface IntentoDePagoJpaRepository extends JpaRepository<IntentoDePagoJpaEntity, UUID> {

	/** Carga por PK vía query derivada (respeta el filtro multi-tenant §5.4), no el findById de em.find. */
	Optional<IntentoDePagoJpaEntity> findFirstById(UUID id);
}
