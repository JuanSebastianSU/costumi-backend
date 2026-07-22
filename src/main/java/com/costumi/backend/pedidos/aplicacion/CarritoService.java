package com.costumi.backend.pedidos.aplicacion;

import com.costumi.backend.compartido.ContextoDeTenant;
import com.costumi.backend.disfraces.ResolucionDeDisfraces;
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
class CarritoService implements AgregarItemAlCarrito, QuitarItemDelCarrito, ConsultarCarrito, HacerCheckout,
		HacerCheckoutDeRenta {

	private final CarritoRepository carritos;
	private final ConsultaDeInventario inventario;
	private final ResolucionDeDisfraces disfraces;
	private final RegistroDeVentas ventas;
	private final RegistroDeRentas rentas;
	private final ContextoDeTenant contexto;
	private final com.costumi.backend.identidad.ConsultaDeSucursales sucursales;
	private final com.costumi.backend.clientes.ResolucionDeClientes clientes;

	CarritoService(CarritoRepository carritos, ConsultaDeInventario inventario, ResolucionDeDisfraces disfraces,
			RegistroDeVentas ventas, RegistroDeRentas rentas, ContextoDeTenant contexto,
			com.costumi.backend.identidad.ConsultaDeSucursales sucursales,
			com.costumi.backend.clientes.ResolucionDeClientes clientes) {
		this.carritos = carritos;
		this.inventario = inventario;
		this.disfraces = disfraces;
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
		if (comando.esDisfraz()) {
			// SEC-3: el disfraz debe existir en la empresa (evita FK 500 y anclar basura al carrito).
			if (disfraces.resumenDeDisfraces(comando.empresaId(), List.of(comando.disfrazId())).isEmpty()) {
				throw new IllegalArgumentException("El disfraz no existe en esta empresa");
			}
			carrito.agregarDisfraz(comando.disfrazId(), comando.selecciones(), comando.cantidad(),
					comando.fechaRetiro(), comando.fechaDevolucion());
		} else {
			carrito.agregarItem(comando.prendaId(), comando.cantidad(), comando.fechaRetiro(),
					comando.fechaDevolucion());
		}
		return carritos.guardar(carrito);
	}

	@Override
	@Transactional
	public Carrito ejecutar(UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo, UUID lineaId) {
		Carrito carrito = carritos.buscarPendiente(empresaId, sucursalId, clienteId, tipo)
				.orElseThrow(CarritoNoEncontrado::new);
		carrito.quitarLinea(lineaId);
		return carritos.guardar(carrito);
	}

	@Override
	@Transactional(readOnly = true)
	public CarritoValorizado pendiente(UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo) {
		Carrito carrito = carritos.buscarPendiente(empresaId, sucursalId, clienteId, tipo)
				.orElseThrow(CarritoNoEncontrado::new);
		// Valorizamos con la MISMA lógica que el checkout, para que el total mostrado coincida con
		// lo que se cobrará. VENTA: precioVenta × cantidad. RENTA: precioPorDía × cantidad × días.
		// Se consultan de una sola vez los disfraces del carrito para saber, ANTES de valorizar, si alguno
		// dejó de admitir esta operación (el dueño pudo cambiarle el tipo después de que el cliente lo agregó).
		Map<UUID, ResolucionDeDisfraces.ResumenDeDisfraz> resumenes = disfraces.resumenDeDisfraces(empresaId,
				carrito.lineas().stream().filter(LineaDeCarrito::esDisfraz).map(LineaDeCarrito::disfrazId).toList());
		List<CarritoValorizado.LineaValorizada> lineas = new ArrayList<>();
		BigDecimal total = BigDecimal.ZERO;
		for (LineaDeCarrito linea : carrito.lineas()) {
			CarritoValorizado.LineaValorizada valorizada = valorizar(empresaId, tipo, linea, resumenes);
			if (valorizada.subtotal() != null) {
				total = total.add(valorizada.subtotal());
			}
			lineas.add(valorizada);
		}
		return new CarritoValorizado(carrito.id(), carrito.sucursalId(), carrito.clienteId(),
				carrito.tipo(), carrito.estado(), lineas, total);
	}

	/**
	 * Valoriza una línea sin dejar caer el carrito entero: si el disfraz ya no admite esta operación (o
	 * desapareció de la empresa), la línea vuelve con precio nulo y el motivo, en vez de romper la consulta
	 * y dejar al cliente sin forma de quitarla.
	 */
	private CarritoValorizado.LineaValorizada valorizar(UUID empresaId, TipoPedido tipo, LineaDeCarrito linea,
			Map<UUID, ResolucionDeDisfraces.ResumenDeDisfraz> resumenes) {
		if (!linea.esDisfraz()) {
			return valorizarPrenda(empresaId, tipo, linea);
		}
		String motivo = motivoNoDisponible(tipo, resumenes.get(linea.disfrazId()));
		return motivo != null ? noDisponible(linea, motivo) : valorizarDisfraz(empresaId, tipo, linea);
	}

	/** Motivo por el que la línea de un disfraz ya no sirve para este carrito, o {@code null} si sirve. */
	private static String motivoNoDisponible(TipoPedido tipo, ResolucionDeDisfraces.ResumenDeDisfraz resumen) {
		if (resumen == null) {
			return "Este disfraz ya no está disponible";
		}
		if (tipo == TipoPedido.VENTA && !resumen.permiteVenta()) {
			return "Este disfraz es solo para renta, no se puede comprar";
		}
		if (tipo == TipoPedido.RENTA && !resumen.permiteRenta()) {
			return "Este disfraz es solo para venta, no se puede rentar";
		}
		return null;
	}

	private static CarritoValorizado.LineaValorizada noDisponible(LineaDeCarrito linea, String motivo) {
		return new CarritoValorizado.LineaValorizada(linea.id(), linea.prendaId(), linea.disfrazId(),
				linea.selecciones(), linea.cantidad(), linea.fechaRetiro(), linea.fechaDevolucion(), null, null,
				motivo);
	}

	/** Valoriza una línea de prenda: precioUnitario × cantidad (× días si es renta). */
	private CarritoValorizado.LineaValorizada valorizarPrenda(UUID empresaId, TipoPedido tipo, LineaDeCarrito linea) {
		BigDecimal precioUnitario = (tipo == TipoPedido.VENTA
				? inventario.precioVenta(empresaId, linea.prendaId())
				: inventario.precioRenta(empresaId, linea.prendaId())).orElse(null);
		BigDecimal subtotal = null;
		if (precioUnitario != null) {
			subtotal = precioUnitario.multiply(BigDecimal.valueOf(linea.cantidad()));
			if (tipo == TipoPedido.RENTA) {
				subtotal = subtotal.multiply(BigDecimal.valueOf(dias(linea.fechaRetiro(), linea.fechaDevolucion())));
			}
		}
		String motivo = precioUnitario == null
				? (tipo == TipoPedido.VENTA ? "Esta prenda ya no tiene precio de venta"
						: "Esta prenda ya no tiene precio de renta")
				: null;
		return new CarritoValorizado.LineaValorizada(linea.id(), linea.prendaId(), null, List.of(), linea.cantidad(),
				linea.fechaRetiro(), linea.fechaDevolucion(), precioUnitario, subtotal, motivo);
	}

	/**
	 * Valoriza una línea de disfraz: se resuelve a sus piezas con {@link ResolucionDeDisfraces} (misma
	 * lógica que el checkout, para que el total coincida). {@code precioUnitario} = precio de UN disfraz
	 * (suma de las piezas); {@code subtotal} = precioUnitario × cantidad (× días si es renta).
	 */
	private CarritoValorizado.LineaValorizada valorizarDisfraz(UUID empresaId, TipoPedido tipo, LineaDeCarrito linea) {
		List<ResolucionDeDisfraces.LineaResuelta> resueltas = tipo == TipoPedido.VENTA
				? disfraces.lineasDeVenta(empresaId, linea.disfrazId(), linea.cantidad(), aSelecciones(linea))
				: disfraces.lineasDeRenta(empresaId, linea.disfrazId(), linea.cantidad(), aSelecciones(linea));
		BigDecimal precioUnitario = resueltas.stream()
				.map(ResolucionDeDisfraces.LineaResuelta::precio)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(linea.cantidad()));
		if (tipo == TipoPedido.RENTA) {
			subtotal = subtotal.multiply(BigDecimal.valueOf(dias(linea.fechaRetiro(), linea.fechaDevolucion())));
		}
		return new CarritoValorizado.LineaValorizada(linea.id(), null, linea.disfrazId(), linea.selecciones(),
				linea.cantidad(), linea.fechaRetiro(), linea.fechaDevolucion(), precioUnitario, subtotal, null);
	}

	/**
	 * Nombre con el que se cobra el disfraz. Se guarda en la línea (no se resuelve al leer) para que el
	 * pedido histórico no cambie si después lo renombran.
	 */
	private String nombreDeDisfraz(UUID empresaId, UUID disfrazId) {
		ResolucionDeDisfraces.ResumenDeDisfraz resumen = disfraces
				.resumenDeDisfraces(empresaId, List.of(disfrazId)).get(disfrazId);
		return resumen == null ? null : resumen.nombre();
	}

	/** Traduce las selecciones de dominio del carrito a las del puerto de Disfraces. */
	private static List<ResolucionDeDisfraces.SeleccionDeSlot> aSelecciones(LineaDeCarrito linea) {
		return linea.selecciones().stream()
				.map(s -> new ResolucionDeDisfraces.SeleccionDeSlot(s.orden(), s.prendaId()))
				.toList();
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
		List<RegistroDeVentas.ItemDeVenta> items = new ArrayList<>();
		for (LineaDeCarrito linea : carrito.lineas()) {
			if (linea.esDisfraz()) {
				// El disfraz se resuelve a sus piezas valuadas (precio ya repartido si tiene precio general).
				// El grupo identifica ESTA instancia del disfraz: dos líneas del mismo disfraz con piezas
				// distintas no deben mezclarse al mostrar el pedido.
				UUID grupo = UUID.randomUUID();
				for (ResolucionDeDisfraces.LineaResuelta r : disfraces.lineasDeVenta(empresaId, linea.disfrazId(),
						linea.cantidad(), aSelecciones(linea))) {
					items.add(new RegistroDeVentas.ItemDeVenta(r.prendaId(), r.cantidad(), r.precio(),
							linea.disfrazId(), grupo, linea.cantidad(), nombreDeDisfraz(empresaId, linea.disfrazId())));
				}
			} else {
				items.add(new RegistroDeVentas.ItemDeVenta(linea.prendaId(), linea.cantidad(),
						precioVentaDe(empresaId, linea)));
			}
		}
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
			List<RegistroDeRentas.ItemDeRenta> items = new ArrayList<>();
			for (LineaDeCarrito linea : grupo.getValue()) {
				if (linea.esDisfraz()) {
					// El disfraz se resuelve a sus piezas valuadas (precio por día ya repartido si tiene general).
					UUID grupoDisfraz = UUID.randomUUID();
					for (ResolucionDeDisfraces.LineaResuelta r : disfraces.lineasDeRenta(empresaId, linea.disfrazId(),
							linea.cantidad(), aSelecciones(linea))) {
						items.add(new RegistroDeRentas.ItemDeRenta(r.prendaId(), r.cantidad(), r.precio(),
								linea.disfrazId(), grupoDisfraz, linea.cantidad(),
								nombreDeDisfraz(empresaId, linea.disfrazId())));
					}
				} else {
					items.add(new RegistroDeRentas.ItemDeRenta(linea.prendaId(), linea.cantidad(),
							precioRentaDe(empresaId, linea)));
				}
			}
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
