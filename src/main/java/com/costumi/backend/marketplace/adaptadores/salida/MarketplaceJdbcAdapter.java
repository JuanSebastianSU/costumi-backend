package com.costumi.backend.marketplace.adaptadores.salida;

import com.costumi.backend.marketplace.dominio.EmpresaEnVitrina;
import com.costumi.backend.marketplace.dominio.MarketplaceReadRepository;
import com.costumi.backend.marketplace.dominio.PrendaEnVitrina;
import com.costumi.backend.marketplace.dominio.SucursalEnVitrina;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Adaptador de lectura: lista las empresas ACTIVAS (visibles públicamente, RF-15.6). */
@Repository
class MarketplaceJdbcAdapter implements MarketplaceReadRepository {

	// Solo tiendas que PUEDEN operar: ACTIVA y con al menos un punto de retiro (sucursal no archivada).
	// Así el cliente no entra a una tienda sin sucursal, que no le dejaría armar el pedido. El filtro de
	// texto va siempre (con texto vacío, '%%' matchea todas): evita el problema de tipo del param null.
	private static final String EMPRESAS_WHERE =
			"from empresa e where e.estado = 'ACTIVA' "
			+ "and exists (select 1 from sucursal s where s.empresa_id = e.id and s.archivada = false) "
			+ "and lower(e.nombre) like lower('%' || :texto || '%')";

	private static final String EMPRESAS_COUNT = "select count(*) " + EMPRESAS_WHERE;

	private static final String EMPRESAS_PAGINA = "select id, nombre, logo_url, ciudad, descripcion "
			+ EMPRESAS_WHERE + " order by nombre limit :tamano offset :offset";

	// Catálogo público: prendas no archivadas de una empresa, solo si la empresa está ACTIVA.
	// Opcionalmente filtrado por categoría (RF-18.1); el ORDER BY se agrega al final en el método.
	private static final String CATALOGO = "select p.id, p.nombre, p.tipo_articulo, p.precio_renta, "
			+ "p.precio_venta, c.nombre as categoria, p.foto_url "
			+ "from prenda p join categoria c on c.id = p.categoria_id join empresa e on e.id = p.empresa_id "
			+ "where p.empresa_id = :empresaId and p.archivada = false and e.estado = 'ACTIVA'";

	// Sucursales (puntos de retiro) no archivadas de una empresa, solo si la empresa está ACTIVA.
	private static final String SUCURSALES = "select s.id, s.nombre, s.direccion, s.ubicacion_maps, s.descripcion, "
			+ "s.latitud, s.longitud, s.foto_url "
			+ "from sucursal s join empresa e on e.id = s.empresa_id "
			+ "where s.empresa_id = :empresaId and s.archivada = false and e.estado = 'ACTIVA' "
			+ "order by s.nombre";

	private final JdbcClient jdbc;

	MarketplaceJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public com.costumi.backend.compartido.Pagina<EmpresaEnVitrina> empresas(String texto,
			com.costumi.backend.compartido.SolicitudDePagina solicitud) {
		String t = (texto == null) ? "" : texto.trim();
		Long total = jdbc.sql(EMPRESAS_COUNT).param("texto", t).query(Long.class).single();
		if (total == null || total == 0) {
			return new com.costumi.backend.compartido.Pagina<>(List.of(), 0, solicitud.pagina(), solicitud.tamano());
		}
		List<EmpresaEnVitrina> contenido = jdbc.sql(EMPRESAS_PAGINA).param("texto", t)
				.param("tamano", solicitud.tamano())
				.param("offset", (long) solicitud.pagina() * solicitud.tamano())
				.query(MarketplaceJdbcAdapter::mapear).list();
		return new com.costumi.backend.compartido.Pagina<>(contenido, total, solicitud.pagina(), solicitud.tamano());
	}

	@Override
	public List<PrendaEnVitrina> catalogoDe(UUID empresaId, UUID categoriaId) {
		String sql = CATALOGO + (categoriaId != null ? " and p.categoria_id = :categoriaId" : "") + " order by p.nombre";
		JdbcClient.StatementSpec spec = jdbc.sql(sql).param("empresaId", empresaId);
		if (categoriaId != null) {
			spec = spec.param("categoriaId", categoriaId);
		}
		return spec.query(MarketplaceJdbcAdapter::mapearPrenda).list();
	}

	@Override
	public List<SucursalEnVitrina> sucursalesActivasDe(UUID empresaId) {
		return jdbc.sql(SUCURSALES).param("empresaId", empresaId).query(MarketplaceJdbcAdapter::mapearSucursal).list();
	}

	private static EmpresaEnVitrina mapear(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
		return EmpresaEnVitrina.de(rs.getObject("id", UUID.class), rs.getString("nombre"),
				rs.getString("logo_url"), rs.getString("ciudad"), rs.getString("descripcion"));
	}

	private static PrendaEnVitrina mapearPrenda(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
		return new PrendaEnVitrina(
				rs.getObject("id", UUID.class),
				rs.getString("nombre"),
				rs.getString("tipo_articulo"),
				rs.getBigDecimal("precio_renta"),
				rs.getBigDecimal("precio_venta"),
				rs.getString("categoria"),
				rs.getString("foto_url"));
	}

	private static SucursalEnVitrina mapearSucursal(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
		return new SucursalEnVitrina(
				rs.getObject("id", UUID.class),
				rs.getString("nombre"),
				rs.getString("direccion"),
				rs.getString("ubicacion_maps"),
				rs.getString("descripcion"),
				(Double) rs.getObject("latitud"),
				(Double) rs.getObject("longitud"),
				rs.getString("foto_url"));
	}
}
