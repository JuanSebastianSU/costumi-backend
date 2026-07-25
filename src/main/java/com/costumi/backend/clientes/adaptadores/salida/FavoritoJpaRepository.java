package com.costumi.backend.clientes.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface FavoritoJpaRepository extends JpaRepository<FavoritoJpaEntity, UUID> {

	List<FavoritoJpaEntity> findByUsuarioIdOrderByGuardadoEnDesc(UUID usuarioId);

	Optional<FavoritoJpaEntity> findByUsuarioIdAndDisfrazId(UUID usuarioId, UUID disfrazId);

	void deleteByUsuarioIdAndDisfrazId(UUID usuarioId, UUID disfrazId);
}
