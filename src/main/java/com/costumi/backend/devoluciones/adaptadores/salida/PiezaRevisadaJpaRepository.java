package com.costumi.backend.devoluciones.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface PiezaRevisadaJpaRepository extends JpaRepository<PiezaRevisadaJpaEntity, UUID> {

	List<PiezaRevisadaJpaEntity> findByDevolucionId(UUID devolucionId);
}
