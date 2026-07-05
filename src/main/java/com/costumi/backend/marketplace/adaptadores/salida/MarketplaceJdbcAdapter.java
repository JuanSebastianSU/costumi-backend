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

	private final JdbcClient jdbc;

	MarketplaceJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<EmpresaEnVitrina> empresasActivas() {
		return jdbc.sql(EMPRESAS_ACTIVAS)
				.query((rs, rowNum) -> EmpresaEnVitrina.de(rs.getObject("id", UUID.class), rs.getString("nombre")))
				.list();
	}
}
