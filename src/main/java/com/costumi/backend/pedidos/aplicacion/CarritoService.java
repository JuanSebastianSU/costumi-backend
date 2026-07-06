package com.costumi.backend.pedidos.aplicacion;

import com.costumi.backend.inventario.ConsultaDeInventario;
import com.costumi.backend.pedidos.dominio.Carrito;
import com.costumi.backend.pedidos.dominio.CarritoRepository;
import com.costumi.backend.pedidos.dominio.LineaDeCarrito;
import com.costumi.backend.pedidos.dominio.TipoPedido;
import com.costumi.backend.ventas.RegistroDeVentas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Casos de uso del carrito, acotados a la empresa (tenant) y segmentados por RF-16. */
@Service
class CarritoService implements AgregarItemAlCarrito, ConsultarCarrito, HacerCheckout {

	private final CarritoRepository carritos;
	private final ConsultaDeInventario inventario;
	private final RegistroDeVentas ventas;

	CarritoService(CarritoRepository carritos, ConsultaDeInventario inventario, RegistroDeVentas ventas) {
		this.carritos = carritos;
		this.inventario = inventario;
		this.ventas = ventas;
	}

	@Override
	@Transactional
	public Carrito ejecutar(AgregarItemAlCarritoComando comando) {
		Carrito carrito = carritos
				.buscarPendiente(comando.empresaId(), comando.sucursalId(), comando.clienteId(), comando.tipo())
				.orElseGet(() -> Carrito.crear(
						comando.empresaId(), comando.sucursalId(), comando.clienteId(), comando.tipo()));
		carrito.agregarItem(comando.prendaId(), comando.cantidad());
		return carritos.guardar(carrito);
	}

	@Override
	@Transactional(readOnly = true)
	public Carrito pendiente(UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo) {
		return carritos.buscarPendiente(empresaId, sucursalId, clienteId, tipo)
				.orElseThrow(CarritoNoEncontrado::new);
	}

	@Override
	@Transactional
	public UUID ejecutar(UUID empresaId, UUID sucursalId, UUID clienteId, UUID empleadoId) {
		Carrito carrito = carritos.buscarPendiente(empresaId, sucursalId, clienteId, TipoPedido.VENTA)
				.orElseThrow(CarritoNoEncontrado::new);
		List<RegistroDeVentas.ItemDeVenta> items = carrito.lineas().stream()
				.map(linea -> new RegistroDeVentas.ItemDeVenta(linea.prendaId(), linea.cantidad(),
						precioDe(empresaId, linea)))
				.toList();
		UUID ventaId = ventas.registrar(empresaId, sucursalId, empleadoId, clienteId, items);
		carrito.confirmar();
		carritos.guardar(carrito);
		return ventaId;
	}

	private BigDecimal precioDe(UUID empresaId, LineaDeCarrito linea) {
		return inventario.precioVenta(empresaId, linea.prendaId())
				.orElseThrow(() -> new IllegalArgumentException("La prenda no tiene precio de venta"));
	}
}
