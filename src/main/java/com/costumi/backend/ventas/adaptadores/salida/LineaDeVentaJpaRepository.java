package com.costumi.backend.ventas.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface LineaDeVentaJpaRepository extends JpaRepository<LineaDeVentaJpaEntity, UUID> {

	List<LineaDeVentaJpaEntity> findByVentaId(UUID ventaId);

	/** Líneas de varias ventas en una sola consulta (evita el N+1 al paginar, C3). */
	List<LineaDeVentaJpaEntity> findByVentaIdIn(Collection<UUID> ventaIds);

	@Transactional
	void deleteByVentaId(UUID ventaId);
}
