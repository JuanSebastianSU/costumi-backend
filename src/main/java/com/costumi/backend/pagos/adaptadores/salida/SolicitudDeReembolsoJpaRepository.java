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

	/**
	 * Página de solicitudes, más recientes primero. {@code buscar} (sin distinguir mayúsculas) filtra por
	 * el motivo de la solicitud o el de la decisión.
	 */
	@org.springframework.data.jpa.repository.Query("select s from SolicitudDeReembolsoJpaEntity s "
			+ "where s.empresaId = :empresaId and (cast(:buscar as string) is null "
			+ "or lower(s.motivoSolicitud) like lower(concat('%', cast(:buscar as string), '%')) "
			+ "or lower(s.motivoDecision) like lower(concat('%', cast(:buscar as string), '%'))) order by s.creadaEn desc")
	org.springframework.data.domain.Page<SolicitudDeReembolsoJpaEntity> buscarPagina(
			@org.springframework.data.repository.query.Param("empresaId") UUID empresaId,
			@org.springframework.data.repository.query.Param("buscar") String buscar,
			org.springframework.data.domain.Pageable pageable);

	boolean existsByEmpresaIdAndConceptoIdAndEstado(UUID empresaId, UUID conceptoId, EstadoSolicitudReembolso estado);
}
