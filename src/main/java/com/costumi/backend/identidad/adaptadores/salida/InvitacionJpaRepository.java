package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.EstadoInvitacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface InvitacionJpaRepository extends JpaRepository<InvitacionJpaEntity, UUID> {

	Optional<InvitacionJpaEntity> findFirstById(UUID id);

	Optional<InvitacionJpaEntity> findFirstByTokenHash(String tokenHash);

	List<InvitacionJpaEntity> findByEmpresaIdAndEstado(UUID empresaId, EstadoInvitacion estado);

	Optional<InvitacionJpaEntity> findFirstByEmpresaIdAndEmailIgnoreCaseAndEstado(UUID empresaId, String email,
			EstadoInvitacion estado);
}
