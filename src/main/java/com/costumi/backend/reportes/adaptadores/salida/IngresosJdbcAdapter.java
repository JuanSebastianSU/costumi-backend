package com.costumi.backend.reportes.adaptadores.salida;

import com.costumi.backend.reportes.dominio.IngresosReadRepository;
import com.costumi.backend.reportes.dominio.ResumenDeIngresos;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Adaptador de lectura: suma los pagos por tipo de concepto (renta/venta) de la empresa.
 * Es un modelo de lectura sobre el esquema compartido; no depende de otros módulos por código.
 */
@Repository
class IngresosJdbcAdapter implements IngresosReadRepository {

	private static final String SUMA_POR_TIPO =
			"select coalesce(sum(monto), 0) from pago where empresa_id = :empresaId and tipo_concepto = :tipo";

	private final JdbcClient jdbc;

	IngresosJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public ResumenDeIngresos deEmpresa(UUID empresaId, UUID sucursalId, LocalDate desde, LocalDate hasta) {
		return ResumenDeIngresos.de(suma(empresaId, "RENTA", sucursalId, desde, hasta),
				suma(empresaId, "VENTA", sucursalId, desde, hasta));
	}

	private BigDecimal suma(UUID empresaId, String tipo, UUID sucursalId, LocalDate desde, LocalDate hasta) {
		// El rango filtra por la fecha del pago (inclusive), igual que ingresos-por-metodo.
		StringBuilder sql = new StringBuilder(SUMA_POR_TIPO);
		if (sucursalId != null) {
			sql.append(" and sucursal_id = :sucursalId");
		}
		if (desde != null) {
			sql.append(" and fecha::date >= :desde");
		}
		if (hasta != null) {
			sql.append(" and fecha::date <= :hasta");
		}
		JdbcClient.StatementSpec spec = jdbc.sql(sql.toString()).param("empresaId", empresaId).param("tipo", tipo);
		if (sucursalId != null) {
			spec = spec.param("sucursalId", sucursalId);
		}
		if (desde != null) {
			spec = spec.param("desde", desde);
		}
		if (hasta != null) {
			spec = spec.param("hasta", hasta);
		}
		return spec.query(BigDecimal.class).single();
	}
}
