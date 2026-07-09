package com.costumi.backend.ventas.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

interface LineaDeVentaJpaRepository extends JpaRepository<LineaDeVentaJpaEntity, UUID> {

	List<LineaDeVentaJpaEntity> findByVentaId(UUID ventaId);

	@Transactional
	void deleteByVentaId(UUID ventaId);
}
