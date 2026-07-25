package com.costumi.backend.clientes.adaptadores.salida;

import com.costumi.backend.clientes.dominio.CargaDeCliente;
import com.costumi.backend.clientes.dominio.DeudaEnTienda;
import com.costumi.backend.clientes.dominio.FiltroDeClientes;
import com.costumi.backend.clientes.dominio.FiltroDeHistorial;
import com.costumi.backend.clientes.dominio.HistorialItem;
import com.costumi.backend.clientes.dominio.HistorialReadRepository;
import com.costumi.backend.clientes.dominio.LineaDeEstadoDeCuenta;
import com.costumi.backend.clientes.dominio.LineaDeHistorial;
import com.costumi.backend.compartido.CodigoDeRetiro;
import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Adaptador de lectura del historial de clientes (RF-7.2) con JdbcClient sobre {@code renta}/{@code venta}
 * y sus líneas (con nombre y foto de la prenda), para que "Mis Pedidos" y el detalle muestren QUÉ se
 * rentó/compró y CUÁNTO falta pagar. Es un modelo de lectura sobre el esquema compartido; no depende de
 * otros módulos por código.
 */
@Repository
class HistorialJdbcAdapter implements HistorialReadRepository {

	// --- Fragmentos de saldo reutilizados (RF-11.5/11.6), consistentes con Devoluciones y Pagos ---
	// Multa de una renta = Σ max(0, daños + retraso − depósito) de sus devoluciones (Devolucion.multa()).
	private static final String MULTA_DE_RENTA = """
			coalesce((select sum(greatest(d.cargo_por_danos + d.cargo_por_retraso - d.deposito, 0))
			          from devolucion d where d.renta_id = r.id and d.empresa_id = r.empresa_id), 0)""";
	// Pagos netos de ingreso de la renta = COBRO (+) − REEMBOLSO (−); depósito y su devolución no cuentan
	// (Pago.montoNeto()).
	private static final String PAGOS_NETOS_DE_RENTA = """
			coalesce((select sum(case p.tipo_pago when 'COBRO' then p.monto when 'REEMBOLSO' then -p.monto else 0 end)
			          from pago p where p.empresa_id = r.empresa_id and p.tipo_concepto = 'RENTA'
			                        and p.concepto_id = r.id), 0)""";
	// Análogo para VENTA: mismo cruce COBRO/REEMBOLSO sobre la tabla pago (tipo_concepto='VENTA').
	private static final String PAGOS_NETOS_DE_VENTA = """
			coalesce((select sum(case p.tipo_pago when 'COBRO' then p.monto when 'REEMBOLSO' then -p.monto else 0 end)
			          from pago p where p.empresa_id = v.empresa_id and p.tipo_concepto = 'VENTA'
			                        and p.concepto_id = v.id), 0)""";

	// Saldo por operación: lo que aún debe. Renta: importe + multa − cobrado (mientras no esté cerrada ni
	// cancelada); venta: total − cobrado (mientras siga confirmada). Devuelta/cerrada/cancelada → 0.
	private static final String SALDO_RENTA = "case when r.estado in ('RESERVADA','ACTIVA','DEVUELTA') "
			+ "then greatest(r.importe + " + MULTA_DE_RENTA + " - " + PAGOS_NETOS_DE_RENTA + ", 0) else 0 end";
	private static final String SALDO_VENTA = "case when v.estado = 'CONFIRMADA' "
			+ "then greatest(v.total - " + PAGOS_NETOS_DE_VENTA + ", 0) else 0 end";
	// Categoría para las pestañas de "Mis Pedidos".
	private static final String CATEGORIA_RENTA = "case r.estado when 'RESERVADA' then 'POR_RETIRAR' "
			+ "when 'ACTIVA' then 'ACTIVO' when 'DEVUELTA' then 'ACTIVO' else 'CERRADO' end";
	private static final String CATEGORIA_VENTA = "case v.estado when 'CONFIRMADA' then 'ACTIVO' "
			+ "when 'PARCIALMENTE_DEVUELTA' then 'ACTIVO' else 'CERRADO' end";

	// Etiqueta de pago derivada del saldo y lo cobrado (se calcula fuera del union, sobre el alias t).
	private static final String ESTADO_PAGO_COL =
			"case when t.saldo <= 0 then 'PAGADO' when t.cobrado > 0 then 'PARCIAL' else 'PENDIENTE' end as estado_pago";

