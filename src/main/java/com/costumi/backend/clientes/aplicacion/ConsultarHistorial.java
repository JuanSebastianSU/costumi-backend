package com.costumi.backend.clientes.aplicacion;

import com.costumi.backend.clientes.dominio.CargaDeCliente;
import com.costumi.backend.clientes.dominio.FiltroDeClientes;
import com.costumi.backend.clientes.dominio.HistorialItem;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Puerto de entrada: historial de un cliente y clientes con pendientes (RF-7.2/11.5). */
public interface ConsultarHistorial {

	List<HistorialItem> historialDeCliente(UUID empresaId, UUID clienteId);

	/** Historial del usuario del marketplace, uniendo sus fichas en todas las tiendas (RF-14.4/18.9). */
	List<HistorialItem> historialDeUsuario(UUID usuarioId);

	/** Ids de clientes de la empresa que caen en la categoría de pendiente indicada (RF-11.5/11.6). */
	List<UUID> clientesPorFiltro(UUID empresaId, FiltroDeClientes filtro);

	/** Saldo pendiente y multa por cliente (RF-7/11.5) para los clientes dados (la página actual). */
	Map<UUID, CargaDeCliente> cargaDeClientes(UUID empresaId, Collection<UUID> clienteIds);
}
