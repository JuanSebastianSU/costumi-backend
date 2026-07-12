package com.costumi.backend.pedidos.aplicacion;

import com.costumi.backend.compartido.ContextoDeTenant;
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
	private final ContextoDeTenant contexto;
	private final com.costumi.backend.identidad.ConsultaDeSucursales sucursales;
	private final com.costumi.backend.clientes.ResolucionDeClientes clientes;

	CarritoService(CarritoRepository carritos, ConsultaDeInventario inventario, RegistroDeVentas ventas,
			RegistroDeRentas rentas, ContextoDeTenant contexto,
			com.costumi.backend.identidad.ConsultaDeSucursales sucursales,
			com.costumi.backend.clientes.ResolucionDeClientes clientes) {
		this.carritos = carritos;
		this.inventario = inventario;
		this.ventas = ventas;
		this.rentas = rentas;
		this.contexto = contexto;
		this.sucursales = sucursales;
		this.clientes = clientes;
	}

	@Override
	@Transactional
	public Carrito ejecutar(AgregarItemAlCarritoComando comando) {
		// SEC-1: el carrito se ancla a una sucursal; debe existir, ser del tenant y estar activa.
		if (!sucursales.existeActiva(comando.empresaId(), comando.sucursalId())) {
			throw new IllegalArgumentException("La sucursal no existe o está archivada en esta empresa");
		}
		// SEC-2: el carrito pertenece a un cliente; debe existir y ser de esta empresa (en el flujo CLIENTE
		// la ficha ya viene resuelta/creada del token, así que siempre pasa).
		if (!clientes.existe(comando.empresaId(), comando.clienteId())) {
			throw new IllegalArgumentException("El cliente no existe en esta empresa");
		}
		Carrito carrito = carritos
				.buscarPendiente(comando.empresaId(), comando.sucursalId(), comando.clienteId(), comando.tipo())
				.orElseGet(() -> Carrito.crear(
						comando.empresaId(), comando.sucursalId(), comando.clienteId(), comando.tipo()));
		carrito.agregarItem(comando.prendaId(), comando.cantidad(), comando.fechaRetiro(), comando.fechaDevolucion());
		return carritos.guardar(carrito);
	}

	@Override
	@Transactional(readOnly = true)
	public CarritoValorizado pendiente(UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo) {
		Carrito carrito = carritos.buscarPendiente(empresaId, sucursalId, clienteId, tipo)
				.orElseThrow(CarritoNoEncontrado::new);
		// Valorizamos con la MISMA lógica que el checkout, para que el total mostrado coincida con
		// lo que se cobrará. VENTA: precioVenta × cantidad. RENTA: precioPorDía × cantidad × días.
		List<CarritoValorizado.LineaValorizada> lineas = new ArrayList<>();
		BigDecimal total = BigDecimal.ZERO;
		for (LineaDeCarrito linea : carrito.lineas()) {
			BigDecimal precioUnitario = (tipo == TipoPedido.VENTA
					? inventario.precioVenta(empresaId, linea.prendaId())
					: inventario.precioRenta(empresaId, linea.prendaId())).orElse(null);
			BigDecimal subtotal = null;
			if (precioUnitario != null) {
				subtotal = precioUnitario.multiply(BigDecimal.valueOf(linea.cantidad()));
				if (tipo == TipoPedido.RENTA) {
					subtotal = subtotal.multiply(BigDecimal.valueOf(dias(linea.fechaRetiro(), linea.fechaDevolucion())));
				}
				total = total.add(subtotal);
			}
			lineas.add(new CarritoValorizado.LineaValorizada(linea.prendaId(), linea.cantidad(),
					linea.fechaRetiro(), linea.fechaDevolucion(), precioUnitario, subtotal));
		}
		return new CarritoValorizado(carrito.id(), carrito.sucursalId(), carrito.clienteId(),
				carrito.tipo(), carrito.estado(), lineas, total);
	}

	/** Días facturables del periodo de renta (igual que el dominio Renta: al menos 1). */
	private static long dias(LocalDate retiro, LocalDate devolucion) {
		if (retiro == null || devolucion == null) {
			return 1;
		}
		return Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(retiro, devolucion));
	}

	@Override
	@Transactional
	public UUID ejecutar(UUID empresaId, UUID sucursalId, UUID clienteId, UUID empleadoId) {
		// Serializa el checkout para que un doble submit no cree ventas duplicadas (RF-17.6).
		carritos.bloquearPedido(empresaId, sucursalId, clienteId, TipoPedido.VENTA);
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
		UUID usuarioId = contexto.usuarioId().orElse(null); // quién confirma el pedido (RF-1.4)
		// Serializa el checkout para que un doble submit no cree rentas duplicadas (RF-17.6).
		carritos.bloquearPedido(empresaId, sucursalId, clienteId, TipoPedido.RENTA);
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
					null, items, usuarioId));
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