	// Recencia («más reciente primero»): la fecha del ítem y el orden salen de cuándo se REGISTRÓ la
	// operación (creada_en), igual para renta y venta.
	private static final String HISTORIAL_POR_CLIENTE = wrapper(ramas(false));
	private static final String HISTORIAL_DE_USUARIO = ramas(true);

	private static final String LINEAS_RENTA = """
			select rl.renta_id as operacion_id, rl.prenda_id, p.nombre, p.foto_url, rl.cantidad,
			       rl.precio_por_dia as precio_unitario,
			       rl.disfraz_id, rl.disfraz_nombre, rl.disfraz_grupo, rl.disfraz_cantidad
			from renta_linea rl
			join renta r on r.id = rl.renta_id
			join prenda p on p.id = rl.prenda_id
			where r.empresa_id = :empresaId and r.cliente_id = :clienteId
			""";

	private static final String LINEAS_VENTA = """
			select lv.venta_id as operacion_id, lv.prenda_id, p.nombre, p.foto_url, lv.cantidad,
			       lv.precio_unitario,
			       lv.disfraz_id, lv.disfraz_nombre, lv.disfraz_grupo, lv.disfraz_cantidad
			from linea_de_venta lv
			join venta v on v.id = lv.venta_id
			join prenda p on p.id = lv.prenda_id
			where v.empresa_id = :empresaId and v.cliente_id = :clienteId
			""";

	// Líneas por lote (para el historial paginado / detalle por usuario, que cruza tiendas): por ids de
	// operación. Los ids son globalmente únicos y ya vienen acotados a las operaciones del propio usuario.
	private static final String LINEAS_RENTA_POR_IDS = """
			select rl.renta_id as operacion_id, rl.prenda_id, p.nombre, p.foto_url, rl.cantidad,
			       rl.precio_por_dia as precio_unitario,
			       rl.disfraz_id, rl.disfraz_nombre, rl.disfraz_grupo, rl.disfraz_cantidad
			from renta_linea rl
			join prenda p on p.id = rl.prenda_id
			where rl.renta_id in (:ids)
			""";

	private static final String LINEAS_VENTA_POR_IDS = """
			select lv.venta_id as operacion_id, lv.prenda_id, p.nombre, p.foto_url, lv.cantidad,
			       lv.precio_unitario,
			       lv.disfraz_id, lv.disfraz_nombre, lv.disfraz_grupo, lv.disfraz_cantidad
			from linea_de_venta lv
			join prenda p on p.id = lv.prenda_id
			where lv.venta_id in (:ids)
			""";

	private final JdbcClient jdbc;

	HistorialJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Fila de línea con el id de su operación (renta o venta), para agrupar en memoria. */
	private record LineaConOperacion(UUID operacionId, UUID prendaId, String nombre, String fotoUrl, int cantidad,
			BigDecimal precioUnitario, UUID disfrazId, String disfrazNombre, UUID disfrazGrupo,
			Integer disfrazCantidad) {
	}

	/**
	 * Las dos ramas (renta ∪ venta) del historial con las columnas calculadas (cobrado, saldo, categoría).
	 * {@code porUsuario=true} cruza las fichas del usuario ({@code cliente.usuario_id}); {@code false} acota
	 * a un cliente de una empresa. Ambas ramas devuelven exactamente las mismas columnas (obligatorio para el
	 * {@code union all}).
	 */
	private static String ramas(boolean porUsuario) {
		String joinR = porUsuario ? "join cliente cl on cl.id = r.cliente_id and cl.empresa_id = r.empresa_id " : "";
		String whereR = porUsuario ? "cl.usuario_id = :usuarioId"
				: "r.empresa_id = :empresaId and r.cliente_id = :clienteId";
		String joinV = porUsuario ? "join cliente cl on cl.id = v.cliente_id and cl.empresa_id = v.empresa_id " : "";
		String whereV = porUsuario ? "cl.usuario_id = :usuarioId"
				: "v.empresa_id = :empresaId and v.cliente_id = :clienteId";
		return "select 'RENTA' as tipo, r.id as operacion_id, r.importe as monto, r.estado, "
				+ PAGOS_NETOS_DE_RENTA + " as cobrado, " + SALDO_RENTA + " as saldo, " + CATEGORIA_RENTA
				+ " as categoria, r.creada_en::date as fecha, r.creada_en as registrada_en, "
				+ "e.id as empresa_id, e.nombre as empresa_nombre "
				+ "from renta r " + joinR + "join empresa e on e.id = r.empresa_id where " + whereR
				+ " union all "
				+ "select 'VENTA' as tipo, v.id as operacion_id, v.total as monto, v.estado, "
				+ PAGOS_NETOS_DE_VENTA + " as cobrado, " + SALDO_VENTA + " as saldo, " + CATEGORIA_VENTA
				+ " as categoria, v.creada_en::date as fecha, v.creada_en as registrada_en, "
				+ "e.id as empresa_id, e.nombre as empresa_nombre "
				+ "from venta v " + joinV + "join empresa e on e.id = v.empresa_id where " + whereV;
	}

