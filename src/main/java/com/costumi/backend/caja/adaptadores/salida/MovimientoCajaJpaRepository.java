package com.costumi.backend.caja.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

interface MovimientoCajaJpaRepository extends JpaRepository<MovimientoCajaJpaEntity, UUID> {

	List<MovimientoCajaJpaEntity> findByTurnoIdOrderByOrden(UUID turnoId);

	@Transactional
	void deleteByTurnoId(UUID turnoId);
}
