package com.costumi.backend.devoluciones.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface DevolucionJpaRepository extends JpaRepository<DevolucionJpaEntity, UUID> {

	List<DevolucionJpaEntity> findByEmpresaId(UUID empresaId);
}
