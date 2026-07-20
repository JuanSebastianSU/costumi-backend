package com.costumi.backend.reportes.adaptadores.salida;

import com.costumi.backend.reportes.dominio.ArticuloRanking;
import com.costumi.backend.reportes.dominio.EmpleadoVentas;
import com.costumi.backend.reportes.dominio.RankingReadRepository;
import com.costumi.backend.reportes.dominio.ValorEtiquetaRanking;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Adaptador de lectura de rankings (RF-9.1) con JdbcClient. Filtros de sucursal/fecha opcionales. */
@Repository
class RankingJdbcAdapter implements RankingReadRepository {

	private final JdbcClient jdbc;

	RankingJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<ArticuloRanking> masVendidos(UUID empresaId, UUID sucursalId, int limite) {
		String sql = "select lv.prenda_id, p.nombre,"
				+ " sum(lv.cantidad) as unidades, coalesce(sum(lv.cantidad * lv.precio_unitario), 0) as monto"
				+ " from linea_de_venta lv"
				+ " join venta v on v.id = lv.venta_id"
				+ " join prenda p on p.id = lv.prenda_id"
				+ " where lv.empresa_id = :empresaId"
				+ (sucursalId == null ? "" : " and v.sucursal_id = :sucursalId")
				+ " group by lv.prenda_id, p.nombre order by unidades desc limit :limite";
		JdbcClient.StatementSpec spec = jdbc.sql(sql).param("empresaId", empresaId).param("limite", limite);
		if (sucursalId != null) {
			spec = spec.param("sucursalId", sucursalId);
		}
		return spec.query(ArticuloRanking.class).list();
	}

	@Override
	public List<ArticuloRanking> masRentados(UUID empresaId, UUID sucursalId, LocalDate desde, LocalDate hasta,
			int limite) {
		// Multi-artículo (RF-3.1): cuenta las unidades por línea (renta_linea), no las rentas. El monto
		// es Σ precio×cantidad×días de cada línea (mismo criterio que el importe de la renta).
		String sql = "select l.prenda_id, p.nombre, sum(l.cantidad) as unidades,"
				+ " coalesce(sum(l.cantidad * l.precio_por_dia"
				+ "   * greatest(1, (r.fecha_devolucion - r.fecha_retiro))), 0) as monto"
				+ " from renta_linea l"
				+ " join renta r on r.id = l.renta_id"
				+ " join prenda p on p.id = l.prenda_id"
				+ " where r.empresa_id = :empresaId"
				+ (sucursalId == null ? "" : " and r.sucursal_id = :sucursalId")
				+ (desde == null ? "" : " and r.fecha_retiro >= :desde")
				+ (hasta == null ? "" : " and r.fecha_retiro <= :hasta")
				+ " group by l.prenda_id, p.nombre order by unidades desc limit :limite";
		JdbcClient.StatementSpec spec = jdbc.sql(sql).param("empresaId", empresaId).param("limite", limite);
		if (sucursalId != null) {
			spec = spec.param("sucursalId", sucursalId);
		}
		if (desde != null) {
			spec = spec.param("desde", desde);
		}
		if (hasta != null) {
			spec = spec.param("hasta", hasta);
		}
		return spec.query(ArticuloRanking.class).list();
	}

	@Override
	public List<EmpleadoVentas> ventasPorEmpleado(UUID empresaId, UUID sucursalId) {
		String sql = "select v.empleado_id, u.email, count(*) as numero_ventas, coalesce(sum(v.total), 0) as total"
				+ " from venta v join usuario u on u.id = v.empleado_id"
				+ " where v.empresa_id = :empresaId"
				+ (sucursalId == null ? "" : " and v.sucursal_id = :sucursalId")
				+ " group by v.empleado_id, u.email order by total desc";
		JdbcClient.StatementSpec spec = jdbc.sql(sql).param("empresaId", empresaId);
		if (sucursalId != null) {
			spec = spec.param("sucursalId", sucursalId);
		}
		return spec.query(EmpleadoVentas.class).list();
	}

	@Override
	public List<ValorEtiquetaRanking> ventasPorEtiqueta(UUID empresaId, UUID tipoEtiquetaId, UUID sucursalId) {
		String sql = "select ve.valor,"
				+ " coalesce(sum(lv.cantidad), 0) as unidades,"
				+ " coalesce(sum(lv.cantidad * lv.precio_unitario), 0) as monto"
				+ " from linea_de_venta lv"
				+ " join venta v on v.id = lv.venta_id"
				+ " join prenda_valor_etiqueta pve"
				+ "   on pve.prenda_id = lv.prenda_id and pve.tipo_etiqueta_id = :tipoEtiquetaId"
				+ " join valor_etiqueta ve on ve.id = pve.valor_etiqueta_id"
				+ " where lv.empresa_id = :empresaId"
				+ (sucursalId == null ? "" : " and v.sucursal_id = :sucursalId")
				+ " group by ve.valor order by unidades desc";
		JdbcClient.StatementSpec spec = jdbc.sql(sql).param("empresaId", empresaId).param("tipoEtiquetaId", tipoEtiquetaId);
		if (sucursalId != null) {
			spec = spec.param("sucursalId", sucursalId);
		}
		return spec.query(ValorEtiquetaRanking.class).list();
	}
}