	/** Envuelve las ramas para añadir la etiqueta de pago y ordenar por recencia. */
	private static String wrapper(String ramas) {
		return "select t.*, " + ESTADO_PAGO_COL + " from ( " + ramas + " ) t order by t.registrada_en desc";
	}

	/** Traduce la pestaña a su predicado sobre las columnas calculadas del historial. */
	private static String predicado(FiltroDeHistorial filtro) {
		return switch (filtro) {
			case TODOS -> "true";
			case POR_PAGAR -> "t.saldo > 0";
			case POR_RETIRAR -> "t.categoria = 'POR_RETIRAR'";
			case ACTIVOS -> "t.categoria = 'ACTIVO'";
			case CERRADOS -> "t.categoria = 'CERRADO'";
		};
	}

	private static HistorialItem mapItem(ResultSet rs, Map<UUID, List<LineaDeHistorial>> lineas) throws SQLException {
		UUID operacionId = rs.getObject("operacion_id", UUID.class);
		String tipo = rs.getString("tipo");
		String codigoRetiro = CodigoDeRetiro.de("RENTA".equals(tipo) ? "R" : "V", operacionId);
		return new HistorialItem(tipo, operacionId, codigoRetiro, rs.getBigDecimal("monto"), rs.getString("estado"),
				rs.getBigDecimal("saldo"), rs.getString("estado_pago"), rs.getObject("fecha", LocalDate.class),
				rs.getObject("empresa_id", UUID.class), rs.getString("empresa_nombre"),
				lineas.getOrDefault(operacionId, List.of()));
	}

	private static HistorialItem conLineas(HistorialItem i, List<LineaDeHistorial> lineas) {
		return new HistorialItem(i.tipo(), i.operacionId(), i.codigoRetiro(), i.monto(), i.estado(),
				i.saldoPendiente(), i.estadoPago(), i.fecha(), i.empresaId(), i.empresaNombre(), lineas);
	}

	@Override
	public List<HistorialItem> deCliente(UUID empresaId, UUID clienteId) {
		// Dos consultas (rentas y ventas) para todas las líneas del cliente; se agrupan por operación en memoria.
		List<LineaConOperacion> rentaLineas = jdbc.sql(LINEAS_RENTA)
				.param("empresaId", empresaId).param("clienteId", clienteId)
				.query(LineaConOperacion.class).list();
		List<LineaConOperacion> ventaLineas = jdbc.sql(LINEAS_VENTA)
				.param("empresaId", empresaId).param("clienteId", clienteId)
				.query(LineaConOperacion.class).list();
		Map<UUID, List<LineaDeHistorial>> lineasPorOperacion = agrupar(Stream.concat(rentaLineas.stream(),
				ventaLineas.stream()));

		return jdbc.sql(HISTORIAL_POR_CLIENTE).param("empresaId", empresaId).param("clienteId", clienteId)
				.query((rs, rowNum) -> mapItem(rs, lineasPorOperacion))
				.list();
	}

