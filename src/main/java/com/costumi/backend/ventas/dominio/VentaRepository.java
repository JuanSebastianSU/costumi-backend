package com.costumi.backend.ventas.dominio;

import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de Ventas con sus líneas (scoped por tenant). */
public interface VentaRepository {

	Venta guardar(Venta venta);

	Optional<Venta> buscarPorId(UUID id);

	List<Venta> listarPorEmpresa(UUID empresaId);

	/**
	 * Página de ventas de la empresa, más recientes primero (C3); {@code estado} y el rango {@code [desde,
	 * hasta]} (por fecha de registro) son opcionales.
	 */
	Pagina<Venta> listar(UUID empresaId, String buscar, EstadoVenta estado, LocalDate desde, LocalDate hasta,
			SolicitudDePagina solicitud);

	/** Cantidad y suma de totales de las ventas de la empresa con los mismos filtros (período de G8). */
	TotalesDeVentas totales(UUID empresaId, EstadoVenta estado, LocalDate desde, LocalDate hasta);

	/** Venta con esa clave de idempotencia en la empresa, si existe (RF-17.6). */
	Optional<Venta> buscarPorClave(UUID empresaId, String claveIdempotencia);
}
