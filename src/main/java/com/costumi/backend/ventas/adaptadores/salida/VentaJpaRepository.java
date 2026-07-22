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

	/**
	 * Página de ventas; {@code buscar} filtra por el CÓDIGO DE RETIRO que el cliente muestra en la tienda
	 * (p. ej. {@code V-75EC3602}). El código son los 8 primeros caracteres del id, así que se busca por
	 * prefijo; el llamador envía el texto ya normalizado (sin el prefijo y en minúsculas).
	 */
	@org.springframework.data.jpa.repository.Query("select v from VentaJpaEntity v where v.empresaId = :empresaId "
			+ "and (cast(:buscar as string) is null or lower(cast(v.id as string)) like concat(cast(:buscar as string), '%'))")
	Page<VentaJpaEntity> buscarPagina(
			@org.springframework.data.repository.query.Param("empresaId") UUID empresaId,
			@org.springframework.data.repository.query.Param("buscar") String buscar, Pageable pageable);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<VentaJpaEntity> findFirstById(UUID id);

	/** Venta con esa clave de idempotencia en la empresa, si existe (RF-17.6). */
	Optional<VentaJpaEntity> findByEmpresaIdAndClaveIdempotencia(UUID empresaId, String claveIdempotencia);
}
