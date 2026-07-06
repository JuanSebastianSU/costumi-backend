package com.costumi.backend.marketplace.adaptadores.salida;

import com.costumi.backend.marketplace.dominio.EmpresaEnVitrina;
import com.costumi.backend.marketplace.dominio.MarketplaceReadRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Adaptador de lectura: lista las empresas ACTIVAS (visibles públicamente, RF-15.6). */
@Repository
class MarketplaceJdbcAdapter implements MarketplaceReadRepository {

	private static final String EMPRESAS_ACTIVAS =
			"select id, nombre from empresa where estado = 'ACTIVA' order by nombre";

	private static final String BUSCAR_EMPRESAS = "select id, nombre from empresa "
			+ "where estado = 'ACTIVA' and lower(nombre) like lower('%' || :texto || '%') order by nombre";

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

	private static EmpresaEnVitrina mapear(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
		return EmpresaEnVitrina.de(rs.getObject("id", UUID.class), rs.getString("nombre"));
	}
}
