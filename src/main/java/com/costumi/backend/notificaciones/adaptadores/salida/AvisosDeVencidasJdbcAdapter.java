package com.costumi.backend.notificaciones.adaptadores.salida;

import com.costumi.backend.notificaciones.dominio.AvisosDeVencidasReadRepository;
import com.costumi.backend.notificaciones.dominio.RentaVencidaAviso;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Adaptador de lectura (RF-11.1): rentas ACTIVAS vencidas de la empresa, con JdbcClient sobre {@code renta}. */
@Repository
class AvisosDeVencidasJdbcAdapter implements AvisosDeVencidasReadRepository {

	private static final String VENCIDAS = """
			select id as renta_id, cliente_id, fecha_devolucion
			from renta
			where empresa_id = :empresaId and estado = 'ACTIVA' and fecha_devolucion < :hoy
			""";

	private static final String EMPRESAS_CON_VENCIDAS = """
			select distinct empresa_id
			from renta
			where estado = 'ACTIVA' and fecha_devolucion < :hoy
			""";

	private final JdbcClient jdbc;

	AvisosDeVencidasJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<RentaVencidaAviso> vencidas(UUID empresaId, LocalDate hoy) {
		return jdbc.sql(VENCIDAS).param("empresaId", empresaId).param("hoy", hoy)
				.query(RentaVencidaAviso.class).list();
	}

	@Override
	public List<UUID> empresasConVencidas(LocalDate hoy) {
		return jdbc.sql(EMPRESAS_CON_VENCIDAS).param("hoy", hoy).query(UUID.class).list();
	}
}
