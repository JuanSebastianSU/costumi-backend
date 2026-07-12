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
			select 'RENTA' as tipo, r.id as operacion_id, r.importe as monto, r.estado, r.fecha_retiro as fecha,
			       e.id as empresa_id, e.nombre as empresa_nombre
			from renta r join empresa e on e.id = r.empresa_id
			where r.empresa_id = :empresaId and r.cliente_id = :clienteId
			union all
			select 'VENTA' as tipo, v.id as operacion_id, v.total as monto, v.estado, null::date as fecha,
			       e.id as empresa_id, e.nombre as empresa_nombre
			from venta v join empresa e on e.id = v.empresa_id
			where v.empresa_id = :empresaId and v.cliente_id = :clienteId
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
