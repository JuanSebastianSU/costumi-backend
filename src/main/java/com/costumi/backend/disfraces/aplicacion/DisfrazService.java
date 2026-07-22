package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.ResolucionDeDisfraces;
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
		RentarVariosDisfraces, VenderVariosDisfraces, ResolucionDeDisfraces, AsignarFotoDeDisfraz {

	private final DisfrazRepository disfraces;
	private final ConsultaDeInventario inventario;
	private final ConsultaDeTaxonomia taxonomia;
	private final com.costumi.backend.disfraces.dominio.CategoriaDeDisfrazRepository categoriasDeDisfraz;
	private final RegistroDeRentas rentas;
	private final RegistroDeVentas ventas;
	private final AlmacenDeImagenesPublico almacenDeImagenes;

	DisfrazService(DisfrazRepository disfraces, ConsultaDeInventario inventario, ConsultaDeTaxonomia taxonomia,
			com.costumi.backend.disfraces.dominio.CategoriaDeDisfrazRepository categoriasDeDisfraz,
			RegistroDeRentas rentas, RegistroDeVentas ventas, AlmacenDeImagenesPublico almacenDeImagenes) {
		this.disfraces = disfraces;
		this.inventario = inventario;
		this.taxonomia = taxonomia;
		this.categoriasDeDisfraz = categoriasDeDisfraz;
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
		validarCategoriaDelTenant(comando.empresaId(), comando.categoriaId());
		comando.slots().forEach(slot -> validarSlotDelTenant(comando.empresaId(), slot));
		Disfraz disfraz = Disfraz.crear(comando.empresaId(), comando.nombre(), comando.categoriaId(),
				aSlots(comando.slots()), comando.precioRentaGeneral(), comando.precioVentaGeneral(), comando.tipo());
		return disfraces.guardar(disfraz);
	}

	@Override
	@Transactional
	public Disfraz ejecutar(EditarDisfrazComando comando) {
		Disfraz disfraz = disfraces.buscarPorId(comando.disfrazId())
				.filter(d -> d.empresaId().equals(comando.empresaId()))
				.orElseThrow(() -> new DisfrazNoEncontrado(comando.disfrazId()));
		validarCategoriaDelTenant(comando.empresaId(), comando.categoriaId());
		comando.slots().forEach(slot -> validarSlotDelTenant(comando.empresaId(), slot));
		disfraz.redefinir(comando.nombre(), comando.categoriaId(), aSlots(comando.slots()),
				comando.precioRentaGeneral(), comando.precioVentaGeneral(), comando.tipo());
		return disfraces.guardar(disfraz);
	}

	/**
	 * La categoría del disfraz, si se indica, debe existir en la empresa. Se valida contra la taxonomía
	 * PROPIA de disfraces (categoria_disfraz), NO contra las categorías de prenda (§5.4, cross-ref por tenant).
	 */
	private void validarCategoriaDelTenant(UUID empresaId, UUID categoriaId) {
		if (categoriaId == null) {
			return;
		}
		boolean existe = categoriasDeDisfraz.buscarPorId(categoriaId)
				.filter(c -> c.empresaId().equals(empresaId))
				.isPresent();
		if (!existe) {
			throw new IllegalArgumentException("La categoría del disfraz no existe en esta empresa");
		}
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
	public com.costumi.backend.compartido.Pagina<Disfraz> deEmpresa(UUID empresaId, String buscar, UUID categoriaId, com.costumi.backend.compartido.SolicitudDePagina pagina) {
		return disfraces.listarPorEmpresa(empresaId, buscar, categoriaId, pagina);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Disfraz> activosDeEmpresa(UUID empresaId) {
		return disfraces.listarPorEmpresa(empresaId).stream().filter(Disfraz::activo).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ConsultarDisfraces.Sugeridos sugeridosDe(UUID empresaId, Disfraz disfraz) {
		return calcularSugeridos(disfraz, inventario.prendasValuadasDeEmpresa(empresaId));
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, ConsultarDisfraces.Sugeridos> sugeridosDe(UUID empresaId, List<Disfraz> disfraces) {
		// Catálogo de la empresa UNA sola vez: los sugeridos de todos los disfraces se calculan en memoria.
		List<ConsultaDeInventario.PrendaValuada> catalogo = inventario.prendasValuadasDeEmpresa(empresaId);
		Map<UUID, ConsultarDisfraces.Sugeridos> resultado = new java.util.LinkedHashMap<>();
		for (Disfraz disfraz : disfraces) {
			resultado.put(disfraz.id(), calcularSugeridos(disfraz, catalogo));
		}
		return resultado;
	}

	/**
	 * Suma, slot por slot, los precios/valores mínimos y máximos de sus opciones elegibles resueltas sobre el
	 * {@code catalogo} ya cargado (sin más consultas). Slot fijo → su prenda; personalizable → sus opciones
	 * explícitas o las del pool (categoría + etiquetas), filtradas en memoria.
	 */
	private static ConsultarDisfraces.Sugeridos calcularSugeridos(Disfraz disfraz,
			List<ConsultaDeInventario.PrendaValuada> catalogo) {
		Map<UUID, ConsultaDeInventario.PrendaValuada> porId = new java.util.HashMap<>();
		for (ConsultaDeInventario.PrendaValuada p : catalogo) {
			porId.put(p.prendaId(), p);
		}
		BigDecimal rentaMin = BigDecimal.ZERO;
		BigDecimal rentaMax = BigDecimal.ZERO;
		BigDecimal ventaMin = BigDecimal.ZERO;
		BigDecimal ventaMax = BigDecimal.ZERO;
		BigDecimal danoMin = BigDecimal.ZERO;
		BigDecimal danoMax = BigDecimal.ZERO;
		BigDecimal reposicionMin = BigDecimal.ZERO;
		BigDecimal reposicionMax = BigDecimal.ZERO;
		for (Slot slot : disfraz.slots()) {
			List<ConsultaDeInventario.PrendaValuada> opciones = opcionesValuadas(slot, catalogo, porId);
			rentaMin = rentaMin.add(extremo(opciones, ConsultaDeInventario.PrendaValuada::precioRenta, false));
			rentaMax = rentaMax.add(extremo(opciones, ConsultaDeInventario.PrendaValuada::precioRenta, true));
			ventaMin = ventaMin.add(extremo(opciones, ConsultaDeInventario.PrendaValuada::precioVenta, false));
			ventaMax = ventaMax.add(extremo(opciones, ConsultaDeInventario.PrendaValuada::precioVenta, true));
			danoMin = danoMin.add(extremo(opciones, ConsultaDeInventario.PrendaValuada::valorDano, false));
			danoMax = danoMax.add(extremo(opciones, ConsultaDeInventario.PrendaValuada::valorDano, true));
			reposicionMin = reposicionMin.add(extremo(opciones, ConsultaDeInventario.PrendaValuada::valorReposicion, false));
			reposicionMax = reposicionMax.add(extremo(opciones, ConsultaDeInventario.PrendaValuada::valorReposicion, true));
		}
		return new ConsultarDisfraces.Sugeridos(rentaMin, rentaMax, ventaMin, ventaMax,
				new ConsultarDisfraces.MultaSugerida(danoMin, danoMax, reposicionMin, reposicionMax));
	}

	/** Opciones elegibles de un slot resueltas sobre el catálogo: la fija, las explícitas o las del pool. */
	private static List<ConsultaDeInventario.PrendaValuada> opcionesValuadas(Slot slot,
			List<ConsultaDeInventario.PrendaValuada> catalogo,
			Map<UUID, ConsultaDeInventario.PrendaValuada> porId) {
		if (slot.ejePrenda() == EjeDePrenda.FIJA) {
			ConsultaDeInventario.PrendaValuada fija = porId.get(slot.prendaFijaId());
			return fija == null ? List.of() : List.of(fija);
		}
		if (slot.tieneOpcionesExplicitas()) {
			return slot.prendasOpcion().stream().map(porId::get)
					.filter(java.util.Objects::nonNull).toList();
		}
		PoolDeSlot pool = slot.pool();
		return catalogo.stream()
				.filter(p -> pool.categoriaId().equals(p.categoriaId()))
				.filter(p -> cumpleEtiquetas(p.etiquetas(), pool.etiquetasPermitidas()))
				.toList();
	}

	/** El extremo (máx si {@code max}; si no, mín) de un valor entre las opciones; 0 si no hay ninguno. */
	private static BigDecimal extremo(List<ConsultaDeInventario.PrendaValuada> opciones,
			java.util.function.Function<ConsultaDeInventario.PrendaValuada, BigDecimal> valor, boolean max) {
		java.util.stream.Stream<BigDecimal> valores = opciones.stream().map(valor)
				.filter(java.util.Objects::nonNull);
		return (max ? valores.max(BigDecimal::compareTo) : valores.min(BigDecimal::compareTo))
				.orElse(BigDecimal.ZERO);
	}

	/** ¿Los valores de etiqueta de la prenda satisfacen los permitidos del pool por dimensión? (vacío = cualquiera). */
	private static boolean cumpleEtiquetas(Map<UUID, UUID> valoresDeLaPrenda, Map<UUID, Set<UUID>> permitidas) {
		for (Map.Entry<UUID, Set<UUID>> exigencia : permitidas.entrySet()) {
			UUID valor = valoresDeLaPrenda.get(exigencia.getKey());
			if (valor == null || !exigencia.getValue().contains(valor)) {
				return false;
			}
		}
		return true;
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

	@Override
	@Transactional(readOnly = true)
	public List<LineaResuelta> lineasDeRenta(UUID empresaId, UUID disfrazId, int cantidad,
			List<SeleccionDeSlot> selecciones) {
		return itemsRentaDe(empresaId, disfrazId, Math.max(1, cantidad), seleccionesResueltas(selecciones)).stream()
				.map(it -> new LineaResuelta(it.prendaId(), it.cantidad(), it.precioPorDia()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<LineaResuelta> lineasDeVenta(UUID empresaId, UUID disfrazId, int cantidad,
			List<SeleccionDeSlot> selecciones) {
		return itemsVentaDe(empresaId, disfrazId, Math.max(1, cantidad), seleccionesResueltas(selecciones)).stream()
				.map(it -> new LineaResuelta(it.prendaId(), it.cantidad(), it.precioUnitario()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, ResumenDeDisfraz> resumenDeDisfraces(UUID empresaId, java.util.Collection<UUID> disfrazIds) {
		Map<UUID, ResumenDeDisfraz> resumen = new java.util.LinkedHashMap<>();
		for (UUID disfrazId : disfrazIds) {
			if (disfrazId == null || resumen.containsKey(disfrazId)) {
				continue;
			}
			disfraces.buscarPorId(disfrazId)
					.filter(d -> d.empresaId().equals(empresaId))
					.ifPresent(d -> resumen.put(d.id(), new ResumenDeDisfraz(d.id(), d.nombre(), d.fotoUrl(),
							d.permiteRenta(), d.permiteVenta())));
		}
		return resumen;
	}

	private static Map<Integer, UUID> seleccionesResueltas(List<SeleccionDeSlot> selecciones) {
		Map<Integer, UUID> m = new HashMap<>();
		if (selecciones != null) {
			for (SeleccionDeSlot s : selecciones) {
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
		// El dueño decide para qué está disponible el disfraz (RF-2.3): si es solo de venta, no se renta.
		if (!disfraz.permiteRenta()) {
			throw new IllegalArgumentException("Este disfraz es solo para venta, no se puede rentar");
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
		// Cada pieza recuerda de qué disfraz salió (ver itemsVentaDe).
		UUID grupo = UUID.randomUUID();
		return items.stream()
				.map(i -> new RegistroDeRentas.ItemDeRenta(i.prendaId(), i.cantidad(), i.precioPorDia(),
						disfrazId, grupo, cantidad, disfraz.nombre()))
				.toList();
	}

	/** Resuelve un disfraz a sus líneas de venta (una por pieza), con la cantidad y el reparto de precio general. */
	private List<RegistroDeVentas.ItemDeVenta> itemsVentaDe(UUID empresaId, UUID disfrazId, int cantidad,
			Map<Integer, UUID> seleccionPorOrden) {
		Disfraz disfraz = exigirDelTenant(empresaId, disfrazId);
		if (!disfraz.activo()) {
			throw new IllegalArgumentException("El disfraz está archivado y no se puede vender");
		}
		// El dueño decide para qué está disponible el disfraz (RF-2.3): si es solo de renta, no se vende.
		if (!disfraz.permiteVenta()) {
			throw new IllegalArgumentException("Este disfraz es solo para renta, no se puede comprar");
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
		// Precio de venta general (RF-2.10) × cantidad: reparte el precio del conjunto entre las piezas.
		if (disfraz.tienePrecioVentaGeneral()) {
			items = repartirVenta(items, disfraz.precioVentaGeneral().multiply(BigDecimal.valueOf(cantidad)));
		}
		// Cada pieza recuerda de qué disfraz salió: si no, al cobrar el disfraz se pierde y el pedido
		// queda como un montón de prendas sueltas. El grupo distingue esta instancia de otra del mismo
		// disfraz en el mismo pedido (p. ej. con piezas distintas).
		UUID grupo = UUID.randomUUID();
		return items.stream()
				.map(i -> new RegistroDeVentas.ItemDeVenta(i.prendaId(), i.cantidad(), i.precioUnitario(),
						disfrazId, grupo, cantidad, disfraz.nombre()))
				.toList();
	}

	/**
	 * Reparte el {@code precioGeneral} de venta entre las líneas proporcionalmente a su precio, de modo que
	 * la suma iguale exactamente el precio del conjunto; la última línea absorbe el redondeo (idéntico al
	 * reparto de renta, pero sobre líneas de venta).
	 */
	private static List<RegistroDeVentas.ItemDeVenta> repartirVenta(List<RegistroDeVentas.ItemDeVenta> items,
			BigDecimal precioGeneral) {
		BigDecimal base = items.stream()
				.map(it -> it.precioUnitario().multiply(BigDecimal.valueOf(it.cantidad())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		List<RegistroDeVentas.ItemDeVenta> resultado = new ArrayList<>();
		BigDecimal acumulado = BigDecimal.ZERO;
		for (int i = 0; i < items.size(); i++) {
			RegistroDeVentas.ItemDeVenta it = items.get(i);
			BigDecimal cantidad = BigDecimal.valueOf(it.cantidad());
			BigDecimal precioUnitario;
			if (i == items.size() - 1) {
				precioUnitario = precioGeneral.subtract(acumulado).divide(cantidad, 2, RoundingMode.HALF_UP);
			} else if (base.signum() == 0) {
				precioUnitario = BigDecimal.ZERO;
			} else {
				BigDecimal lineaBase = it.precioUnitario().multiply(cantidad);
				BigDecimal lineaTotal = precioGeneral.multiply(lineaBase).divide(base, 2, RoundingMode.HALF_UP);
				precioUnitario = lineaTotal.divide(cantidad, 2, RoundingMode.HALF_UP);
				acumulado = acumulado.add(precioUnitario.multiply(cantidad));
			}
			resultado.add(new RegistroDeVentas.ItemDeVenta(it.prendaId(), it.cantidad(), precioUnitario));
		}
		return resultado;
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
