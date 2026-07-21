package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.ConsultaDeStockDePool;
import com.costumi.backend.disfraces.dominio.Disfraz;
import com.costumi.backend.disfraces.dominio.DisfrazRepository;
import com.costumi.backend.disfraces.dominio.EjeDePrenda;
import com.costumi.backend.disfraces.dominio.PoolDeSlot;
import com.costumi.backend.disfraces.dominio.Slot;
import com.costumi.backend.catalogo.ConsultaDeTaxonomia;
import com.costumi.backend.inventario.AlmacenDeImagenesPublico;
import com.costumi.backend.inventario.ConsultaDeInventario;
import com.costumi.backend.rentas.RegistroDeRentas;
import com.costumi.backend.ventas.RegistroDeVentas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.costumi.backend.inventario.ConsultaDeInventario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Casos de uso de Disfraces (Capa 3), acotados a la empresa (tenant). */
@Service
class DisfrazService implements CrearDisfraz, EditarDisfraz, CambiarEstadoDisfraz, ConsultarDisfraces,
		ConsultarDisponibilidadDeDisfraz, ConsultarOpcionesDeSlot, RentarDisfraz, VenderDisfraz,
		RentarVariosDisfraces, VenderVariosDisfraces, AsignarFotoDeDisfraz {

	private final DisfrazRepository disfraces;
	private final ConsultaDeInventario inventario;
	private final ConsultaDeTaxonomia taxonomia;
	private final RegistroDeRentas rentas;
	private final RegistroDeVentas ventas;
	private final AlmacenDeImagenesPublico almacenDeImagenes;

	DisfrazService(DisfrazRepository disfraces, ConsultaDeInventario inventario, ConsultaDeTaxonomia taxonomia,
			RegistroDeRentas rentas, RegistroDeVentas ventas, AlmacenDeImagenesPublico almacenDeImagenes) {
		this.disfraces = disfraces;
		this.inventario = inventario;
		this.taxonomia = taxonomia;
		this.rentas = rentas;
		this.ventas = ventas;
		this.almacenDeImagenes = almacenDeImagenes;
	}

	@Override
	@Transactional
	public Disfraz ejecutar(UUID empresaId, UUID disfrazId, byte[] contenido) {
		Disfraz disfraz = exigirDelTenant(empresaId, disfrazId);
		String url = almacenDeImagenes.subir(contenido, "disfraces/" + empresaId + "/" + disfrazId + "/");
		disfraz.asignarFoto(url);
		return disfraces.guardar(disfraz);
	}

	@Override
	@Transactional
	public Disfraz ejecutar(CrearDisfrazComando comando) {
		comando.slots().forEach(slot -> validarSlotDelTenant(comando.empresaId(), slot));
		Disfraz disfraz = Disfraz.crear(comando.empresaId(), comando.nombre(), aSlots(comando.slots()),
				comando.precioRentaGeneral());
		return disfraces.guardar(disfraz);
	}

	@Override
	@Transactional
	public Disfraz ejecutar(EditarDisfrazComando comando) {
		Disfraz disfraz = disfraces.buscarPorId(comando.disfrazId())
				.filter(d -> d.empresaId().equals(comando.empresaId()))
				.orElseThrow(() -> new DisfrazNoEncontrado(comando.disfrazId()));
		comando.slots().forEach(slot -> validarSlotDelTenant(comando.empresaId(), slot));
		disfraz.redefinir(comando.nombre(), aSlots(comando.slots()), comando.precioRentaGeneral());
		return disfraces.guardar(disfraz);
	}

	@Override
	@Transactional
	public Disfraz archivar(UUID empresaId, UUID disfrazId) {
		Disfraz disfraz = exigirDelTenant(empresaId, disfrazId);
		disfraz.archivar();
		return disfraces.guardar(disfraz);
	}

	@Override
	@Transactional
	public Disfraz activar(UUID empresaId, UUID disfrazId) {
		Disfraz disfraz = exigirDelTenant(empresaId, disfrazId);
		disfraz.activar();
		return disfraces.guardar(disfraz);
	}

	/** §5.4: toda referencia por id (prenda fija, categoría y valores del pool) debe ser del tenant. */
	private void validarSlotDelTenant(UUID empresaId, SlotComando slot) {
		switch (slot.ejePrenda()) {
			case FIJA -> exigirPrendaDelTenant(empresaId, slot.prendaFijaId(), "La prenda fija");
			case PERSONALIZABLE -> {
				// Opciones explícitas: cada prenda elegida debe ser del tenant. Es la forma preferida.
				if (!slot.prendasOpcion().isEmpty()) {
					slot.prendasOpcion().forEach(id -> exigirPrendaDelTenant(empresaId, id, "Una opción del slot"));
					return;
				}
				PoolComando pool = slot.pool();
				if (pool == null || pool.categoriaId() == null
						|| !taxonomia.categoriaExiste(empresaId, pool.categoriaId())) {
					throw new IllegalArgumentException("La categoría del pool no existe en esta empresa");
				}
				pool.etiquetasPermitidas().forEach((tipo, valores) -> valores.forEach(valor -> {
					if (!taxonomia.valorPerteneceATipo(empresaId, tipo, valor)) {
						throw new IllegalArgumentException("Un valor permitido del pool no pertenece a su tipo en esta empresa");
					}
				}));
			}
		}
	}

	private void exigirPrendaDelTenant(UUID empresaId, UUID prendaId, String descripcion) {
		if (prendaId == null || !inventario.prendaExiste(empresaId, prendaId)) {
			throw new IllegalArgumentException(descripcion + " no existe en esta empresa");
		}
	}

	private Disfraz exigirDelTenant(UUID empresaId, UUID disfrazId) {
		return disfraces.buscarPorId(disfrazId)
				.filter(d -> d.empresaId().equals(empresaId))
				.orElseThrow(() -> new DisfrazNoEncontrado(disfrazId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Disfraz> deEmpresa(UUID empresaId) {
		return disfraces.listarPorEmpresa(empresaId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Disfraz> activosDeEmpresa(UUID empresaId) {
		return disfraces.listarPorEmpresa(empresaId).stream().filter(Disfraz::activo).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal precioRentaSugerido(UUID empresaId, Disfraz disfraz) {
		BigDecimal total = BigDecimal.ZERO;
		for (Slot slot : disfraz.slots()) {
			total = total.add(precioDeSlot(empresaId, slot));
		}
		return total;
	}

	/** Precio de renta de un slot: el de la prenda fija, o el mínimo ("desde") entre las opciones elegibles. */
	private BigDecimal precioDeSlot(UUID empresaId, Slot slot) {
		if (slot.ejePrenda() == EjeDePrenda.FIJA) {
			return inventario.precioRenta(empresaId, slot.prendaFijaId()).orElse(BigDecimal.ZERO);
		}
		return opcionesElegibles(empresaId, slot).stream()
				.map(ConsultaDeInventario.OpcionDePool::precioRenta)
				.filter(java.util.Objects::nonNull)
				.min(BigDecimal::compareTo)
				.orElse(BigDecimal.ZERO);
	}

	/**
	 * Opciones elegibles de un slot personalizable como {@link ConsultaDeInventario.OpcionDePool}: las
	 * prendas explícitas del slot (si las tiene) o las derivadas del pool. Las prendas explícitas que ya
	 * no existan en el inventario se omiten (no rompen el cálculo).
	 */
	private List<ConsultaDeInventario.OpcionDePool> opcionesElegibles(UUID empresaId, Slot slot) {
		if (slot.tieneOpcionesExplicitas()) {
			return slot.prendasOpcion().stream()
					.map(id -> inventario.opcionDePrenda(empresaId, id))
					.flatMap(java.util.Optional::stream)
					.toList();
		}
		PoolDeSlot pool = slot.pool();
		return inventario.opcionesDelPool(empresaId, pool.categoriaId(), pool.etiquetasPermitidas());
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal precioVentaSugerido(UUID empresaId, Disfraz disfraz) {
		BigDecimal total = BigDecimal.ZERO;
		for (Slot slot : disfraz.slots()) {
			total = total.add(precioVentaDeSlot(empresaId, slot));
		}
		return total;
	}

	/** Precio de venta de un slot: el de la prenda fija, o el mínimo ("desde") entre las opciones elegibles. */
	private BigDecimal precioVentaDeSlot(UUID empresaId, Slot slot) {
		if (slot.ejePrenda() == EjeDePrenda.FIJA) {
			return inventario.precioVenta(empresaId, slot.prendaFijaId()).orElse(BigDecimal.ZERO);
		}
		return opcionesElegibles(empresaId, slot).stream()
				.map(opcion -> inventario.precioVenta(empresaId, opcion.prendaId()).orElse(null))
				.filter(java.util.Objects::nonNull)
				.min(BigDecimal::compareTo)
				.orElse(BigDecimal.ZERO);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean estaDisponible(UUID empresaId, UUID disfrazId) {
		Disfraz disfraz = exigirDelTenant(empresaId, disfrazId);
		return disfraz.estaDisponible(consultaDeStockPara(empresaId));
	}

	@Override
	@Transactional(readOnly = true)
	public OpcionesDeSlot opciones(UUID empresaId, UUID disfrazId, int orden, List<UUID> valoresFiltro) {
		Disfraz disfraz = exigirDelTenant(empresaId, disfrazId);
		if (!disfraz.activo()) {
			throw new DisfrazNoEncontrado(disfrazId);
		}
		Slot slot = disfraz.slots().stream().filter(s -> s.orden() == orden).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("El slot " + orden + " no existe en el disfraz"));
		List<ConsultaDeInventario.OpcionDePool> opciones;
		if (slot.ejePrenda() == EjeDePrenda.FIJA) {
			opciones = inventario.opcionDePrenda(empresaId, slot.prendaFijaId()).map(List::of).orElseGet(List::of);
		} else {
			opciones = opcionesElegibles(empresaId, slot);
		}
		// Filtro adicional de la ruleta: la opción debe incluir TODOS los valores de etiqueta elegidos.
		if (valoresFiltro != null && !valoresFiltro.isEmpty()) {
			Set<UUID> requeridos = new HashSet<>(valoresFiltro);
			opciones = opciones.stream()
					.filter(opcion -> opcion.etiquetas().values().containsAll(requeridos))
					.toList();
		}
		return new OpcionesDeSlot(slot.orden(), slot.nombre(), slot.ejePrenda(), slot.opcional(), opciones);
	}

	@Override
	@Transactional
	public UUID ejecutar(RentarDisfrazComando comando) {
		UUID empresaId = comando.empresaId();
		List<RegistroDeRentas.ItemDeRenta> items = itemsRentaDe(empresaId, comando.disfrazId(), comando.cantidad(),
				seleccionesDeRenta(comando.selecciones()));
		return rentas.registrar(empresaId, comando.sucursalId(), comando.clienteId(), comando.fechaRetiro(),
				comando.fechaDevolucion(), null, items, comando.empleadoId());
	}

	@Override
	@Transactional
	public UUID ejecutar(RentarVariosDisfracesComando comando) {
		UUID empresaId = comando.empresaId();
		// Un pedido: se resuelve cada disfraz a sus piezas y se suman las prendas sueltas, todo en UNA sola renta.
		List<RegistroDeRentas.ItemDeRenta> todos = new ArrayList<>();
		if (comando.items() != null) {
			for (RentarVariosDisfracesComando.ItemDeDisfraz item : comando.items()) {
				Map<Integer, UUID> sel = new HashMap<>();
				if (item.selecciones() != null) {
					for (RentarVariosDisfracesComando.SeleccionDeSlot s : item.selecciones()) {
						sel.put(s.orden(), s.prendaId());
					}
				}
				todos.addAll(itemsRentaDe(empresaId, item.disfrazId(), Math.max(1, item.cantidad()), sel));
			}
		}
		if (comando.lineas() != null) {
			for (RentarVariosDisfracesComando.LineaDePrenda l : comando.lineas()) {
				todos.add(new RegistroDeRentas.ItemDeRenta(l.prendaId(), Math.max(1, l.cantidad()), l.precioPorDia()));
			}
		}
		if (todos.isEmpty()) {
			throw new IllegalArgumentException("El pedido no resolvió ningún artículo para rentar");
		}
		return rentas.registrar(empresaId, comando.sucursalId(), comando.clienteId(), comando.fechaRetiro(),
				comando.fechaDevolucion(), null, todos, comando.empleadoId());
	}

	@Override
	@Transactional
	public UUID ejecutar(VenderDisfrazComando comando) {
		UUID empresaId = comando.empresaId();
		List<RegistroDeVentas.ItemDeVenta> items = itemsVentaDe(empresaId, comando.disfrazId(), comando.cantidad(),
				seleccionesDeVenta(comando.selecciones()));
		return ventas.registrar(empresaId, comando.sucursalId(), comando.empleadoId(), comando.clienteId(), items);
	}

	@Override
	@Transactional
	public UUID ejecutar(VenderVariosDisfracesComando comando) {
		UUID empresaId = comando.empresaId();
		List<RegistroDeVentas.ItemDeVenta> todos = new ArrayList<>();
		if (comando.items() != null) {
			for (VenderVariosDisfracesComando.ItemDeDisfraz item : comando.items()) {
				Map<Integer, UUID> sel = new HashMap<>();
				if (item.selecciones() != null) {
					for (VenderVariosDisfracesComando.SeleccionDeSlot s : item.selecciones()) {
						sel.put(s.orden(), s.prendaId());
					}
				}
				todos.addAll(itemsVentaDe(empresaId, item.disfrazId(), Math.max(1, item.cantidad()), sel));
			}
		}
		if (comando.lineas() != null) {
			for (VenderVariosDisfracesComando.LineaDePrenda l : comando.lineas()) {
				todos.add(new RegistroDeVentas.ItemDeVenta(l.prendaId(), Math.max(1, l.cantidad()), l.precioUnitario()));
			}
		}
		if (todos.isEmpty()) {
			throw new IllegalArgumentException("El pedido no resolvió ningún artículo para vender");
		}
		return ventas.registrar(empresaId, comando.sucursalId(), comando.empleadoId(), comando.clienteId(), todos);
	}

	private static Map<Integer, UUID> seleccionesDeRenta(List<RentarDisfrazComando.SeleccionDeSlot> selecciones) {
		Map<Integer, UUID> m = new HashMap<>();
		if (selecciones != null) {
			for (RentarDisfrazComando.SeleccionDeSlot s : selecciones) {
				m.put(s.orden(), s.prendaId());
			}
		}
		return m;
	}

	private static Map<Integer, UUID> seleccionesDeVenta(List<VenderDisfrazComando.SeleccionDeSlot> selecciones) {
		Map<Integer, UUID> m = new HashMap<>();
		if (selecciones != null) {
			for (VenderDisfrazComando.SeleccionDeSlot s : selecciones) {
				m.put(s.orden(), s.prendaId());
			}
		}
		return m;
	}

	/** Resuelve un disfraz a sus líneas de renta (una por pieza), con la cantidad y el reparto de precio general. */
	private List<RegistroDeRentas.ItemDeRenta> itemsRentaDe(UUID empresaId, UUID disfrazId, int cantidad,
			Map<Integer, UUID> seleccionPorOrden) {
		Disfraz disfraz = exigirDelTenant(empresaId, disfrazId);
		if (!disfraz.activo()) {
			throw new IllegalArgumentException("El disfraz está archivado y no se puede rentar");
		}
		List<RegistroDeRentas.ItemDeRenta> items = new ArrayList<>();
		for (Slot slot : disfraz.slots()) {
			boolean elegido = seleccionPorOrden.containsKey(slot.orden());
			if (!slot.esObligatorio() && !elegido) {
				continue;
			}
			items.add(itemDeRenta(empresaId, resolverPrenda(empresaId, slot, seleccionPorOrden.get(slot.orden())),
					cantidad));
		}
		if (items.isEmpty()) {
			throw new IllegalArgumentException("El disfraz no resolvió ningún artículo para rentar");
		}
		// Precio general (RF-2.10) × cantidad: reparte el precio del conjunto entre las piezas.
		if (disfraz.tienePrecioGeneral()) {
			items = repartirPrecioGeneral(items,
					disfraz.precioRentaGeneral().multiply(BigDecimal.valueOf(cantidad)));
		}
		return items;
	}

	/** Resuelve un disfraz a sus líneas de venta (una por pieza), con la cantidad. */
	private List<RegistroDeVentas.ItemDeVenta> itemsVentaDe(UUID empresaId, UUID disfrazId, int cantidad,
			Map<Integer, UUID> seleccionPorOrden) {
		Disfraz disfraz = exigirDelTenant(empresaId, disfrazId);
		if (!disfraz.activo()) {
			throw new IllegalArgumentException("El disfraz está archivado y no se puede vender");
		}
		List<RegistroDeVentas.ItemDeVenta> items = new ArrayList<>();
		for (Slot slot : disfraz.slots()) {
			boolean elegido = seleccionPorOrden.containsKey(slot.orden());
			if (!slot.esObligatorio() && !elegido) {
				continue;
			}
			items.add(itemDeVenta(empresaId, resolverPrenda(empresaId, slot, seleccionPorOrden.get(slot.orden())),
					cantidad));
		}
		if (items.isEmpty()) {
			throw new IllegalArgumentException("El disfraz no resolvió ningún artículo para vender");
		}
		return items;
	}

	private RegistroDeVentas.ItemDeVenta itemDeVenta(UUID empresaId, UUID prendaId, int cantidad) {
		BigDecimal precio = inventario.precioVenta(empresaId, prendaId)
				.orElseThrow(() -> new IllegalArgumentException("La prenda del disfraz no tiene precio de venta"));
		return new RegistroDeVentas.ItemDeVenta(prendaId, cantidad, precio);
	}

	/**
	 * Reparte el {@code precioGeneral} (por día) entre las líneas proporcionalmente a su precio, de modo que
	 * la suma de las líneas iguale exactamente el precio del conjunto; la última línea absorbe el redondeo.
	 */
	private static List<RegistroDeRentas.ItemDeRenta> repartirPrecioGeneral(List<RegistroDeRentas.ItemDeRenta> items,
			BigDecimal precioGeneral) {
		BigDecimal base = items.stream()
				.map(it -> it.precioPorDia().multiply(BigDecimal.valueOf(it.cantidad())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		List<RegistroDeRentas.ItemDeRenta> resultado = new ArrayList<>();
		BigDecimal acumulado = BigDecimal.ZERO;
		for (int i = 0; i < items.size(); i++) {
			RegistroDeRentas.ItemDeRenta it = items.get(i);
			BigDecimal cantidad = BigDecimal.valueOf(it.cantidad());
			BigDecimal precioUnitario;
			if (i == items.size() - 1) {
				// La última línea toma el remanente para cuadrar el total exacto.
				precioUnitario = precioGeneral.subtract(acumulado).divide(cantidad, 2, RoundingMode.HALF_UP);
			} else if (base.signum() == 0) {
				precioUnitario = BigDecimal.ZERO;
			} else {
				BigDecimal lineaBase = it.precioPorDia().multiply(cantidad);
				BigDecimal lineaTotal = precioGeneral.multiply(lineaBase).divide(base, 2, RoundingMode.HALF_UP);
				precioUnitario = lineaTotal.divide(cantidad, 2, RoundingMode.HALF_UP);
				acumulado = acumulado.add(precioUnitario.multiply(cantidad));
			}
			resultado.add(new RegistroDeRentas.ItemDeRenta(it.prendaId(), it.cantidad(), precioUnitario));
		}
		return resultado;
	}

	/** Prenda concreta de un slot: la fija, o la elegida por el cliente validada contra sus opciones (RF-2.3). */
	private UUID resolverPrenda(UUID empresaId, Slot slot, UUID prendaElegida) {
		if (slot.ejePrenda() == EjeDePrenda.FIJA) {
			return slot.prendaFijaId();
		}
		if (prendaElegida == null) {
			throw new IllegalArgumentException("Falta elegir la prenda del slot '" + slot.nombre() + "'");
		}
		boolean valida = slot.tieneOpcionesExplicitas()
				? slot.prendasOpcion().contains(prendaElegida)
				: inventario.prendaEnPool(empresaId, prendaElegida, slot.pool().categoriaId(),
						slot.pool().etiquetasPermitidas());
		if (!valida) {
			throw new IllegalArgumentException(
					"La prenda elegida no es una opción del slot '" + slot.nombre() + "'");
		}
		return prendaElegida;
	}

	private RegistroDeRentas.ItemDeRenta itemDeRenta(UUID empresaId, UUID prendaId, int cantidad) {
		BigDecimal precio = inventario.precioRenta(empresaId, prendaId)
				.orElseThrow(() -> new IllegalArgumentException("La prenda del disfraz no tiene precio de renta"));
		return new RegistroDeRentas.ItemDeRenta(prendaId, cantidad, precio);
	}

	/** Puente del puerto de Inventario al puerto de dominio del Disfraz, fijado al tenant. */
	private ConsultaDeStockDePool consultaDeStockPara(UUID empresaId) {
		return new ConsultaDeStockDePool() {
			@Override
			public boolean prendaTieneStock(UUID prendaId) {
				return inventario.prendaTieneStockDisponible(empresaId, prendaId);
			}

			@Override
			public boolean poolTieneStock(UUID categoriaId, Map<UUID, Set<UUID>> etiquetasPermitidas) {
				return inventario.poolTieneStockDisponible(empresaId, categoriaId, etiquetasPermitidas);
			}
		};
	}

	private static List<Slot> aSlots(List<SlotComando> comandos) {
		return comandos.stream().map(DisfrazService::aSlot).toList();
	}

	private static Slot aSlot(SlotComando c) {
		if (c.ejePrenda() == EjeDePrenda.FIJA) {
			return Slot.conPrendaFija(c.orden(), c.nombre(), c.prendaFijaId(), c.opcional());
		}
		// Personalizable: opciones explícitas (preferido) o, por compatibilidad, un pool.
		if (!c.prendasOpcion().isEmpty()) {
			return Slot.personalizableConOpciones(c.orden(), c.nombre(), c.prendasOpcion(), c.opcional());
		}
		PoolComando pool = c.pool();
		if (pool == null) {
			throw new IllegalArgumentException(
					"Un slot personalizable requiere opciones de prenda o un pool");
		}
		return Slot.personalizable(c.orden(), c.nombre(),
				PoolDeSlot.de(pool.categoriaId(), pool.etiquetasPermitidas()), c.opcional());
	}
}
