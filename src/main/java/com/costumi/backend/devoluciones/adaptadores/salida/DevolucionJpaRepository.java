package com.costumi.backend.devoluciones.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface DevolucionJpaRepository extends JpaRepository<DevolucionJpaEntity, UUID> {

	List<DevolucionJpaEntity> findByEmpresaId(UUID empresaId);

	/**
	 * Página de devoluciones de la empresa. {@code buscar} (sin distinguir mayúsculas) filtra por la
	 * descripción que el personal escribió en alguna pieza revisada (p. ej. "mancha"), que es el único
	 * texto libre de una devolución.
	 */
	@org.springframework.data.jpa.repository.Query("select d from DevolucionJpaEntity d "
			+ "where d.empresaId = :empresaId and (cast(:buscar as string) is null or exists ("
			+ "  select 1 from PiezaRevisadaJpaEntity p where p.devolucionId = d.id "
			+ "  and lower(p.descripcion) like lower(concat('%', cast(:buscar as string), '%'))))")
	org.springframework.data.domain.Page<DevolucionJpaEntity> buscarPagina(
			@org.springframework.data.repository.query.Param("empresaId") UUID empresaId,
			@org.springframework.data.repository.query.Param("buscar") String buscar,
			org.springframework.data.domain.Pageable pageable);

	List<DevolucionJpaEntity> findByEmpresaIdAndRentaId(UUID empresaId, UUID rentaId);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<DevolucionJpaEntity> findFirstById(UUID id);
}
