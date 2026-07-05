package com.costumi.backend.rentas.adaptadores.salida;

import com.costumi.backend.rentas.dominio.EstadoRenta;
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

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<RentaJpaEntity> findFirstById(UUID id);

	/**
	 * Cuenta las rentas de la prenda cuyo periodo se <b>traslapa</b> (extremos inclusivos) con
	 * {@code [retiro, devolucion]} y están en un estado que ocupa una unidad (RESERVADA/ACTIVA), RF-3.2.
	 */
	@Query("""
			select count(r) from RentaJpaEntity r
			where r.empresaId = :empresaId and r.prendaId = :prendaId
			  and r.estado in :estados
			  and r.fechaRetiro <= :devolucion and :retiro <= r.fechaDevolucion
			""")
	long contarSolapadas(@Param("empresaId") UUID empresaId, @Param("prendaId") UUID prendaId,
			@Param("retiro") LocalDate retiro, @Param("devolucion") LocalDate devolucion,
			@Param("estados") Collection<EstadoRenta> estados);
}
