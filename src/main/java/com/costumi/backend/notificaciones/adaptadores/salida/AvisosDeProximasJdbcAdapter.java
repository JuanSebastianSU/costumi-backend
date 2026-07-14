package com.costumi.backend.notificaciones.adaptadores.salida;

import com.costumi.backend.notificaciones.dominio.AvisosDeProximasReadRepository;
import com.costumi.backend.notificaciones.dominio.RentaProximaAviso;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Adaptador de lectura (RF-11.1): rentas ACTIVAS que vencen en una fecha, con JdbcClient sobre {@code renta}. */
@Repository
class AvisosDeProximasJdbcAdapter implements AvisosDeProximasReadRepository {

	private static final String PROXIMAS = """
			select id as renta_id, cliente_id, sucursal_id, fecha_devolucion
			from renta
			where empresa_id = :empresaId and estado = 'ACTIVA' and fecha_devolucion = :fechaObjetivo
			""";

	private static final String EMPRESAS_CON_PROXIMAS = """
			select distinct empresa_id
			from renta
			where estado = 'ACTIVA' and fecha_devolucion = :fechaObjetivo
			""";

	private final JdbcClient jdbc;

	AvisosDeProximasJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<RentaProximaAviso> proximas(UUID empresaId, LocalDate fechaObjetivo) {
		return jdbc.sql(PROXIMAS).param("empresaId", empresaId).param("fechaObjetivo", fechaObjetivo)
				.query(RentaProximaAviso.class).list();
	}

	@Override
	public List<UUID> empresasConProximas(LocalDate fechaObjetivo) {
		return jdbc.sql(EMPRESAS_CON_PROXIMAS).param("fechaObjetivo", fechaObjetivo).query(UUID.class).list();
	}
}
