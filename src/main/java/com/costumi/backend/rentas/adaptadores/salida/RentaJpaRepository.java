package com.costumi.backend.rentas.adaptadores.salida;

import com.costumi.backend.rentas.dominio.EstadoRenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface RentaJpaRepository extends JpaRepository<RentaJpaEntity, UUID> {

	List<RentaJpaEntity> findByEmpresaId(UUID empresaId);

	List<RentaJpaEntity> findByEmpresaIdAndClienteId(UUID empresaId, UUID clienteId);

	Page<RentaJpaEntity> findByEmpresaId(UUID empresaId, Pageable pageable);

	Page<RentaJpaEntity> findByEmpresaIdAndClienteId(UUID empresaId, UUID clienteId, Pageable pageable);

	Optional<RentaJpaEntity> findByEmpresaIdAndClaveIdempotencia(UUID empresaId, String claveIdempotencia);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<RentaJpaEntity> findFirstById(UUID id);

	/**
	 * Suma las <b>unidades</b> de la prenda comprometidas por rentas cuyo periodo se <b>traslapa</b>
	 * (extremos inclusivos) con {@code [retiro, devolucion]} y están en un estado que ocupa stock
	 * (RESERVADA/ACTIVA), RF-3.2. Multi-artículo: cuenta la cantidad de cada línea, no las rentas.
	 */
	@Query("""
			select coalesce(sum(l.cantidad), 0) from RentaLineaJpaEntity l, RentaJpaEntity r
			where r.id = l.rentaId and r.empresaId = :empresaId and l.prendaId = :prendaId
			  and r.estado in :estados
			  and r.fechaRetiro <= :devolucion and :retiro <= r.fechaDevolucion
			""")
	long sumarCantidadSolapada(@Param("empresaId") UUID empresaId, @Param("prendaId") UUID prendaId,
			@Param("retiro") LocalDate retiro, @Param("devolucion") LocalDate devolucion,
			@Param("estados") Collection<EstadoRenta> estados);
}
