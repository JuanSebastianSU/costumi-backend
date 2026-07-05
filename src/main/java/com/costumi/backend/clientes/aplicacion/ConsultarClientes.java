package com.costumi.backend.clientes.aplicacion;

import com.costumi.backend.clientes.dominio.Cliente;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista o busca Clientes de una empresa (RF-7.3), scoped por tenant. */
public interface ConsultarClientes {

	List<Cliente> buscar(UUID empresaId, String texto);
}
