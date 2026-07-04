package com.costumi.backend.identidad.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SucursalJpaRepository extends JpaRepository<SucursalJpaEntity, UUID> {

	List<SucursalJpaEntity> findByEmpresaId(UUID empresaId);
}
