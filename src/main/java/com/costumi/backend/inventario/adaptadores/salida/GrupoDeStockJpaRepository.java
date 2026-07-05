package com.costumi.backend.inventario.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface GrupoDeStockJpaRepository extends JpaRepository<GrupoDeStockJpaEntity, UUID> {

	List<GrupoDeStockJpaEntity> findByPrendaId(UUID prendaId);
}
