package com.costumi.backend.inventario.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface PrendaJpaRepository extends JpaRepository<PrendaJpaEntity, UUID> {

	List<PrendaJpaEntity> findByEmpresaId(UUID empresaId);
}
