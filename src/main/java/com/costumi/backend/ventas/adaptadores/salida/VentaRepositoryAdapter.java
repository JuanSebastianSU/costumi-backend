package com.costumi.backend.ventas.adaptadores.salida;

import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;
import com.costumi.backend.ventas.dominio.LineaDeVenta;
import com.costumi.backend.ventas.dominio.Venta;
import com.costumi.backend.ventas.dominio.VentaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

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
			lineas.save(new LineaDeVentaJpaEntity(UUID.randomUUID(), venta.id(), venta.empresaId(),
					linea.prendaId(), linea.cantidad(), linea.precioUnitario(), linea.cantidadDevuelta()));
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
	public Pagina<Venta> listar(UUID empresaId, SolicitudDePagina solicitud) {
		Pageable pageable = PageRequest.of(solicitud.pagina(), solicitud.tamano(),
				Sort.by(Sort.Order.desc("creadaEn"), Sort.Order.asc("id")));
		Page<VentaJpaEntity> pagina = cabeceras.findByEmpresaId(empresaId, pageable);
		return new Pagina<>(aDominioEnLote(pagina.getContent()), pagina.getTotalElements(),
				solicitud.pagina(), solicitud.tamano());
	}

	/** Rehidrata una página de ventas cargando TODAS sus líneas en una sola consulta (evita el N+1, C3). */
	private List<Venta> aDominioEnLote(List<VentaJpaEntity> cabecerasPagina) {
		if (cabecerasPagina.isEmpty()) {
			return List.of();
		}
		List<UUID> ids = cabecerasPagina.stream().map(VentaJpaEntity::getId).toList();
		Map<UUID, List<LineaDeVenta>> lineasPorVenta = lineas.findByVentaIdIn(ids).stream()
				.collect(Collectors.groupingBy(LineaDeVentaJpaEntity::getVentaId, Collectors.mapping(
						l -> LineaDeVenta.rehidratar(l.getPrendaId(), l.getCantidad(), l.getPrecioUnitario(),
								l.getCantidadDevuelta()), Collectors.toList())));
		return cabecerasPagina.stream()
				.map(c -> aDominioConLineas(c, lineasPorVenta.getOrDefault(c.getId(), List.of())))
				.toList();
	}

	@Override
	public Optional<Venta> buscarPorClave(UUID empresaId, String claveIdempotencia) {
		return cabeceras.findByEmpresaIdAndClaveIdempotencia(empresaId, claveIdempotencia).map(this::aDominio);
	}

	private Venta aDominio(VentaJpaEntity cabecera) {
		List<LineaDeVenta> lineasDominio = lineas.findByVentaId(cabecera.getId()).stream()
				.map(l -> LineaDeVenta.rehidratar(l.getPrendaId(), l.getCantidad(), l.getPrecioUnitario(),
						l.getCantidadDevuelta()))
				.toList();
		return aDominioConLineas(cabecera, lineasDominio);
	}

	private static Venta aDominioConLineas(VentaJpaEntity cabecera, List<LineaDeVenta> lineasDominio) {
		return Venta.rehidratar(cabecera.getId(), cabecera.getEmpresaId(), cabecera.getSucursalId(),
				cabecera.getEmpleadoId(), cabecera.getClienteId(), cabecera.getDescuento(), cabecera.getTotal(),
				cabecera.getEstado(), lineasDominio, cabecera.getClaveIdempotencia(), cabecera.getCreadaEn());
	}
}
