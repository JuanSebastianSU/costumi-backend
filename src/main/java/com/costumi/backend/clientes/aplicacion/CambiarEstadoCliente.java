package com.costumi.backend.clientes.aplicacion;

import com.costumi.backend.clientes.dominio.Cliente;

import java.util.UUID;

/** Puerto de entrada: archiva o reactiva una ficha de cliente (RF-7), scoped por tenant. */
public interface CambiarEstadoCliente {

	Cliente archivar(UUID empresaId, UUID clienteId);

	Cliente activar(UUID empresaId, UUID clienteId);
}
