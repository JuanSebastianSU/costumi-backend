package com.costumi.backend.notificaciones.adaptadores.salida;

import com.costumi.backend.notificaciones.dominio.AvisosDeStockBajoReadRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador de lectura (RF-11.2): grupos de stock por debajo del umbral, con JdbcClient sobre
 * {@code grupo_de_stock}. Modelo de lectura sobre el esquema compartido; no depende de clases de Inventario.
 */
@Repository
class AvisosDeStockBajoJdbcAdapter implements AvisosDeStockBajoReadRepository {

	private static final String EMPRESAS_CON_STOCK_BAJO = """
			select distinct empresa_id
			from grupo_de_stock
			where disponibles < :umbral
			""";

	private static final String CONTEO = """
			select count(*)
			from grupo_de_stock
			where empresa_id = :empresaId and disponibles < :umbral
			""";

	private final JdbcClient jdbc;

	AvisosDeStockBajoJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<UUID> empresasConStockBajo(int umbral) {
		return jdbc.sql(EMPRESAS_CON_STOCK_BAJO).param("umbral", umbral).query(UUID.class).list();
	}

	@Override
	public int conteoStockBajo(UUID empresaId, int umbral) {
		return jdbc.sql(CONTEO).param("empresaId", empresaId).param("umbral", umbral)
				.query(Integer.class).single();
	}
}
