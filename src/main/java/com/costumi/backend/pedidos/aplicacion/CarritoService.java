package com.costumi.backend.pedidos.aplicacion;

import com.costumi.backend.inventario.ConsultaDeInventario;
import com.costumi.backend.pedidos.dominio.Carrito;
import com.costumi.backend.pedidos.dominio.CarritoRepository;
import com.costumi.backend.pedidos.dominio.LineaDeCarrito;
import com.costumi.backend.pedidos.dominio.TipoPedido;
import com.costumi.backend.rentas.RegistroDeRentas;
import com.costumi.backend.ventas.RegistroDeVentas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Casos de uso del carrito, acotados a la empresa (tenant) y segmentados por RF-16. */
@Service
class CarritoService implements AgregarItemAlCarrito, ConsultarCarrito, HacerCheckout, HacerCheckoutDeRenta {

	private final CarritoRepository carritos;
	private final ConsultaDeInventario inventario;
	private final RegistroDeVentas ventas;
	private final RegistroDeRentas rentas;

	CarritoService(CarritoRepository carritos, ConsultaDeInventario inventario, RegistroDeVentas ventas,
			RegistroDeRentas rentas) {
		this.carritos = carritos;
		this.inventario = inventario;
		this.ventas = ventas;
		this.rentas = rentas;
	}

	@Override
	@Transactional
	public Carrito ejecutar(AgregarItemAlCarritoComando comando) {
		Carrito carrito = carritos
				.buscarPendiente(comando.empresaId(), comando.sucursalId(), comando.clienteId(), comando.tipo())
				.orElseGet(() -> Carrito.crear(
						comando.empresaId(), comando.sucursalId(), comando.clienteId(), comando.tipo()));
		carrito.agregarItem(comando.prendaId(), comando.cantidad(), comando.fechaRetiro(), comando.fechaDevolucion());
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
						precioVentaDe(empresaId, linea)))
				.toList();
		UUID ventaId = ventas.registrar(empresaId, sucursalId, empleadoId, clienteId, items);
		carrito.confirmar();
		carritos.guardar(carrito);
		return ventaId;
	}

	@Override
	@Transactional
	public List<UUID> ejecutar(UUID empresaId, UUID sucursalId, UUID clienteId) {
		Carrito carrito = carritos.buscarPendiente(empresaId, sucursalId, clienteId, TipoPedido.RENTA)
				.orElseThrow(CarritoNoEncontrado::new);
		// Agrupa las líneas por periodo (retiro/devolución): una renta multi-artículo por periodo distinto.
		Map<ClavePeriodo, List<LineaDeCarrito>> porPeriodo = new LinkedHashMap<>();
		for (LineaDeCarrito linea : carrito.lineas()) {
			porPeriodo.computeIfAbsent(new ClavePeriodo(linea.fechaRetiro(), linea.fechaDevolucion()),
					k -> new ArrayList<>()).add(linea);
		}
		List<UUID> rentaIds = new ArrayList<>();
		for (Map.Entry<ClavePeriodo, List<LineaDeCarrito>> grupo : porPeriodo.entrySet()) {
			ClavePeriodo periodo = grupo.getKey();
			List<RegistroDeRentas.ItemDeRenta> items = grupo.getValue().stream()
					.map(linea -> new RegistroDeRentas.ItemDeRenta(linea.prendaId(), linea.cantidad(),
							precioRentaDe(empresaId, linea)))
					.toList();
			// El depósito/garantía se gestiona en el pago (RF-6.2/6.8); la renta se crea sin depósito.
			rentaIds.add(rentas.registrar(empresaId, sucursalId, clienteId, periodo.retiro(), periodo.devolucion(),
					null, items));
		}
		carrito.confirmar();
		carritos.guardar(carrito);
		return rentaIds;
	}

	private BigDecimal precioVentaDe(UUID empresaId, LineaDeCarrito linea) {
		return inventario.precioVenta(empresaId, linea.prendaId())
				.orElseThrow(() -> new IllegalArgumentException("La prenda no tiene precio de venta"));
	}

	private BigDecimal precioRentaDe(UUID empresaId, LineaDeCarrito linea) {
		return inventario.precioRenta(empresaId, linea.prendaId())
				.orElseThrow(() -> new IllegalArgumentException("La prenda no tiene precio de renta"));
	}

	/** Clave de agrupación de líneas de renta por periodo. */
	private record ClavePeriodo(LocalDate retiro, LocalDate devolucion) {
	}
}
