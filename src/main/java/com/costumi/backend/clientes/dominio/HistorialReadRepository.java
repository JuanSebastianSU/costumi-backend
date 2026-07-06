package com.costumi.backend.clientes.dominio;

import java.util.List;
import java.util.UUID;

/** Puerto de salida: historial de operaciones de un cliente (RF-7.2), acotado al tenant. */
public interface HistorialReadRepository {

	List<HistorialItem> deCliente(UUID empresaId, UUID clienteId);

	/** Ids de clientes de la empresa con rentas ACTIVAS (pendientes de devolver), RF-11.5/11.6. */
	List<UUID> clientesConPendientes(UUID empresaId);
}
