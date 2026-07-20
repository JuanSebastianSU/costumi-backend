package com.costumi.backend.rentas.adaptadores.salida;

import com.costumi.backend.rentas.dominio.ReservasVencidasReadRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador de lectura de reservas a expirar: rentas RESERVADA creadas antes del límite cuyo importe aún
 * no está cubierto por los pagos netos (COBRO − REEMBOLSO). Lee sobre el esquema compartido (renta + pago).
 */
@Repository
class ReservasVencidasJdbcAdapter implements ReservasVencidasReadRepository {

	private static final String SQL = """
			select r.empresa_id, r.id as renta_id
			from renta r
			where r.estado = 'RESERVADA' and r.creada_en < :limite
			  and r.importe > coalesce((select sum(case p.tipo_pago when 'COBRO' then p.monto
			                                                        when 'REEMBOLSO' then -p.monto else 0 end)
			                            from pago p where p.empresa_id = r.empresa_id
			                                          and p.tipo_concepto = 'RENTA' and p.concepto_id = r.id), 0)
			""";

	private final JdbcClient jdbc;

	ReservasVencidasJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<ReservaVencida> reservasVencidas(Instant limite) {
		return jdbc.sql(SQL).param("limite", Timestamp.from(limite))
				.query((rs, rowNum) -> new ReservaVencida(rs.getObject("empresa_id", UUID.class),
						rs.getObject("renta_id", UUID.class)))
				.list();
	}
}
