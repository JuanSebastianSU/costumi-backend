package com.costumi.backend.clientes.dominio;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Puerto de salida: historial de operaciones de un cliente (RF-7.2), acotado al tenant. */
public interface HistorialReadRepository {

	List<HistorialItem> deCliente(UUID empresaId, UUID clienteId);

	/**
	 * Ids de clientes de la empresa que caen en la categoría de pendiente indicada (RF-11.5/11.6).
	 * {@code hoy} se usa para las rentas vencidas.
	 */
	List<UUID> clientesPorFiltro(UUID empresaId, FiltroDeClientes filtro, LocalDate hoy);
}
