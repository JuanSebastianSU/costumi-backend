package com.costumi.backend.pedidos.adaptadores.salida;

import com.costumi.backend.pedidos.dominio.Carrito;
import com.costumi.backend.pedidos.dominio.CarritoRepository;
import com.costumi.backend.pedidos.dominio.EstadoCarrito;
import com.costumi.backend.pedidos.dominio.LineaDeCarrito;
import com.costumi.backend.pedidos.dominio.TipoPedido;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: persiste el agregado Carrito (cabecera + líneas) con JPA. */
@Repository
class CarritoRepositoryAdapter implements CarritoRepository {

	private final CarritoJpaRepository cabeceras;
	private final LineaDeCarritoJpaRepository lineas;

	CarritoRepositoryAdapter(CarritoJpaRepository cabeceras, LineaDeCarritoJpaRepository lineas) {
		this.cabeceras = cabeceras;
		this.lineas = lineas;
	}

	@Override
	public Carrito guardar(Carrito carrito) {
		cabeceras.save(new CarritoJpaEntity(carrito.id(), carrito.empresaId(), carrito.sucursalId(),
				carrito.clienteId(), carrito.tipo(), carrito.estado()));
		lineas.deleteByCarritoId(carrito.id());
		for (LineaDeCarrito linea : carrito.lineas()) {
			lineas.save(new LineaDeCarritoJpaEntity(UUID.randomUUID(), carrito.id(), carrito.empresaId(),
					linea.prendaId(), linea.cantidad()));
		}
		return carrito;
	}

	@Override
	public Optional<Carrito> buscarPorId(UUID id) {
		return cabeceras.findById(id).map(this::aDominio);
	}

	@Override
	public Optional<Carrito> buscarPendiente(UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo) {
		return cabeceras
				.findByEmpresaIdAndSucursalIdAndClienteIdAndTipoAndEstado(
						empresaId, sucursalId, clienteId, tipo, EstadoCarrito.PENDIENTE)
				.map(this::aDominio);
	}

	private Carrito aDominio(CarritoJpaEntity cabecera) {
		List<LineaDeCarrito> lineasDominio = lineas.findByCarritoId(cabecera.getId()).stream()
				.map(l -> LineaDeCarrito.de(l.getPrendaId(), l.getCantidad()))
				.toList();
		return Carrito.rehidratar(cabecera.getId(), cabecera.getEmpresaId(), cabecera.getSucursalId(),
				cabecera.getClienteId(), cabecera.getTipo(), cabecera.getEstado(), lineasDominio);
	}
}
