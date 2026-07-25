package com.costumi.backend.marketplace.adaptadores.salida;

import com.costumi.backend.marketplace.dominio.CategoriaEnVitrina;
import com.costumi.backend.marketplace.dominio.DisfrazDestacado;
import com.costumi.backend.marketplace.dominio.EmpresaEnVitrina;
import com.costumi.backend.marketplace.dominio.HorarioEnVitrina;
import com.costumi.backend.marketplace.dominio.MarketplaceReadRepository;
import com.costumi.backend.marketplace.dominio.PrendaEnVitrina;
import com.costumi.backend.marketplace.dominio.SucursalEnVitrina;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
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

	// Columnas de la vitrina: incluye la portada y cuántos disfraces activos tiene la tienda (para la tarjeta).
	private static final String EMPRESAS_COLS = "select id, nombre, logo_url, portada_url, ciudad, descripcion, "
			+ "(select count(*) from disfraz d where d.empresa_id = e.id and d.activo = true) as disfraces_count ";

	private static final String EMPRESAS_PAGINA = EMPRESAS_COLS + EMPRESAS_WHERE
			+ " order by nombre limit :tamano offset :offset";

	// Detalle público de UNA tienda ACTIVA (con punto de retiro): para la cabecera de su vitrina.
	private static final String EMPRESA_DETALLE = EMPRESAS_COLS
			+ "from empresa e where e.id = :empresaId and e.estado = 'ACTIVA' "
			+ "and exists (select 1 from sucursal s where s.empresa_id = e.id and s.archivada = false)";

	// Facetas: categorías (de prenda) que realmente aparecen en el catálogo público de la tienda.
	private static final String CATEGORIAS_DE_TIENDA = "select distinct c.id, c.nombre "
			+ "from prenda p join categoria c on c.id = p.categoria_id join empresa e on e.id = p.empresa_id "
			+ "where p.empresa_id = :empresaId and p.archivada = false and c.archivada = false "
			+ "and e.estado = 'ACTIVA' order by c.nombre";

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
	public Optional<EmpresaEnVitrina> empresa(UUID empresaId) {
		return jdbc.sql(EMPRESA_DETALLE).param("empresaId", empresaId).query(MarketplaceJdbcAdapter::mapear)
				.optional();
	}

	@Override
	public List<CategoriaEnVitrina> categoriasDe(UUID empresaId) {
		return jdbc.sql(CATEGORIAS_DE_TIENDA).param("empresaId", empresaId)
				.query((rs, rowNum) -> new CategoriaEnVitrina(rs.getObject("id", UUID.class), rs.getString("nombre")))
				.list();
	}

	// Destacados: disfraces de tiendas ACTIVAS (con punto de retiro), ordenados por movimiento = cuántas
	// instancias (grupos) se vendieron + rentaron. Sin movimiento aún → van al final (el carrusel no queda
	// vacío en un marketplace joven). Cuenta grupos distintos para no multiplicar por las piezas del disfraz.
	private static final String DESTACADOS = "select d.id as disfraz_id, d.empresa_id, e.nombre as empresa_nombre, "
			+ "d.nombre, d.foto_url, d.precio_renta_general as precio_renta, d.precio_venta_general as precio_venta, "
			+ "(coalesce(v.u, 0) + coalesce(r.u, 0)) as movimiento "
			+ "from disfraz d join empresa e on e.id = d.empresa_id "
			+ "left join (select disfraz_id, count(distinct disfraz_grupo) u from linea_de_venta "
			+ "           where disfraz_id is not null group by disfraz_id) v on v.disfraz_id = d.id "
			+ "left join (select disfraz_id, count(distinct disfraz_grupo) u from renta_linea "
			+ "           where disfraz_id is not null group by disfraz_id) r on r.disfraz_id = d.id "
			+ "where d.activo = true and e.estado = 'ACTIVA' "
			+ "and exists (select 1 from sucursal s where s.empresa_id = e.id and s.archivada = false) "
			+ "order by movimiento desc, d.nombre limit :limite";

	@Override
	public List<DisfrazDestacado> destacados(int limite) {
		return jdbc.sql(DESTACADOS).param("limite", limite)
				.query((rs, rowNum) -> new DisfrazDestacado(
						rs.getObject("disfraz_id", UUID.class), rs.getObject("empresa_id", UUID.class),
						rs.getString("empresa_nombre"), rs.getString("nombre"), rs.getString("foto_url"),
						rs.getBigDecimal("precio_renta"), rs.getBigDecimal("precio_venta")))
				.list();
	}

	@Override
	public List<HorarioEnVitrina> horarioDe(UUID empresaId) {
		return jdbc.sql("select dia_semana, abre, cierra from horario_atencion where empresa_id = :empresaId "
						+ "order by dia_semana")
				.param("empresaId", empresaId)
				.query((rs, rowNum) -> new HorarioEnVitrina(rs.getInt("dia_semana"),
						rs.getObject("abre", java.time.LocalTime.class),
						rs.getObject("cierra", java.time.LocalTime.class)))
				.list();
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
				rs.getString("logo_url"), rs.getString("portada_url"), rs.getString("ciudad"),
				rs.getString("descripcion"), rs.getLong("disfraces_count"));
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
