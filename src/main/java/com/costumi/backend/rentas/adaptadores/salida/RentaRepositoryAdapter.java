package com.costumi.backend.rentas.adaptadores.salida;

import com.costumi.backend.rentas.dominio.EstadoRenta;
import com.costumi.backend.rentas.dominio.Renta;
import com.costumi.backend.rentas.dominio.RentaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link RentaRepository} con JPA. */
@Repository
class RentaRepositoryAdapter implements RentaRepository {

	/** Estados que ocupan una unidad de stock en el calendario (RF-3.2). */
	private static final Set<EstadoRenta> ESTADOS_VIGENTES = Set.of(EstadoRenta.RESERVADA, EstadoRenta.ACTIVA);

	private final RentaJpaRepository jpa;

	@PersistenceContext
	private EntityManager em;

	RentaRepositoryAdapter(RentaJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Renta guardar(Renta renta) {
		return aDominio(jpa.save(aEntidad(renta)));
	}

	@Override
	public Optional<Renta> buscarPorId(UUID id) {
		return jpa.findFirstById(id).map(RentaRepositoryAdapter::aDominio);
	}

	@Override
	public List<Renta> listarPorEmpresa(UUID empresaId) {
		return jpa.findByEmpresaId(empresaId).stream().map(RentaRepositoryAdapter::aDominio).toList();
	}

	@Override
	public List<Renta> listarPorCliente(UUID empresaId, UUID clienteId) {
		return jpa.findByEmpresaIdAndClienteId(empresaId, clienteId).stream()
				.map(RentaRepositoryAdapter::aDominio).toList();
	}

	@Override
	public long contarSolapadas(UUID empresaId, UUID prendaId, LocalDate retiro, LocalDate devolucion) {
		return jpa.contarSolapadas(empresaId, prendaId, retiro, devolucion, ESTADOS_VIGENTES);
	}

	@Override
	public void bloquearReservaDePrenda(UUID prendaId) {
		// Lock de transacción por prenda (se libera al commit); serializa reservas concurrentes (RF-0.4).
		// Se envuelve en count(*) para no mapear el tipo void que devuelve pg_advisory_xact_lock.
		em.createNativeQuery(
						"select count(*) from (select pg_advisory_xact_lock(hashtext(cast(:prenda as text)))) as lock_")
				.setParameter("prenda", prendaId.toString())
				.getSingleResult();
	}

	@Override
	public Optional<Renta> buscarPorClave(UUID empresaId, String claveIdempotencia) {
		return jpa.findByEmpresaIdAndClaveIdempotencia(empresaId, claveIdempotencia).map(RentaRepositoryAdapter::aDominio);
	}

	private static RentaJpaEntity aEntidad(Renta r) {
		return new RentaJpaEntity(r.id(), r.empresaId(), r.sucursalId(), r.clienteId(), r.prendaId(),
				r.fechaRetiro(), r.fechaDevolucion(), r.precioPorDia(), r.deposito(), r.importe(), r.estado(),
				r.claveIdempotencia());
	}

	private static Renta aDominio(RentaJpaEntity e) {
		return Renta.rehidratar(e.getId(), e.getEmpresaId(), e.getSucursalId(), e.getClienteId(), e.getPrendaId(),
				e.getFechaRetiro(), e.getFechaDevolucion(), e.getPrecioPorDia(), e.getDeposito(), e.getImporte(),
				e.getEstado(), e.getClaveIdempotencia());
	}
}
