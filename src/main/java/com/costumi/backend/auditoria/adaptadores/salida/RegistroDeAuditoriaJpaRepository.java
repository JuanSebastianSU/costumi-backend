package com.costumi.backend.auditoria.adaptadores.salida;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface RegistroDeAuditoriaJpaRepository extends JpaRepository<RegistroDeAuditoriaJpaEntity, UUID> {

	List<RegistroDeAuditoriaJpaEntity> findByEmpresaIdOrderByFechaDesc(UUID empresaId);

	/**
	 * Página de registros de la empresa, más recientes primero. {@code buscar} (opcional, sin distinguir
	 * mayúsculas) filtra por acción o detalle. La auditoría crece sin techo: no se puede devolver entera.
	 */
	@Query("select r from RegistroDeAuditoriaJpaEntity r where r.empresaId = :empresaId "
			+ "and (cast(:buscar as string) is null or lower(r.accion) like lower(concat('%', cast(:buscar as string), '%')) "
			+ "or lower(r.detalle) like lower(concat('%', cast(:buscar as string), '%'))) order by r.fecha desc")
	Page<RegistroDeAuditoriaJpaEntity> buscarPagina(@Param("empresaId") UUID empresaId,
			@Param("buscar") String buscar, Pageable pageable);
}
