package com.costumi.backend.reportes.adaptadores.salida;

import com.costumi.backend.reportes.dominio.GananciaReadRepository;
import com.costumi.backend.reportes.dominio.ResumenDeGanancia;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
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
	public ResumenDeGanancia deEmpresa(UUID empresaId, UUID sucursalId, LocalDate desde, LocalDate hasta) {
		// El rango acota los ingresos por la fecha del pago y el costo por la fecha de la venta (creada_en).
		String ingresosSql = INGRESOS + (sucursalId == null ? "" : " and sucursal_id = :sucursalId")
				+ (desde == null ? "" : " and fecha::date >= :desde") + (hasta == null ? "" : " and fecha::date <= :hasta");
		String costoSql = COSTO_DE_VENTAS + (sucursalId == null ? "" : " and v.sucursal_id = :sucursalId")
				+ (desde == null ? "" : " and v.creada_en::date >= :desde")
				+ (hasta == null ? "" : " and v.creada_en::date <= :hasta");
		BigDecimal ingresos = conRango(jdbc.sql(ingresosSql).param("empresaId", empresaId), sucursalId, desde, hasta)
				.query(BigDecimal.class).single();
		BigDecimal costo = conRango(jdbc.sql(costoSql).param("empresaId", empresaId), sucursalId, desde, hasta)
				.query(BigDecimal.class).single();
		return ResumenDeGanancia.de(ingresos, costo);
	}

	private static JdbcClient.StatementSpec conRango(JdbcClient.StatementSpec spec, UUID sucursalId, LocalDate desde,
			LocalDate hasta) {
		if (sucursalId != null) {
			spec = spec.param("sucursalId", sucursalId);
		}
		if (desde != null) {
			spec = spec.param("desde", desde);
		}
		if (hasta != null) {
			spec = spec.param("hasta", hasta);
		}
		return spec;
	}
}
