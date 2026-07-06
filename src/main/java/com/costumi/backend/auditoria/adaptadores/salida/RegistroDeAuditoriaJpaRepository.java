package com.costumi.backend.auditoria.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface RegistroDeAuditoriaJpaRepository extends JpaRepository<RegistroDeAuditoriaJpaEntity, UUID> {

	List<RegistroDeAuditoriaJpaEntity> findByEmpresaIdOrderByFechaDesc(UUID empresaId);
}
