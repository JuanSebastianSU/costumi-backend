package com.costumi.backend.pedidos.adaptadores.salida;

import com.costumi.backend.pedidos.dominio.Carrito;
import com.costumi.backend.pedidos.dominio.CarritoRepository;
import com.costumi.backend.pedidos.dominio.EstadoCarrito;
import com.costumi.backend.pedidos.dominio.LineaDeCarrito;
import com.costumi.backend.pedidos.dominio.SeleccionDeSlot;
import com.costumi.backend.pedidos.dominio.TipoPedido;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: persiste el agregado Carrito (cabecera + líneas + selecciones) con JPA. */
@Repository
class CarritoRepositoryAdapter implements CarritoRepository {

	private final CarritoJpaRepository cabeceras;
	private final LineaDeCarritoJpaRepository lineas;
	private final LineaDeCarritoSeleccionJpaRepository selecciones;

	@PersistenceContext
	private EntityManager em;

	CarritoRepositoryAdapter(CarritoJpaRepository cabeceras, LineaDeCarritoJpaRepository lineas,
			LineaDeCarritoSeleccionJpaRepository selecciones) {
		this.cabeceras = cabeceras;
		this.lineas = lineas;
		this.selecciones = selecciones;
	}

	@Override
	public void bloquearPedido(UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo) {
		// Lock de transacción por pedido (se libera al commit); serializa checkouts concurrentes (RF-17.6).
		// Se envuelve en count(*) para no mapear el tipo void que devuelve pg_advisory_xact_lock.
		String clave = empresaId + ":" + sucursalId + ":" + clienteId + ":" + tipo.name();
		em.createNativeQuery(
						"select count(*) from (select pg_advisory_xact_lock(hashtext(cast(:clave as text)))) as lock_")
				.setParameter("clave", clave)
				.getSingleResult();
	}

	@Override
	public Carrito guardar(Carrito carrito) {
		cabeceras.save(new CarritoJpaEntity(carrito.id(), carrito.empresaId(), carrito.sucursalId(),
				carrito.clienteId(), carrito.tipo(), carrito.estado()));
		// Borra-y-recrea las líneas (y sus selecciones): el adaptador rehace el detalle del carrito.
		List<UUID> lineasViejas = lineas.findByCarritoId(carrito.id()).stream()
				.map(LineaDeCarritoJpaEntity::getId).toList();
		if (!lineasViejas.isEmpty()) {
			selecciones.deleteByLineaIdIn(lineasViejas);
		}
		lineas.deleteByCarritoId(carrito.id());
		for (LineaDeCarrito linea : carrito.lineas()) {
			// Se conserva el id de la línea del dominio: es estable y el cliente lo usa para quitarla.
			UUID lineaId = linea.id();
			lineas.save(new LineaDeCarritoJpaEntity(lineaId, carrito.id(), carrito.empresaId(),
					linea.prendaId(), linea.disfrazId(), linea.cantidad(), linea.fechaRetiro(),
					linea.fechaDevolucion()));
			for (SeleccionDeSlot seleccion : linea.selecciones()) {
				selecciones.save(new LineaDeCarritoSeleccionJpaEntity(UUID.randomUUID(), lineaId,
						carrito.empresaId(), seleccion.orden(), seleccion.prendaId()));
			}
		}
		return carrito;
	}

	@Override
	public Optional<Carrito> buscarPorId(UUID id) {
		return cabeceras.findFirstById(id).map(this::aDominio);
	}

	@Override
	public Optional<Carrito> buscarPendiente(UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo) {
		return cabeceras
				.findByEmpresaIdAndSucursalIdAndClienteIdAndTipoAndEstado(
						empresaId, sucursalId, clienteId, tipo, EstadoCarrito.PENDIENTE)
				.map(this::aDominio);
	}

	private Carrito aDominio(CarritoJpaEntity cabecera) {
		List<LineaDeCarritoJpaEntity> filas = lineas.findByCarritoId(cabecera.getId());
		List<UUID> lineaIds = filas.stream().map(LineaDeCarritoJpaEntity::getId).toList();
		// Selecciones de todas las líneas del carrito en UNA consulta (sin N+1), agrupadas por línea.
		Map<UUID, List<SeleccionDeSlot>> seleccionesPorLinea = new HashMap<>();
		if (!lineaIds.isEmpty()) {
			for (LineaDeCarritoSeleccionJpaEntity s : selecciones.findByLineaIdIn(lineaIds)) {
				seleccionesPorLinea.computeIfAbsent(s.getLineaId(), k -> new ArrayList<>())
						.add(new SeleccionDeSlot(s.getOrden(), s.getPrendaId()));
			}
		}
		List<LineaDeCarrito> lineasDominio = filas.stream()
				.map(l -> LineaDeCarrito.rehidratar(l.getId(), l.getPrendaId(), l.getDisfrazId(),
						seleccionesPorLinea.getOrDefault(l.getId(), List.of()), l.getCantidad(),
						l.getFechaRetiro(), l.getFechaDevolucion()))
				.toList();
		return Carrito.rehidratar(cabecera.getId(), cabecera.getEmpresaId(), cabecera.getSucursalId(),
				cabecera.getClienteId(), cabecera.getTipo(), cabecera.getEstado(), lineasDominio);
	}
}
