package com.costumi.backend.rentas.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface RentaLineaJpaRepository extends JpaRepository<RentaLineaJpaEntity, UUID> {

	List<RentaLineaJpaEntity> findByRentaId(UUID rentaId);

	/** Líneas de varias rentas en una sola consulta (evita el N+1 al paginar, C3). */
	List<RentaLineaJpaEntity> findByRentaIdIn(Collection<UUID> rentaIds);
}
