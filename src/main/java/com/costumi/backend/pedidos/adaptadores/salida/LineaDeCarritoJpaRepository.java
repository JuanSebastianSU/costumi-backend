package com.costumi.backend.pedidos.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface LineaDeCarritoJpaRepository extends JpaRepository<LineaDeCarritoJpaEntity, UUID> {

	List<LineaDeCarritoJpaEntity> findByCarritoId(UUID carritoId);

	void deleteByCarritoId(UUID carritoId);
}
