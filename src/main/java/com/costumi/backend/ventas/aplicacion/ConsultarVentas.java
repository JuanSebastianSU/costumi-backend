package com.costumi.backend.ventas.aplicacion;

import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;
import com.costumi.backend.ventas.dominio.EstadoVenta;
import com.costumi.backend.ventas.dominio.TotalesDeVentas;
import com.costumi.backend.ventas.dominio.Venta;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de entrada: lista las Ventas de una empresa (scoped por tenant). */
public interface ConsultarVentas {

	List<Venta> deEmpresa(UUID empresaId);

	/**
	 * Página de ventas de la empresa, más recientes primero (C3). {@code buscar} filtra por código de retiro;
	 * {@code estado} y el rango {@code [desde, hasta]} (por fecha de registro) son opcionales.
	 */
	Pagina<Venta> listar(UUID empresaId, String buscar, EstadoVenta estado, LocalDate desde, LocalDate hasta,
			SolicitudDePagina solicitud);

	/** Totales del período (cantidad + suma) de ventas con los mismos filtros, para el resumen de G8. */
	TotalesDeVentas totales(UUID empresaId, EstadoVenta estado, LocalDate desde, LocalDate hasta);

	/** Una venta del tenant por id (para su detalle con líneas y foto). */
	Optional<Venta> buscarPorId(UUID empresaId, UUID ventaId);
}
