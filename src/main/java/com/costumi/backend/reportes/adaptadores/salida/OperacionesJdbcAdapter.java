package com.costumi.backend.reportes.adaptadores.salida;

import com.costumi.backend.reportes.dominio.OperacionesReadRepository;
import com.costumi.backend.reportes.dominio.RentaVencida;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador de lectura de reportes operativos (RF-9.1) con JdbcClient sobre {@code renta}. El filtro
 * de sucursal es opcional: {@code null} = todas las sucursales de la empresa (se agrega a la consulta
 * solo cuando viene, para no dejar parámetros sin tipo).
 */
@Repository
class OperacionesJdbcAdapter implements OperacionesReadRepository {

	private final JdbcClient jdbc;

	OperacionesJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<RentaVencida> rentasVencidas(UUID empresaId, UUID sucursalId, LocalDate hoy) {
		String sql = "select id as renta_id, cliente_id, prenda_id, fecha_devolucion, importe, deposito"
				+ " from renta where empresa_id = :empresaId and estado = 'ACTIVA' and fecha_devolucion < :hoy"
				+ (sucursalId == null ? "" : " and sucursal_id = :sucursalId")
				+ " order by fecha_devolucion";
		JdbcClient.StatementSpec spec = jdbc.sql(sql).param("empresaId", empresaId).param("hoy", hoy);
		if (sucursalId != null) {
			spec = spec.param("sucursalId", sucursalId);
		}
		return spec.query(RentaVencida.class).list();
	}

	@Override
	public BigDecimal depositosActivos(UUID empresaId, UUID sucursalId) {
		String sql = "select coalesce(sum(deposito), 0) from renta where empresa_id = :empresaId"
				+ " and estado in ('RESERVADA', 'ACTIVA', 'DEVUELTA')"
				+ (sucursalId == null ? "" : " and sucursal_id = :sucursalId");
		JdbcClient.StatementSpec spec = jdbc.sql(sql).param("empresaId", empresaId);
		if (sucursalId != null) {
			spec = spec.param("sucursalId", sucursalId);
		}
		return spec.query(BigDecimal.class).single();
	}
}
