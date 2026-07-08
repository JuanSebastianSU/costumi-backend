package com.costumi.backend.rentas.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface RentaLineaJpaRepository extends JpaRepository<RentaLineaJpaEntity, UUID> {

	List<RentaLineaJpaEntity> findByRentaId(UUID rentaId);
}
