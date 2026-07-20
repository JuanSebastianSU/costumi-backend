package com.costumi.backend.clientes.dominio;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Puerto de salida: historial de operaciones de un cliente (RF-7.2), acotado al tenant. */
public interface HistorialReadRepository {

	List<HistorialItem> deCliente(UUID empresaId, UUID clienteId);

	/**
	 * Estado de cuenta del cliente (RF-7/11.5): una línea por renta con saldo o multa, con el desglose
	 * (importe, daños, retraso, depósito, multa, pagado, saldo) que explica cuánto debe y por qué.
	 */
	List<LineaDeEstadoDeCuenta> estadoDeCuenta(UUID empresaId, UUID clienteId);

	/**
	 * Ids de clientes de la empresa que caen en la categoría de pendiente indicada (RF-11.5/11.6).
	 * {@code hoy} se usa para las rentas vencidas.
	 */
	List<UUID> clientesPorFiltro(UUID empresaId, FiltroDeClientes filtro, LocalDate hoy);

	/**
	 * Saldo pendiente y multa acumulada por cliente (RF-7/11.5), solo para los {@code clienteIds} dados
	 * (la página actual). Los clientes sin carga no aparecen en el mapa.
	 */
	Map<UUID, CargaDeCliente> cargaDeClientes(UUID empresaId, Collection<UUID> clienteIds);
}
