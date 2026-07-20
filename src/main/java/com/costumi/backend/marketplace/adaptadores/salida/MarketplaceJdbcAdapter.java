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
	// Así el cliente no entra a una tienda sin sucursal, que no le dejaría armar el pedido.
	private static final String EMPRESAS_ACTIVAS =
			"select id, nombre from empresa e where e.estado = 'ACTIVA' "
			+ "and exists (select 1 from sucursal s where s.empresa_id = e.id and s.archivada = false) "
			+ "order by nombre";

	private static final String BUSCAR_EMPRESAS = "select id, nombre from empresa e "
			+ "where e.estado = 'ACTIVA' and lower(e.nombre) like lower('%' || :texto || '%') "
			+ "and exists (select 1 from sucursal s where s.empresa_id = e.id and s.archivada = false) "
			+ "order by nombre";

	// Catálogo público: prendas no archivadas de una empresa, solo si la empresa está ACTIVA.
	private static final String CATALOGO = "select p.id, p.nombre, p.tipo_articulo, p.precio_renta, "
			+ "p.precio_venta, c.nombre as categoria, p.foto_url "
			+ "from prenda p join categoria c on c.id = p.categoria_id join empresa e on e.id = p.empresa_id "
			+ "where p.empresa_id = :empresaId and p.archivada = false and e.estado = 'ACTIVA' "
			+ "order by p.nombre";

	// Sucursales (puntos de retiro) no archivadas de una empresa, solo si la empresa está ACTIVA.
	private static final String SUCURSALES = "select s.id, s.nombre, s.direccion "
			+ "from sucursal s join empresa e on e.id = s.empresa_id "
			+ "where s.empresa_id = :empresaId and s.archivada = false and e.estado = 'ACTIVA' "
			+ "order by s.nombre";

	private final JdbcClient jdbc;

	MarketplaceJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<EmpresaEnVitrina> empresasActivas() {
		return jdbc.sql(EMPRESAS_ACTIVAS).query(MarketplaceJdbcAdapter::mapear).list();
	}

	@Override
	public List<EmpresaEnVitrina> buscarEmpresas(String texto) {
		if (texto == null || texto.isBlank()) {
			return empresasActivas();
		}
		return jdbc.sql(BUSCAR_EMPRESAS).param("texto", texto.trim()).query(MarketplaceJdbcAdapter::mapear).list();
	}

	@Override
	public List<PrendaEnVitrina> catalogoDe(UUID empresaId) {
		return jdbc.sql(CATALOGO).param("empresaId", empresaId).query(MarketplaceJdbcAdapter::mapearPrenda).list();
	}

	@Override
	public List<SucursalEnVitrina> sucursalesActivasDe(UUID empresaId) {
		return jdbc.sql(SUCURSALES).param("empresaId", empresaId).query(MarketplaceJdbcAdapter::mapearSucursal).list();
	}

	private static EmpresaEnVitrina mapear(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
		return EmpresaEnVitrina.de(rs.getObject("id", UUID.class), rs.getString("nombre"));
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
				rs.getString("direccion"));
	}
}
