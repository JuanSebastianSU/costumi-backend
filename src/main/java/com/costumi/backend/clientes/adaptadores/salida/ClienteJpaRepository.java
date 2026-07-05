package com.costumi.backend.clientes.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface ClienteJpaRepository extends JpaRepository<ClienteJpaEntity, UUID> {

	List<ClienteJpaEntity> findByEmpresaId(UUID empresaId);

	@Query("""
			select c from ClienteJpaEntity c
			where c.empresaId = :empresaId
			  and (lower(c.nombre) like lower(concat('%', :texto, '%'))
			       or lower(c.documento) like lower(concat('%', :texto, '%'))
			       or c.telefono like concat('%', :texto, '%'))
			""")
	List<ClienteJpaEntity> buscar(@Param("empresaId") UUID empresaId, @Param("texto") String texto);
}