	@Override
	public Pagina<HistorialItem> historialDeUsuario(UUID usuarioId, FiltroDeHistorial filtro,
			SolicitudDePagina solicitud) {
		String pred = predicado(filtro);
		Long total = jdbc.sql("select count(*) from ( " + HISTORIAL_DE_USUARIO + " ) t where " + pred)
				.param("usuarioId", usuarioId).query(Long.class).single();
		if (total == null || total == 0) {
			return new Pagina<>(List.of(), 0, solicitud.pagina(), solicitud.tamano());
		}
		String sql = "select t.*, " + ESTADO_PAGO_COL + " from ( " + HISTORIAL_DE_USUARIO + " ) t where " + pred
				+ " order by t.registrada_en desc limit :tamano offset :offset";
		List<HistorialItem> pagina = jdbc.sql(sql).param("usuarioId", usuarioId)
				.param("tamano", solicitud.tamano())
				.param("offset", (long) solicitud.pagina() * solicitud.tamano())
				.query((rs, rowNum) -> mapItem(rs, Map.of()))
				.list();
		Map<UUID, List<LineaDeHistorial>> lineas = lineasPorIds(idsPorTipo(pagina, "RENTA"), idsPorTipo(pagina, "VENTA"));
		List<HistorialItem> conLineas = pagina.stream()
				.map(i -> conLineas(i, lineas.getOrDefault(i.operacionId(), List.of())))
				.toList();
		return new Pagina<>(conLineas, total, solicitud.pagina(), solicitud.tamano());
	}

	@Override
	public Optional<HistorialItem> operacionDeUsuario(UUID usuarioId, UUID operacionId) {
		String sql = "select t.*, " + ESTADO_PAGO_COL + " from ( " + HISTORIAL_DE_USUARIO
				+ " ) t where t.operacion_id = :operacionId";
		List<HistorialItem> encontrada = jdbc.sql(sql).param("usuarioId", usuarioId).param("operacionId", operacionId)
				.query((rs, rowNum) -> mapItem(rs, Map.of()))
				.list();
		if (encontrada.isEmpty()) {
			return Optional.empty();
		}
		HistorialItem item = encontrada.get(0);
		Map<UUID, List<LineaDeHistorial>> lineas = lineasPorIds(
				"RENTA".equals(item.tipo()) ? List.of(operacionId) : List.of(),
				"VENTA".equals(item.tipo()) ? List.of(operacionId) : List.of());
		return Optional.of(conLineas(item, lineas.getOrDefault(operacionId, List.of())));
	}

	private static List<UUID> idsPorTipo(List<HistorialItem> items, String tipo) {
		return items.stream().filter(i -> tipo.equals(i.tipo())).map(HistorialItem::operacionId).toList();
	}

	/** Líneas de las operaciones dadas (rentas y ventas), agrupadas por id de operación; evita el N+1. */
	private Map<UUID, List<LineaDeHistorial>> lineasPorIds(List<UUID> rentaIds, List<UUID> ventaIds) {
		Stream<LineaConOperacion> rentas = rentaIds.isEmpty() ? Stream.of()
				: jdbc.sql(LINEAS_RENTA_POR_IDS).param("ids", rentaIds).query(LineaConOperacion.class).list().stream();
		Stream<LineaConOperacion> ventas = ventaIds.isEmpty() ? Stream.of()
				: jdbc.sql(LINEAS_VENTA_POR_IDS).param("ids", ventaIds).query(LineaConOperacion.class).list().stream();
		return agrupar(Stream.concat(rentas, ventas));
	}

	private static Map<UUID, List<LineaDeHistorial>> agrupar(Stream<LineaConOperacion> lineas) {
		return lineas.collect(Collectors.groupingBy(LineaConOperacion::operacionId,
				Collectors.mapping(l -> new LineaDeHistorial(l.prendaId(), l.nombre(), l.fotoUrl(), l.cantidad(),
						l.precioUnitario(), l.disfrazId(), l.disfrazNombre(), l.disfrazGrupo(), l.disfrazCantidad()),
						Collectors.toList())));
	}

	// SALDOS: rentas activas o devueltas donde lo pagado no cubre importe + multa (aún debe dinero).
	private static final String SALDOS = """
			select distinct r.cliente_id
			from renta r
			where r.empresa_id = :empresaId and r.estado in ('ACTIVA', 'DEVUELTA')
			  and r.importe + %s - %s > 0
			""".formatted(MULTA_DE_RENTA, PAGOS_NETOS_DE_RENTA);

