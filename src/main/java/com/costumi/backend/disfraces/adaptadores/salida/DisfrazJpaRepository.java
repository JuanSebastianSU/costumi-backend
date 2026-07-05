package com.costumi.backend.disfraces.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface DisfrazJpaRepository extends JpaRepository<DisfrazJpaEntity, UUID> {

	List<DisfrazJpaEntity> findByEmpresaId(UUID empresaId);
}
