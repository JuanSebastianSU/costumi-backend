package com.costumi.backend.clientes.aplicacion;

import com.costumi.backend.clientes.dominio.HistorialItem;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: historial de un cliente y clientes con pendientes (RF-7.2/11.5). */
public interface ConsultarHistorial {

	List<HistorialItem> historialDeCliente(UUID empresaId, UUID clienteId);

	/** Historial del usuario del marketplace, uniendo sus fichas en todas las tiendas (RF-14.4/18.9). */
	List<HistorialItem> historialDeUsuario(UUID usuarioId);

	/** Ids de clientes de la empresa con rentas activas pendientes de devolver. */
	List<UUID> clientesConPendientes(UUID empresaId);
}
