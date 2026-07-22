package com.costumi.backend.disfraces.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface DisfrazJpaRepository extends JpaRepository<DisfrazJpaEntity, UUID> {

	List<DisfrazJpaEntity> findByEmpresaId(UUID empresaId);

	/**
	 * Página de disfraces de la empresa, por nombre. {@code buscar} (sin distinguir mayúsculas) filtra por
	 * nombre y {@code categoriaId} por categoría; ambos opcionales. Se pagina en la BD para no traer el
	 * catálogo entero (ni calcular precios sugeridos de disfraces que nadie va a ver).
	 */
	@org.springframework.data.jpa.repository.Query("select d from DisfrazJpaEntity d "
			+ "where d.empresaId = :empresaId "
			+ "and (cast(:buscar as string) is null or lower(d.nombre) like lower(concat('%', cast(:buscar as string), '%'))) "
			+ "and (:categoriaId is null or d.categoriaId = :categoriaId) order by d.nombre asc")
	org.springframework.data.domain.Page<DisfrazJpaEntity> buscarPagina(
			@org.springframework.data.repository.query.Param("empresaId") UUID empresaId,
			@org.springframework.data.repository.query.Param("buscar") String buscar,
			@org.springframework.data.repository.query.Param("categoriaId") UUID categoriaId,
			org.springframework.data.domain.Pageable pageable);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<DisfrazJpaEntity> findFirstById(UUID id);
}
