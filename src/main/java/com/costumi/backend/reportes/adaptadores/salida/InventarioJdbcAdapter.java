package com.costumi.backend.reportes.adaptadores.salida;

import com.costumi.backend.reportes.dominio.GrupoInventario;
import com.costumi.backend.reportes.dominio.InventarioReadRepository;
import com.costumi.backend.reportes.dominio.ResumenInventario;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/** Adaptador de lectura de reportes de inventario (RF-9.1/9.3) con JdbcClient sobre {@code grupo_de_stock}. */
@Repository
class InventarioJdbcAdapter implements InventarioReadRepository {

	// Agrega por prenda (una prenda puede tener stock en varias sucursales); el filtro de sucursal es opcional.
	private static final String TABLERO = """
			select g.prenda_id, p.nombre as prenda_nombre,
			       sum(g.disponibles)::int as disponibles, sum(g.rentadas)::int as rentadas,
			       sum(g.danadas)::int as danadas,
			       sum(g.en_limpieza)::int as en_limpieza, sum(g.perdidas)::int as perdidas
			from grupo_de_stock g
			join prenda p on p.id = g.prenda_id
			where g.empresa_id = :empresaId%s
			group by g.prenda_id, p.nombre
			order by p.nombre
			""";

	private final JdbcClient jdbc;

	InventarioJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<GrupoInventario> tablero(UUID empresaId, UUID sucursalId) {
		String sql = TABLERO.formatted(sucursalId == null ? "" : " and g.sucursal_id = :sucursalId");
		JdbcClient.StatementSpec spec = jdbc.sql(sql).param("empresaId", empresaId);
		if (sucursalId != null) {
			spec = spec.param("sucursalId", sucursalId);
		}
		return spec.query(GrupoInventario.class).list();
	}

	@Override
	public ResumenInventario resumen(UUID empresaId) {
		// El total incluye las rentadas (siguen siendo del inventario); disponibles ya NO cuenta las rentadas.
		long[] estados = jdbc.sql("""
				select coalesce(sum(disponibles + rentadas + danadas + en_limpieza + perdidas), 0) as total,
				       coalesce(sum(disponibles), 0)  as disponibles,
				       coalesce(sum(rentadas), 0)     as rentadas,
				       coalesce(sum(danadas), 0)      as danadas,
				       coalesce(sum(en_limpieza), 0)  as en_limpieza,
				       coalesce(sum(perdidas), 0)     as perdidas
				from grupo_de_stock where empresa_id = :empresaId
				""").param("empresaId", empresaId).query((rs, n) -> new long[] {
					rs.getLong("total"), rs.getLong("disponibles"), rs.getLong("rentadas"), rs.getLong("danadas"),
					rs.getLong("en_limpieza"), rs.getLong("perdidas") }).single();

		// Rentadas ahora = unidades comprometidas en rentas (disponible -> rentada), tomadas del stock real.
		long rentadasAhora = estados[2];

		// Valor de inventario: cuenta disponibles + rentadas (las rentadas siguen siendo mercancía propia).
		BigDecimal valor = jdbc.sql("""
				select coalesce(sum((g.disponibles + g.rentadas) * coalesce(p.precio_venta, 0)), 0)
				from grupo_de_stock g join prenda p on p.id = g.prenda_id
				where g.empresa_id = :empresaId
				""").param("empresaId", empresaId).query(BigDecimal.class).single();

		long total = estados[0];
		BigDecimal utilizacion = total == 0 ? BigDecimal.ZERO
				: BigDecimal.valueOf(rentadasAhora).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
		return new ResumenInventario(total, estados[1], estados[3], estados[4], estados[5], rentadasAhora, utilizacion,
				valor);
	}
}
