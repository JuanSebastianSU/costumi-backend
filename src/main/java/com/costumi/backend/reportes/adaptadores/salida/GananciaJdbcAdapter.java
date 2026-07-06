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

	private static final String COSTO_DE_VENTAS = """
			select coalesce(sum(lv.cantidad * coalesce(p.costo_adquisicion, 0)), 0)
			from linea_de_venta lv
			join prenda p on p.id = lv.prenda_id
			where lv.empresa_id = :empresaId
			""";

	private final JdbcClient jdbc;

	GananciaJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public ResumenDeGanancia deEmpresa(UUID empresaId) {
		BigDecimal ingresos = jdbc.sql(INGRESOS).param("empresaId", empresaId).query(BigDecimal.class).single();
		BigDecimal costo = jdbc.sql(COSTO_DE_VENTAS).param("empresaId", empresaId).query(BigDecimal.class).single();
		return ResumenDeGanancia.de(ingresos, costo);
	}
}
