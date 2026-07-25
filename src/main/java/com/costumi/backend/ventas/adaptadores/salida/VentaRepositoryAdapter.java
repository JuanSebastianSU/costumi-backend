package com.costumi.backend.ventas.adaptadores.salida;

import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;
import com.costumi.backend.ventas.dominio.EstadoVenta;
import com.costumi.backend.ventas.dominio.LineaDeVenta;
import com.costumi.backend.ventas.dominio.TotalesDeVentas;
import com.costumi.backend.ventas.dominio.Venta;
import com.costumi.backend.ventas.dominio.VentaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** Adaptador de salida: persiste la Venta (cabecera + líneas) con JPA. */
@Repository
class VentaRepositoryAdapter implements VentaRepository {

	private final VentaJpaRepository cabeceras;
	private final LineaDeVentaJpaRepository lineas;

	VentaRepositoryAdapter(VentaJpaRepository cabeceras, LineaDeVentaJpaRepository lineas) {
		this.cabeceras = cabeceras;
		this.lineas = lineas;
	}

	@Override
	public Venta guardar(Venta venta) {
		cabeceras.save(new VentaJpaEntity(venta.id(), venta.empresaId(), venta.sucursalId(), venta.empleadoId(),
				venta.clienteId(), venta.descuento(), venta.total(), venta.estado(), venta.claveIdempotencia(),
				venta.creadaEn()));
		// Reescribe las líneas (idempotente): al devolver por partes la venta se guarda varias veces.
		lineas.deleteByVentaId(venta.id());
		for (LineaDeVenta linea : venta.lineas()) {
			com.costumi.backend.ventas.dominio.OrigenDisfraz origen = linea.origenDisfraz();
			lineas.save(new LineaDeVentaJpaEntity(UUID.randomUUID(), venta.id(), venta.empresaId(),
					linea.prendaId(), linea.cantidad(), linea.precioUnitario(), linea.cantidadDevuelta(),
					origen == null ? null : origen.disfrazId(), origen == null ? null : origen.grupo(),
					origen == null ? null : origen.cantidad(), origen == null ? null : origen.nombre()));
		}
		return venta;
	}

	@Override
	public Optional<Venta> buscarPorId(UUID id) {
		return cabeceras.findFirstById(id).map(this::aDominio);
	}

	@Override
	public List<Venta> listarPorEmpresa(UUID empresaId) {
		return cabeceras.findByEmpresaId(empresaId).stream().map(this::aDominio).toList();
	}

	@Override
	public Pagina<Venta> listar(UUID empresaId, String buscar, EstadoVenta estado, LocalDate desde, LocalDate hasta,
			SolicitudDePagina solicitud) {
		Pageable pageable = PageRequest.of(solicitud.pagina(), solicitud.tamano(),
				Sort.by(Sort.Order.desc("creadaEn"), Sort.Order.asc("id")));
		Page<VentaJpaEntity> pagina = cabeceras.buscarPagina(empresaId, normalizarCodigo(buscar), estado,
				inicioDe(desde), finDe(hasta), pageable);
		return new Pagina<>(aDominioEnLote(pagina.getContent()), pagina.getTotalElements(),
				solicitud.pagina(), solicitud.tamano());
	}

	@Override
	public TotalesDeVentas totales(UUID empresaId, EstadoVenta estado, LocalDate desde, LocalDate hasta) {
		Object[] r = cabeceras.totalesRaw(empresaId, estado, inicioDe(desde), finDe(hasta)).get(0);
		return new TotalesDeVentas((Long) r[0], (BigDecimal) r[1]);
	}

	// Rango de fechas → instantes: [desde 00:00, hasta+1 00:00) en UTC, para filtrar por creada_en (Instant).
	// Sin límite se pasa un centinela (no null): un param null rompe la inferencia de tipo de Postgres.
	private static final Instant SIN_LIMITE_INFERIOR = Instant.EPOCH; // 1970: no hay ventas antes
	private static final Instant SIN_LIMITE_SUPERIOR = LocalDate.of(9999, 12, 31).atStartOfDay(ZoneOffset.UTC).toInstant();

	private static Instant inicioDe(LocalDate desde) {
		return desde == null ? SIN_LIMITE_INFERIOR : desde.atStartOfDay(ZoneOffset.UTC).toInstant();
	}

	private static Instant finDe(LocalDate hasta) {
		return hasta == null ? SIN_LIMITE_SUPERIOR : hasta.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
	}

	/** Rehidrata una página de ventas cargando TODAS sus líneas en una sola consulta (evita el N+1, C3). */
	private List<Venta> aDominioEnLote(List<VentaJpaEntity> cabecerasPagina) {
		if (cabecerasPagina.isEmpty()) {
			return List.of();
		}
		List<UUID> ids = cabecerasPagina.stream().map(VentaJpaEntity::getId).toList();
		Map<UUID, List<LineaDeVenta>> lineasPorVenta = lineas.findByVentaIdIn(ids).stream()
				.collect(Collectors.groupingBy(LineaDeVentaJpaEntity::getVentaId, Collectors.mapping(
						VentaRepositoryAdapter::aLineaDeDominio, Collectors.toList())));
		return cabecerasPagina.stream()
				.map(c -> aDominioConLineas(c, lineasPorVenta.getOrDefault(c.getId(), List.of())))
				.toList();
	}

	@Override
	public Optional<Venta> buscarPorClave(UUID empresaId, String claveIdempotencia) {
		return cabeceras.findByEmpresaIdAndClaveIdempotencia(empresaId, claveIdempotencia).map(this::aDominio);
	}

	private static LineaDeVenta aLineaDeDominio(LineaDeVentaJpaEntity l) {
		return LineaDeVenta.rehidratar(l.getPrendaId(), l.getCantidad(), l.getPrecioUnitario(),
				l.getCantidadDevuelta(), com.costumi.backend.ventas.dominio.OrigenDisfraz.rehidratar(
						l.getDisfrazId(), l.getDisfrazGrupo(), l.getDisfrazCantidad(), l.getDisfrazNombre()));
	}

	private Venta aDominio(VentaJpaEntity cabecera) {
		List<LineaDeVenta> lineasDominio = lineas.findByVentaId(cabecera.getId()).stream()
				.map(VentaRepositoryAdapter::aLineaDeDominio)
				.toList();
		return aDominioConLineas(cabecera, lineasDominio);
	}

	private static Venta aDominioConLineas(VentaJpaEntity cabecera, List<LineaDeVenta> lineasDominio) {
		return Venta.rehidratar(cabecera.getId(), cabecera.getEmpresaId(), cabecera.getSucursalId(),
				cabecera.getEmpleadoId(), cabecera.getClienteId(), cabecera.getDescuento(), cabecera.getTotal(),
				cabecera.getEstado(), lineasDominio, cabecera.getClaveIdempotencia(), cabecera.getCreadaEn());
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
