package com.costumi.backend.pagos.adaptadores.salida;

import com.costumi.backend.pagos.dominio.EstadoSolicitudReembolso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SolicitudDeReembolsoJpaRepository extends JpaRepository<SolicitudDeReembolsoJpaEntity, UUID> {

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<SolicitudDeReembolsoJpaEntity> findFirstById(UUID id);

	List<SolicitudDeReembolsoJpaEntity> findByEmpresaIdOrderByCreadaEnDesc(UUID empresaId);

	boolean existsByEmpresaIdAndConceptoIdAndEstado(UUID empresaId, UUID conceptoId, EstadoSolicitudReembolso estado);
}
