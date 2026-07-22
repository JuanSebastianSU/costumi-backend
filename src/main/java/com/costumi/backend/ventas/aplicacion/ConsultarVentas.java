package com.costumi.backend.ventas.aplicacion;

import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;
import com.costumi.backend.ventas.dominio.Venta;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de entrada: lista las Ventas de una empresa (scoped por tenant). */
public interface ConsultarVentas {

	List<Venta> deEmpresa(UUID empresaId);

	/** Página de ventas de la empresa, más recientes primero (C3). */
	/** Página de ventas; {@code buscar} (opcional) filtra por código de retiro. */
	Pagina<Venta> listar(UUID empresaId, String buscar, SolicitudDePagina solicitud);

	/** Una venta del tenant por id (para su detalle con líneas y foto). */
	Optional<Venta> buscarPorId(UUID empresaId, UUID ventaId);
}