	// MULTAS: incurrió en alguna multa (daños/retraso que superaron el depósito), esté pagada o no.
	private static final String MULTAS = """
			select distinct r.cliente_id
			from renta r
			where r.empresa_id = :empresaId and %s > 0
			""".formatted(MULTA_DE_RENTA);

	// VENCIDAS: renta activa cuya fecha de devolución ya pasó.
	private static final String VENCIDAS = """
			select distinct cliente_id from renta
			where empresa_id = :empresaId and estado = 'ACTIVA' and fecha_devolucion < :hoy
			""";

	// PENDIENTES (indicador general): rentas activas por devolver ∪ saldos por cobrar.
	private static final String PENDIENTES = """
			select distinct cliente_id from renta where empresa_id = :empresaId and estado = 'ACTIVA'
			union
			""" + SALDOS;

	@Override
	public List<UUID> clientesPorFiltro(UUID empresaId, FiltroDeClientes filtro, LocalDate hoy) {
		String sql = switch (filtro) {
			case PENDIENTES -> PENDIENTES;
			case VENCIDAS -> VENCIDAS;
			case MULTAS -> MULTAS;
			case SALDOS -> SALDOS;
		};
		JdbcClient.StatementSpec spec = jdbc.sql(sql).param("empresaId", empresaId);
		if (filtro == FiltroDeClientes.VENCIDAS) {
			spec = spec.param("hoy", hoy);
		}
		return spec.query(UUID.class).list();
	}

	// Saldo + multa por cliente: sobre sus rentas de la empresa, reusando los mismos fragmentos que el filtro.
	private static final String CARGA = """
			select r.cliente_id,
			       coalesce(sum(case when r.estado in ('ACTIVA','DEVUELTA')
			                         then greatest(r.importe + %1$s - %2$s, 0) else 0 end), 0) as saldo_pendiente,
			       coalesce(sum(%1$s), 0) as multa_total,
			       coalesce(bool_or(r.estado in ('RESERVADA','ACTIVA')), false) as tiene_renta_en_curso
			from renta r
			where r.empresa_id = :empresaId and r.cliente_id in (:ids)
			group by r.cliente_id
			""".formatted(MULTA_DE_RENTA, PAGOS_NETOS_DE_RENTA);

	// Estado de cuenta: por cada renta del cliente, sus componentes (reusando los mismos fragmentos que CARGA
	// para que la suma de las líneas cuadre exactamente con el saldo/multa agregados).
	private static final String ESTADO_DE_CUENTA = """
			select r.id as renta_id, r.estado, r.fecha_retiro, r.fecha_devolucion, r.importe,
			       coalesce((select sum(d.cargo_por_danos) from devolucion d
			                 where d.renta_id = r.id and d.empresa_id = r.empresa_id), 0) as danos,
			       coalesce((select sum(d.cargo_por_retraso) from devolucion d
			                 where d.renta_id = r.id and d.empresa_id = r.empresa_id), 0) as retraso,
			       coalesce((select sum(d.deposito) from devolucion d
			                 where d.renta_id = r.id and d.empresa_id = r.empresa_id), 0) as deposito,
			       %1$s as multa,
			       %2$s as pagado,
			       case when r.estado in ('ACTIVA','DEVUELTA') then greatest(r.importe + %1$s - %2$s, 0)
			            else 0 end as saldo
			from renta r
			where r.empresa_id = :empresaId and r.cliente_id = :clienteId
			order by r.fecha_retiro desc nulls last
			""".formatted(MULTA_DE_RENTA, PAGOS_NETOS_DE_RENTA);

	@Override
	public List<LineaDeEstadoDeCuenta> estadoDeCuenta(UUID empresaId, UUID clienteId) {
		return jdbc.sql(ESTADO_DE_CUENTA).param("empresaId", empresaId).param("clienteId", clienteId)
				.query((rs, rowNum) -> {
					UUID rentaId = rs.getObject("renta_id", UUID.class);
					return new LineaDeEstadoDeCuenta(rentaId, CodigoDeRetiro.de("R", rentaId), rs.getString("estado"),
							rs.getObject("fecha_retiro", LocalDate.class),
							rs.getObject("fecha_devolucion", LocalDate.class), rs.getBigDecimal("importe"),
							rs.getBigDecimal("danos"), rs.getBigDecimal("retraso"), rs.getBigDecimal("deposito"),
							rs.getBigDecimal("multa"), rs.getBigDecimal("pagado"), rs.getBigDecimal("saldo"));
				})
				.list().stream()
				.filter(l -> l.saldo().signum() > 0 || l.multa().signum() > 0)
				.toList();
	}

