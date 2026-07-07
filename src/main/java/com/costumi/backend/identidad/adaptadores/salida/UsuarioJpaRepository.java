package com.costumi.backend.identidad.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaEntity, UUID> {

	Optional<UsuarioJpaEntity> findByEmail(String email);

	/** Carga por PK vía query derivada (respeta el filtro multi-tenant §5.4), no el findById de em.find. */
	Optional<UsuarioJpaEntity> findFirstById(UUID id);
}
