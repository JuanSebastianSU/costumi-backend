package com.costumi.backend.notificaciones.adaptadores.salida;

import com.costumi.backend.notificaciones.dominio.TipoDeEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PlantillaJpaRepository extends JpaRepository<PlantillaJpaEntity, UUID> {

	/** findFirstBy... atraviesa el filtro multi-tenant (§5.4), a diferencia de findById. */
	Optional<PlantillaJpaEntity> findFirstByEmpresaIdAndTipo(UUID empresaId, TipoDeEvento tipo);

	List<PlantillaJpaEntity> findByEmpresaId(UUID empresaId);
}
