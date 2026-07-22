package com.costumi.backend.pedidos.adaptadores.salida;

import com.costumi.backend.pedidos.dominio.CarritoAbierto;
import com.costumi.backend.pedidos.dominio.CarritosAbiertosReadRepository;
import com.costumi.backend.pedidos.dominio.TipoPedido;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Modelo de lectura: los carritos PENDIENTES del usuario en TODAS las tiendas (RF-16.2/16.3).
 *
 * <p>Se filtra por el <b>usuario del token</b> a través de sus fichas de cliente ({@code cliente.usuario_id}),
 * no por empresa: el cliente del marketplace tiene una ficha por tienda, y esta consulta es justamente
 * la que cruza tiendas para poder volver a un carrito dejado a medias. Mismo criterio que su historial.
 *
 * <p>Solo carritos <b>con líneas</b>: uno vacío se crea con solo mirar la tienda (la consulta de carrito
 * lo resuelve/crea), así que listarlos llenaría la pantalla de tiendas que el cliente solo visitó.
 */
@Repository
class CarritosAbiertosJdbcAdapter implements CarritosAbiertosReadRepository {

	private static final String SQL = """
			select c.empresa_id, e.nombre as empresa_nombre, c.sucursal_id, s.nombre as sucursal_nombre,
			       c.tipo, sum(l.cantidad) as articulos
			from carrito c
			join cliente cl on cl.id = c.cliente_id
			join empresa e on e.id = c.empresa_id
			join sucursal s on s.id = c.sucursal_id
			join linea_de_carrito l on l.carrito_id = c.id
			where cl.usuario_id = :usuarioId and c.estado = 'PENDIENTE'
			group by c.empresa_id, e.nombre, c.sucursal_id, s.nombre, c.tipo
			order by e.nombre, s.nombre, c.tipo
			""";

	private final JdbcClient jdbc;

	CarritosAbiertosJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<CarritoAbierto> deUsuario(UUID usuarioId) {
		return jdbc.sql(SQL).param("usuarioId", usuarioId)
				.query((rs, n) -> new CarritoAbierto(
						rs.getObject("empresa_id", UUID.class),
						rs.getString("empresa_nombre"),
						rs.getObject("sucursal_id", UUID.class),
						rs.getString("sucursal_nombre"),
						TipoPedido.valueOf(rs.getString("tipo")),
						rs.getInt("articulos")))
				.list();
	}
}
