package com.costumi.backend.notificaciones.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface NotificacionJpaRepository extends JpaRepository<NotificacionJpaEntity, UUID> {

	List<NotificacionJpaEntity> findByEmpresaId(UUID empresaId);

	/**
	 * Página de notificaciones de la empresa, más recientes primero. {@code buscar} (opcional, sin
	 * distinguir mayúsculas) filtra por el texto del mensaje. Crecen sin techo: no se devuelven enteras.
	 */
	@org.springframework.data.jpa.repository.Query("select n from NotificacionJpaEntity n "
			+ "where n.empresaId = :empresaId and (cast(:buscar as string) is null "
			+ "or lower(n.mensaje) like lower(concat('%', cast(:buscar as string), '%'))) order by n.fecha desc")
	org.springframework.data.domain.Page<NotificacionJpaEntity> buscarPagina(
			@org.springframework.data.repository.query.Param("empresaId") UUID empresaId,
			@org.springframework.data.repository.query.Param("buscar") String buscar,
			org.springframework.data.domain.Pageable pageable);
}
