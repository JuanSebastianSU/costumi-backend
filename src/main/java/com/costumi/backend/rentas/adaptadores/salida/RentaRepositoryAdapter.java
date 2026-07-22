package com.costumi.backend.rentas.adaptadores.salida;

import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;
import com.costumi.backend.rentas.dominio.EstadoRenta;
import com.costumi.backend.rentas.dominio.Renta;
import com.costumi.backend.rentas.dominio.RentaLinea;
import com.costumi.backend.rentas.dominio.RentaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Adaptador de salida: implementa el puerto {@link RentaRepository} con JPA (cabecera + líneas). */
@Repository
class RentaRepositoryAdapter implements RentaRepository {

	/** Estados que ocupan una unidad de stock en el calendario (RF-3.2). */
	private static final Set<EstadoRenta> ESTADOS_VIGENTES = Set.of(EstadoRenta.RESERVADA, EstadoRenta.ACTIVA);

	private final RentaJpaRepository jpa;
	private final RentaLineaJpaRepository lineasJpa;

	private static RentaLinea aLineaDeDominio(RentaLineaJpaEntity l) {
		return RentaLinea.de(l.getPrendaId(), l.getCantidad(), l.getPrecioPorDia(),
				com.costumi.backend.rentas.dominio.OrigenDisfraz.rehidratar(
						l.getDisfrazId(), l.getDisfrazGrupo(), l.getDisfrazCantidad(), l.getDisfrazNombre()));
	}

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
				com.costumi.backend.rentas.dominio.OrigenDisfraz origen = linea.origenDisfraz();
				lineasJpa.save(new RentaLineaJpaEntity(UUID.randomUUID(), renta.id(), renta.empresaId(),
						linea.prendaId(), linea.cantidad(), linea.precioPorDia(),
						origen == null ? null : origen.disfrazId(), origen == null ? null : origen.grupo(),
						origen == null ? null : origen.cantidad(), origen == null ? null : origen.nombre()));
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
	public Pagina<Renta> listar(UUID empresaId, UUID clienteId, String buscar, SolicitudDePagina solicitud) {
		Pageable pageable = PageRequest.of(solicitud.pagina(), solicitud.tamano(),
				Sort.by(Sort.Order.desc("fechaRetiro"), Sort.Order.asc("id")));
		Page<RentaJpaEntity> pagina = jpa.buscarPagina(empresaId, clienteId, normalizarCodigo(buscar), pageable);
		return new Pagina<>(aDominioEnLote(pagina.getContent()), pagina.getTotalElements(),
				solicitud.pagina(), solicitud.tamano());
	}

	/** Rehidrata una página de rentas cargando TODAS sus líneas en una sola consulta (evita el N+1, C3). */
	private List<Renta> aDominioEnLote(List<RentaJpaEntity> entidades) {
		if (entidades.isEmpty()) {
			return List.of();
		}
		List<UUID> ids = entidades.stream().map(RentaJpaEntity::getId).toList();
		Map<UUID, List<RentaLinea>> lineasPorRenta = lineasJpa.findByRentaIdIn(ids).stream()
				.collect(Collectors.groupingBy(RentaLineaJpaEntity::getRentaId, Collectors.mapping(
						RentaRepositoryAdapter::aLineaDeDominio, Collectors.toList())));
		return entidades.stream()
				.map(e -> aDominioConLineas(e, lineasPorRenta.getOrDefault(e.getId(), List.of())))
				.toList();
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
				r.claveIdempotencia(), r.empleadoId());
	}

	private Renta aDominio(RentaJpaEntity e) {
		List<RentaLinea> lineas = lineasJpa.findByRentaId(e.getId()).stream()
				.map(RentaRepositoryAdapter::aLineaDeDominio)
				.toList();
		return aDominioConLineas(e, lineas);
	}

	private static Renta aDominioConLineas(RentaJpaEntity e, List<RentaLinea> lineas) {
		if (lineas.isEmpty()) {
			// Resiliencia ante filas previas a la migración: reconstruye la línea del artículo principal.
			lineas = List.of(RentaLinea.de(e.getPrendaId(), 1, e.getPrecioPorDia()));
		}
		return Renta.rehidratar(e.getId(), e.getEmpresaId(), e.getSucursalId(), e.getClienteId(), e.getEmpleadoId(),
				lineas, e.getFechaRetiro(), e.getFechaDevolucion(), e.getDeposito(), e.getImporte(), e.getEstado(),
				e.getClaveIdempotencia());
	}

	/**
	 * El usuario escribe el código tal como lo ve ("V-75EC3602"); en la BD el id se guarda en minúsculas y
	 * sin prefijo, así que se quita el prefijo y se pasa a minúsculas para buscar por prefijo del id.
	 */
	private static String normalizarCodigo(String buscar) {
		if (buscar == null || buscar.isBlank()) {
			return null;
		}
		String limpio = buscar.trim().toLowerCase(java.util.Locale.ROOT);
		int guion = limpio.indexOf('-');
		return guion >= 0 && guion <= 2 ? limpio.substring(guion + 1) : limpio;
	}
}
