package com.costumi.backend.clientes.adaptadores.salida;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ClienteJpaRepository extends JpaRepository<ClienteJpaEntity, UUID> {

	List<ClienteJpaEntity> findByEmpresaId(UUID empresaId);

	Page<ClienteJpaEntity> findByEmpresaId(UUID empresaId, Pageable pageable);

	Page<ClienteJpaEntity> findByEmpresaIdAndIdIn(UUID empresaId, Collection<UUID> ids, Pageable pageable);

	@Query("""
			select c from ClienteJpaEntity c
			where c.empresaId = :empresaId
			  and (lower(c.nombre) like lower(concat('%', :texto, '%'))
			       or lower(c.documento) like lower(concat('%', :texto, '%'))
			       or c.telefono like concat('%', :texto, '%'))
			""")
	List<ClienteJpaEntity> buscar(@Param("empresaId") UUID empresaId, @Param("texto") String texto);

	@Query("""
			select c from ClienteJpaEntity c
			where c.empresaId = :empresaId
			  and (lower(c.nombre) like lower(concat('%', :texto, '%'))
			       or lower(c.documento) like lower(concat('%', :texto, '%'))
			       or c.telefono like concat('%', :texto, '%'))
			""")
	Page<ClienteJpaEntity> buscarPagina(@Param("empresaId") UUID empresaId, @Param("texto") String texto,
			Pageable pageable);

	@Query("""
			select c from ClienteJpaEntity c
			where c.empresaId = :empresaId
			  and c.id in :ids
			  and (lower(c.nombre) like lower(concat('%', :texto, '%'))
			       or lower(c.documento) like lower(concat('%', :texto, '%'))
			       or c.telefono like concat('%', :texto, '%'))
			""")
	Page<ClienteJpaEntity> buscarEnIds(@Param("empresaId") UUID empresaId, @Param("texto") String texto,
			@Param("ids") Collection<UUID> ids, Pageable pageable);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<ClienteJpaEntity> findFirstById(UUID id);

	Optional<ClienteJpaEntity> findByEmpresaIdAndUsuarioId(UUID empresaId, UUID usuarioId);

	List<ClienteJpaEntity> findByUsuarioId(UUID usuarioId);
}
