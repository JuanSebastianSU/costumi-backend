package com.costumi.backend.inventario.adaptadores.salida;

import com.costumi.backend.inventario.dominio.CatalogoDeInventarioReadRepository;
import com.costumi.backend.inventario.dominio.PrendaDeCatalogo;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Modelo de lectura (JdbcClient) del catálogo de inventario del tenant. Lista las prendas activas de la
 * empresa —opcionalmente de una categoría y que satisfacen valores de etiqueta por dimensión— con su
 * stock disponible (suma de {@code disponibles} de todos sus grupos de stock) y sus etiquetas.
 *
 * <p>Reemplaza el filtrado en memoria por pool (que cargaba TODAS las prendas del tenant y filtraba con
 * streams, N+1): el filtro por categoría y etiquetas se hace en SQL, y las etiquetas se traen en UNA sola
 * consulta para el conjunto de prendas resultante.
 */
@Repository
class CatalogoDeInventarioJdbcAdapter implements CatalogoDeInventarioReadRepository {

	private static final String BASE = """
			select p.id, p.nombre, p.categoria_id, p.tipo_articulo,
			       p.precio_renta, p.precio_venta, p.valor_dano, p.valor_reposicion, p.foto_url,
			       coalesce(g.disp, 0)::int as unidades_disponibles
			from prenda p
			left join (select prenda_id, sum(disponibles) as disp from grupo_de_stock
			           where empresa_id = :empresaId group by prenda_id) g on g.prenda_id = p.id
			where p.empresa_id = :empresaId and p.archivada = false%s
			order by p.nombre
			""";

	private final JdbcClient jdbc;

	CatalogoDeInventarioJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<PrendaDeCatalogo> buscar(UUID empresaId, UUID categoriaId, Map<UUID, Set<UUID>> etiquetas) {
		// Una dimensión = un tipo de etiqueta con al menos un valor pedido. AND entre dimensiones (un EXISTS
		// por cada una), OR dentro de la dimensión (valor_etiqueta_id in (...)).
		List<Map.Entry<UUID, Set<UUID>>> dimensiones = new ArrayList<>();
		if (etiquetas != null) {
			etiquetas.forEach((tipo, valores) -> {
				if (tipo != null && valores != null && !valores.isEmpty()) {
					dimensiones.add(Map.entry(tipo, valores));
				}
			});
		}

		StringBuilder filtros = new StringBuilder();
		if (categoriaId != null) {
			filtros.append(" and p.categoria_id = :categoriaId");
		}
		for (int i = 0; i < dimensiones.size(); i++) {
			filtros.append(" and exists (select 1 from prenda_valor_etiqueta pve where pve.prenda_id = p.id"
					+ " and pve.tipo_etiqueta_id = :tipo" + i + " and pve.valor_etiqueta_id in (:valores" + i + "))");
		}

		JdbcClient.StatementSpec spec = jdbc.sql(BASE.formatted(filtros.toString())).param("empresaId", empresaId);
		if (categoriaId != null) {
			spec = spec.param("categoriaId", categoriaId);
		}
		for (int i = 0; i < dimensiones.size(); i++) {
			spec = spec.param("tipo" + i, dimensiones.get(i).getKey());
			spec = spec.param("valores" + i, dimensiones.get(i).getValue());
		}

		List<PrendaDeCatalogo> base = spec.query(CatalogoDeInventarioJdbcAdapter::mapear).list();
		if (base.isEmpty()) {
			return base;
		}

		Map<UUID, Map<UUID, UUID>> etiquetasPorPrenda = etiquetasDe(base.stream().map(PrendaDeCatalogo::id).toList());
		return base.stream()
				.map(p -> new PrendaDeCatalogo(p.id(), p.nombre(), p.categoriaId(), p.tipoArticulo(),
						p.precioRenta(), p.precioVenta(), p.valorDano(), p.valorReposicion(), p.fotoUrl(),
						p.unidadesDisponibles(), etiquetasPorPrenda.getOrDefault(p.id(), Map.of())))
				.toList();
	}

	/** Etiquetas ({@code tipo -> valor}) de un conjunto de prendas, en UNA sola consulta (evita N+1). */
	private Map<UUID, Map<UUID, UUID>> etiquetasDe(List<UUID> prendaIds) {
		Map<UUID, Map<UUID, UUID>> mapa = new LinkedHashMap<>();
		jdbc.sql("select prenda_id, tipo_etiqueta_id, valor_etiqueta_id from prenda_valor_etiqueta"
						+ " where prenda_id in (:ids)")
				.param("ids", prendaIds)
				.query((rs, n) -> {
					mapa.computeIfAbsent(rs.getObject("prenda_id", UUID.class), k -> new LinkedHashMap<>())
							.put(rs.getObject("tipo_etiqueta_id", UUID.class),
									rs.getObject("valor_etiqueta_id", UUID.class));
					return null;
				})
				.list();
		return mapa;
	}

	private static PrendaDeCatalogo mapear(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
		return new PrendaDeCatalogo(
				rs.getObject("id", UUID.class),
				rs.getString("nombre"),
				rs.getObject("categoria_id", UUID.class),
				rs.getString("tipo_articulo"),
				rs.getBigDecimal("precio_renta"),
				rs.getBigDecimal("precio_venta"),
				rs.getBigDecimal("valor_dano"),
				rs.getBigDecimal("valor_reposicion"),
				rs.getString("foto_url"),
				rs.getInt("unidades_disponibles"),
				Map.of());
	}
}
