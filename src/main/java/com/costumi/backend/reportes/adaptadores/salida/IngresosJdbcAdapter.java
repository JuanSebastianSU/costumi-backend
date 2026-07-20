package com.costumi.backend.reportes.adaptadores.salida;

import com.costumi.backend.reportes.dominio.IngresosReadRepository;
import com.costumi.backend.reportes.dominio.ResumenDeIngresos;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
	public ResumenDeIngresos deEmpresa(UUID empresaId, UUID sucursalId) {
		return ResumenDeIngresos.de(suma(empresaId, "RENTA", sucursalId), suma(empresaId, "VENTA", sucursalId));
	}

	private BigDecimal suma(UUID empresaId, String tipo, UUID sucursalId) {
		String sql = SUMA_POR_TIPO + (sucursalId == null ? "" : " and sucursal_id = :sucursalId");
		JdbcClient.StatementSpec spec = jdbc.sql(sql).param("empresaId", empresaId).param("tipo", tipo);
		if (sucursalId != null) {
			spec = spec.param("sucursalId", sucursalId);
		}
		return spec.query(BigDecimal.class).single();
	}
}