	/**
	 * Igual que {@link #ESTADO_DE_CUENTA} pero por USUARIO: cruza sus fichas de cliente ({@code cliente
	 * .usuario_id}) para traer lo que debe en TODAS las tiendas, con el nombre de cada una. Reusa los
	 * mismos fragmentos de multa y pagos, así que las cifras cuadran con las que ve la tienda.
	 *
	 * <p>En UNA consulta: hacerlo ficha por ficha sería un N+1 que crece con cada tienda en la que el
	 * cliente compró.
	 */
	private static final String ESTADO_DE_CUENTA_DE_USUARIO = """
			select e.id as empresa_id, e.nombre as empresa_nombre, r.sucursal_id,
			       r.id as renta_id, r.estado, r.fecha_retiro, r.fecha_devolucion, r.importe,
			       coalesce((select sum(d.cargo_por_danos) from devolucion d
			                 where d.renta_id = r.id and d.empresa_id = r.empresa_id), 0) as danos,
			       coalesce((select sum(d.cargo_por_retraso) from devolucion d
			                 where d.renta_id = r.id and d.empresa_id = r.empresa_id), 0) as retraso,
			       coalesce((select sum(d.deposito) from devolucion d
			                 where d.renta_id = r.id and d.empresa_id = r.empresa_id), 0) as deposito,
			       %1$s as multa,
			       %2$s as pagado,
			       case when r.estado in ('ACTIVA','DEVUELTA') then greatest(r.importe + %1$s - %2$s, 0)
			            else 0 end as saldo
			from renta r
			join cliente cl on cl.id = r.cliente_id and cl.empresa_id = r.empresa_id
			join empresa e on e.id = r.empresa_id
			where cl.usuario_id = :usuarioId
			order by r.fecha_retiro desc nulls last
			""".formatted(MULTA_DE_RENTA, PAGOS_NETOS_DE_RENTA);

	@Override
	public List<DeudaEnTienda> estadoDeCuentaDeUsuario(UUID usuarioId) {
		return jdbc.sql(ESTADO_DE_CUENTA_DE_USUARIO).param("usuarioId", usuarioId)
				.query((rs, rowNum) -> {
					UUID rentaId = rs.getObject("renta_id", UUID.class);
					LineaDeEstadoDeCuenta linea = new LineaDeEstadoDeCuenta(rentaId,
							CodigoDeRetiro.de("R", rentaId), rs.getString("estado"),
							rs.getObject("fecha_retiro", LocalDate.class),
							rs.getObject("fecha_devolucion", LocalDate.class), rs.getBigDecimal("importe"),
							rs.getBigDecimal("danos"), rs.getBigDecimal("retraso"), rs.getBigDecimal("deposito"),
							rs.getBigDecimal("multa"), rs.getBigDecimal("pagado"), rs.getBigDecimal("saldo"));
					return new DeudaEnTienda(rs.getObject("empresa_id", UUID.class),
							rs.getObject("sucursal_id", UUID.class), rs.getString("empresa_nombre"), linea);
				})
				.list().stream()
				// Mismo criterio que el estado de cuenta de la tienda: solo lo que debe o lo que le multaron.
				.filter(d -> d.linea().saldo().signum() > 0 || d.linea().multa().signum() > 0)
				.toList();
	}

	@Override
	public Map<UUID, CargaDeCliente> cargaDeClientes(UUID empresaId, Collection<UUID> clienteIds) {
		if (clienteIds == null || clienteIds.isEmpty()) {
			return Map.of();
		}
		Map<UUID, CargaDeCliente> out = new HashMap<>();
		jdbc.sql(CARGA).param("empresaId", empresaId).param("ids", clienteIds)
				.query((rs, rowNum) -> {
					out.put(rs.getObject("cliente_id", UUID.class),
							new CargaDeCliente(rs.getBigDecimal("saldo_pendiente"), rs.getBigDecimal("multa_total"),
							rs.getBoolean("tiene_renta_en_curso")));
					return null;
				})
				.list();
		return out;
	}
}
