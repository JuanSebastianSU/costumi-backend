package com.costumi.backend.reportes.adaptadores.salida;

import com.costumi.backend.reportes.dominio.GananciaReadRepository;
import com.costumi.backend.reportes.dominio.ResumenDeGanancia;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Adaptador de lectura: ganancia = ingresos (suma de pagos) − costo de lo vendido (líneas de venta ×
 * costo de adquisición de la prenda). Modelo de lectura sobre el esquema compartido, acotado al tenant.
 */
@Repository
class GananciaJdbcAdapter implements GananciaReadRepository {

	private static final String INGRESOS =
			"select coalesce(sum(monto), 0) from pago where empresa_id = :empresaId";

	// El costo se acota por sucursal uniendo a la venta (linea_de_venta no lleva sucursal_id).
	private static final String COSTO_DE_VENTAS = """
			select coalesce(sum(lv.cantidad * coalesce(p.costo_adquisicion, 0)), 0)
			from linea_de_venta lv
			join venta v on v.id = lv.venta_id
			join prenda p on p.id = lv.prenda_id
			where lv.empresa_id = :empresaId
			""";

	private final JdbcClient jdbc;

	GananciaJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public ResumenDeGanancia deEmpresa(UUID empresaId, UUID sucursalId) {
		String ingresosSql = INGRESOS + (sucursalId == null ? "" : " and sucursal_id = :sucursalId");
		String costoSql = COSTO_DE_VENTAS + (sucursalId == null ? "" : " and v.sucursal_id = :sucursalId");
		BigDecimal ingresos = conSucursal(jdbc.sql(ingresosSql).param("empresaId", empresaId), sucursalId)
				.query(BigDecimal.class).single();
		BigDecimal costo = conSucursal(jdbc.sql(costoSql).param("empresaId", empresaId), sucursalId)
				.query(BigDecimal.class).single();
		return ResumenDeGanancia.de(ingresos, costo);
	}

	private static JdbcClient.StatementSpec conSucursal(JdbcClient.StatementSpec spec, UUID sucursalId) {
		return sucursalId == null ? spec : spec.param("sucursalId", sucursalId);
	}
}
