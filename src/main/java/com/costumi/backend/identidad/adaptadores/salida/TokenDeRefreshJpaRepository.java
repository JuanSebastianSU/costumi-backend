package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.EstadoDeRefresh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface TokenDeRefreshJpaRepository extends JpaRepository<TokenDeRefreshJpaEntity, UUID> {

	Optional<TokenDeRefreshJpaEntity> findFirstByJti(UUID jti);

	/** Revoca toda la familia en una sola sentencia (logout / reuso detectado, C2). */
	@Modifying(clearAutomatically = true)
	@Query("update TokenDeRefreshJpaEntity t set t.estado = :estado where t.familiaId = :familiaId")
	void revocarFamilia(@Param("familiaId") UUID familiaId, @Param("estado") EstadoDeRefresh estado);
}
