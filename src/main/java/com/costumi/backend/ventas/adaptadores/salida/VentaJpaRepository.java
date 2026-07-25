package com.costumi.backend.ventas.adaptadores.salida;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
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
			+ "and (:estado is null or v.estado = :estado) "
			+ "and v.creadaEn >= :desde and v.creadaEn < :hasta "
			+ "and (cast(:buscar as string) is null or lower(cast(v.id as string)) like concat(cast(:buscar as string), '%'))")
	Page<VentaJpaEntity> buscarPagina(
			@org.springframework.data.repository.query.Param("empresaId") UUID empresaId,
			@org.springframework.data.repository.query.Param("buscar") String buscar,
			@org.springframework.data.repository.query.Param("estado") com.costumi.backend.ventas.dominio.EstadoVenta estado,
			@org.springframework.data.repository.query.Param("desde") Instant desde,
			@org.springframework.data.repository.query.Param("hasta") Instant hasta,
			Pageable pageable);

	/** Cantidad y suma de {@code total} de las ventas de la empresa con los mismos filtros (para el período de G8). */
	@org.springframework.data.jpa.repository.Query("select count(v), coalesce(sum(v.total), 0) from VentaJpaEntity v "
			+ "where v.empresaId = :empresaId and (:estado is null or v.estado = :estado) "
			+ "and v.creadaEn >= :desde and v.creadaEn < :hasta")
	List<Object[]> totalesRaw(
			@org.springframework.data.repository.query.Param("empresaId") UUID empresaId,
			@org.springframework.data.repository.query.Param("estado") com.costumi.backend.ventas.dominio.EstadoVenta estado,
			@org.springframework.data.repository.query.Param("desde") Instant desde,
			@org.springframework.data.repository.query.Param("hasta") Instant hasta);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<VentaJpaEntity> findFirstById(UUID id);

	/** Venta con esa clave de idempotencia en la empresa, si existe (RF-17.6). */
	Optional<VentaJpaEntity> findByEmpresaIdAndClaveIdempotencia(UUID empresaId, String claveIdempotencia);
}
