package com.costumi.backend.clientes.adaptadores.salida;

import com.costumi.backend.clientes.dominio.FiltroDeClientes;
import com.costumi.backend.clientes.dominio.HistorialItem;
import com.costumi.backend.clientes.dominio.HistorialReadRepository;
import com.costumi.backend.clientes.dominio.LineaDeHistorial;
import com.costumi.backend.compartido.CodigoDeRetiro;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Adaptador de lectura del historial de clientes (RF-7.2) con JdbcClient sobre {@code renta}/{@code venta}
 * y sus líneas (con nombre y foto de la prenda), para que "Mis Pedidos" y el detalle muestren QUÉ se
 * rentó/compró. Es un modelo de lectura sobre el esquema compartido; no depende de otros módulos por código.
 */
@Repository
class HistorialJdbcAdapter implements HistorialReadRepository {

	private static final String HISTORIAL = """
			select 'RENTA' as tipo, r.id as operacion_id, r.importe as monto, r.estado, r.fecha_retiro as fecha,
			       e.id as empresa_id, e.nombre as empresa_nombre
			from renta r join empresa e on e.id = r.empresa_id
			where r.empresa_id = :empresaId and r.cliente_id = :clienteId
			union all
			select 'VENTA' as tipo, v.id as operacion_id, v.total as monto, v.estado, null::date as fecha,
			       e.id as empresa_id, e.nombre as empresa_nombre
			from venta v join empresa e on e.id = v.empresa_id
			where v.empresa_id = :empresaId and v.cliente_id = :clienteId
			order by fecha desc nulls last
			""";

	private static final String LINEAS_RENTA = """
			select rl.renta_id as operacion_id, rl.prenda_id, p.nombre, p.foto_url, rl.cantidad,
			       rl.precio_por_dia as precio_unitario
			from renta_linea rl
			join renta r on r.id = rl.renta_id
			join prenda p on p.id = rl.prenda_id
			where r.empresa_id = :empresaId and r.cliente_id = :clienteId
			""";

	private static final String LINEAS_VENTA = """
			select lv.venta_id as operacion_id, lv.prenda_id, p.nombre, p.foto_url, lv.cantidad,
			       lv.precio_unitario
			from linea_de_venta lv
			join venta v on v.id = lv.venta_id
			join prenda p on p.id = lv.prenda_id
			where v.empresa_id = :empresaId and v.cliente_id = :clienteId
			""";

	private final JdbcClient jdbc;

	HistorialJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Fila de línea con el id de su operación (renta o venta), para agrupar en memoria. */
	private record LineaConOperacion(UUID operacionId, UUID prendaId, String nombre, String fotoUrl, int cantidad,
			BigDecimal precioUnitario) {
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
		Map<UUID, List<LineaDeHistorial>> lineasPorOperacion = Stream.concat(rentaLineas.stream(), ventaLineas.stream())
				.collect(Collectors.groupingBy(LineaConOperacion::operacionId,
						Collectors.mapping(l -> new LineaDeHistorial(l.prendaId(), l.nombre(), l.fotoUrl(),
								l.cantidad(), l.precioUnitario()), Collectors.toList())));

		return jdbc.sql(HISTORIAL).param("empresaId", empresaId).param("clienteId", clienteId)
				.query((rs, rowNum) -> {
					UUID operacionId = rs.getObject("operacion_id", UUID.class);
					String tipo = rs.getString("tipo");
					String codigoRetiro = CodigoDeRetiro.de("RENTA".equals(tipo) ? "R" : "V", operacionId);
					return new HistorialItem(tipo, operacionId, codigoRetiro, rs.getBigDecimal("monto"),
							rs.getString("estado"), rs.getObject("fecha", LocalDate.class),
							rs.getObject("empresa_id", UUID.class), rs.getString("empresa_nombre"),
							lineasPorOperacion.getOrDefault(operacionId, List.of()));
				})
				.list();
	}

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
}
