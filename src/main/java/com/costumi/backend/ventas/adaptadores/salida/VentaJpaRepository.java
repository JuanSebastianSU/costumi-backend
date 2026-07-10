package com.costumi.backend.ventas.adaptadores.salida;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface VentaJpaRepository extends JpaRepository<VentaJpaEntity, UUID> {

	List<VentaJpaEntity> findByEmpresaId(UUID empresaId);

	Page<VentaJpaEntity> findByEmpresaId(UUID empresaId, Pageable pageable);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<VentaJpaEntity> findFirstById(UUID id);

	/** Venta con esa clave de idempotencia en la empresa, si existe (RF-17.6). */
	Optional<VentaJpaEntity> findByEmpresaIdAndClaveIdempotencia(UUID empresaId, String claveIdempotencia);
}
