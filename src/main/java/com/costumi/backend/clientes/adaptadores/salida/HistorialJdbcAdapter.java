package com.costumi.backend.clientes.adaptadores.salida;

import com.costumi.backend.clientes.dominio.HistorialItem;
import com.costumi.backend.clientes.dominio.HistorialReadRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador de lectura del historial de clientes (RF-7.2) con JdbcClient sobre {@code renta}/{@code venta}.
 * Es un modelo de lectura sobre el esquema compartido; no depende de otros módulos por código.
 */
@Repository
class HistorialJdbcAdapter implements HistorialReadRepository {

	private static final String HISTORIAL = """
			select 'RENTA' as tipo, id as operacion_id, importe as monto, estado, fecha_retiro as fecha
			from renta where empresa_id = :empresaId and cliente_id = :clienteId
			union all
			select 'VENTA' as tipo, id as operacion_id, total as monto, estado, null::date as fecha
			from venta where empresa_id = :empresaId and cliente_id = :clienteId
			order by fecha desc nulls last
			""";

	private final JdbcClient jdbc;

	HistorialJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<HistorialItem> deCliente(UUID empresaId, UUID clienteId) {
		return jdbc.sql(HISTORIAL).param("empresaId", empresaId).param("clienteId", clienteId)
				.query(HistorialItem.class).list();
	}

	@Override
	public List<UUID> clientesConPendientes(UUID empresaId) {
		return jdbc.sql("select distinct cliente_id from renta where empresa_id = :empresaId and estado = 'ACTIVA'")
				.param("empresaId", empresaId).query(UUID.class).list();
	}
}
