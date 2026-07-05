package com.costumi.backend.ventas.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface VentaJpaRepository extends JpaRepository<VentaJpaEntity, UUID> {

	List<VentaJpaEntity> findByEmpresaId(UUID empresaId);
}
