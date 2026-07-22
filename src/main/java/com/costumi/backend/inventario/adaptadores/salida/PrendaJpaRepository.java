package com.costumi.backend.inventario.adaptadores.salida;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PrendaJpaRepository extends JpaRepository<PrendaJpaEntity, UUID> {

	List<PrendaJpaEntity> findByEmpresaId(UUID empresaId);

	Page<PrendaJpaEntity> findByEmpresaId(UUID empresaId, Pageable pageable);

	/** Página de prendas de la empresa; {@code buscar} (opcional) filtra por nombre sin distinguir mayúsculas. */
	@org.springframework.data.jpa.repository.Query("select p from PrendaJpaEntity p where p.empresaId = :empresaId "
			+ "and (cast(:buscar as string) is null or lower(p.nombre) like lower(concat('%', cast(:buscar as string), '%')))")
	Page<PrendaJpaEntity> buscarPagina(
			@org.springframework.data.repository.query.Param("empresaId") UUID empresaId,
			@org.springframework.data.repository.query.Param("buscar") String buscar, Pageable pageable);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<PrendaJpaEntity> findFirstById(UUID id);
}
