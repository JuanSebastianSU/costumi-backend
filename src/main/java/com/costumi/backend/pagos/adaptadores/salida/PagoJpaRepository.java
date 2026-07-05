package com.costumi.backend.pagos.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PagoJpaRepository extends JpaRepository<PagoJpaEntity, UUID> {

	List<PagoJpaEntity> findByEmpresaIdAndConceptoId(UUID empresaId, UUID conceptoId);

	Optional<PagoJpaEntity> findByEmpresaIdAndClaveIdempotencia(UUID empresaId, String claveIdempotencia);
}
