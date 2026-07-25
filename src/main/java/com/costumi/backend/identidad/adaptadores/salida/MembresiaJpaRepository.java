package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.EstadoMembresia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MembresiaJpaRepository extends JpaRepository<MembresiaJpaEntity, UUID> {

	List<MembresiaJpaEntity> findByUsuarioId(UUID usuarioId);

	Optional<MembresiaJpaEntity> findByUsuarioIdAndEmpresaId(UUID usuarioId, UUID empresaId);

	/** La membresía en ese estado (el índice único parcial V77 garantiza a lo sumo una ACTIVA por usuario). */
	Optional<MembresiaJpaEntity> findFirstByUsuarioIdAndEstado(UUID usuarioId, EstadoMembresia estado);
}
