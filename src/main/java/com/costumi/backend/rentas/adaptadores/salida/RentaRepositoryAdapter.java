package com.costumi.backend.rentas.adaptadores.salida;

import com.costumi.backend.rentas.dominio.EstadoRenta;
import com.costumi.backend.rentas.dominio.Renta;
import com.costumi.backend.rentas.dominio.RentaLinea;
import com.costumi.backend.rentas.dominio.RentaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link RentaRepository} con JPA (cabecera + líneas). */
@Repository
class RentaRepositoryAdapter implements RentaRepository {

	/** Estados que ocupan una unidad de stock en el calendario (RF-3.2). */
	private static final Set<EstadoRenta> ESTADOS_VIGENTES = Set.of(EstadoRenta.RESERVADA, EstadoRenta.ACTIVA);

	private final RentaJpaRepository jpa;
	private final RentaLineaJpaRepository lineasJpa;

	@PersistenceContext
	private EntityManager em;

	RentaRepositoryAdapter(RentaJpaRepository jpa, RentaLineaJpaRepository lineasJpa) {
		this.jpa = jpa;
		this.lineasJpa = lineasJpa;
	}

	@Override
	public Renta guardar(Renta renta) {
		// La cabecera guarda el artículo principal (primera línea) de forma denormalizada, para las
		// vistas por una sola prenda (rentas vencidas, devolución, contrato). El detalle multi-artículo
		// vive en renta_linea.
		jpa.save(aEntidad(renta));
		// Las líneas son inmutables tras crear la renta: solo se insertan la primera vez (create),
		// no en las actualizaciones de estado (entregar/devolver/extender).
		if (lineasJpa.findByRentaId(renta.id()).isEmpty()) {
			for (RentaLinea linea : renta.lineas()) {
				lineasJpa.save(new RentaLineaJpaEntity(UUID.randomUUID(), renta.id(), renta.empresaId(),
						linea.prendaId(), linea.cantidad(), linea.precioPorDia()));
			}
		}
		return renta;
	}

	@Override
	public Optional<Renta> buscarPorId(UUID id) {
		return jpa.findFirstById(id).map(this::aDominio);
	}

	@Override
	public List<Renta> listarPorEmpresa(UUID empresaId) {
		return jpa.findByEmpresaId(empresaId).stream().map(this::aDominio).toList();
	}

	@Override
	public List<Renta> listarPorCliente(UUID empresaId, UUID clienteId) {
		return jpa.findByEmpresaIdAndClienteId(empresaId, clienteId).stream().map(this::aDominio).toList();
	}

	@Override
	public long cantidadSolapada(UUID empresaId, UUID prendaId, LocalDate retiro, LocalDate devolucion) {
		return jpa.sumarCantidadSolapada(empresaId, prendaId, retiro, devolucion, ESTADOS_VIGENTES);
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
		return jpa.findByEmpresaIdAndClaveIdempotencia(empresaId, claveIdempotencia).map(this::aDominio);
	}

	private static RentaJpaEntity aEntidad(Renta r) {
		return new RentaJpaEntity(r.id(), r.empresaId(), r.sucursalId(), r.clienteId(), r.prendaId(),
				r.fechaRetiro(), r.fechaDevolucion(), r.precioPorDia(), r.deposito(), r.importe(), r.estado(),
				r.claveIdempotencia());
	}

	private Renta aDominio(RentaJpaEntity e) {
		List<RentaLinea> lineas = lineasJpa.findByRentaId(e.getId()).stream()
				.map(l -> RentaLinea.de(l.getPrendaId(), l.getCantidad(), l.getPrecioPorDia()))
				.toList();
		if (lineas.isEmpty()) {
			// Resiliencia ante filas previas a la migración: reconstruye la línea del artículo principal.
			lineas = List.of(RentaLinea.de(e.getPrendaId(), 1, e.getPrecioPorDia()));
		}
		return Renta.rehidratar(e.getId(), e.getEmpresaId(), e.getSucursalId(), e.getClienteId(), lineas,
				e.getFechaRetiro(), e.getFechaDevolucion(), e.getDeposito(), e.getImporte(), e.getEstado(),
				e.getClaveIdempotencia());
	}
}
