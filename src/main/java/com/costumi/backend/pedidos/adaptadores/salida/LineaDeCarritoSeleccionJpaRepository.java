package com.costumi.backend.pedidos.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface LineaDeCarritoSeleccionJpaRepository extends JpaRepository<LineaDeCarritoSeleccionJpaEntity, UUID> {

	List<LineaDeCarritoSeleccionJpaEntity> findByLineaIdIn(Collection<UUID> lineaIds);

	void deleteByLineaIdIn(Collection<UUID> lineaIds);
}
