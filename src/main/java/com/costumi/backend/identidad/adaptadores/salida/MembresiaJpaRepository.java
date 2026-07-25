package com.costumi.backend.identidad.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MembresiaJpaRepository extends JpaRepository<MembresiaJpaEntity, UUID> {

	List<MembresiaJpaEntity> findByUsuarioId(UUID usuarioId);

	Optional<MembresiaJpaEntity> findByUsuarioIdAndEmpresaId(UUID usuarioId, UUID empresaId);
}
