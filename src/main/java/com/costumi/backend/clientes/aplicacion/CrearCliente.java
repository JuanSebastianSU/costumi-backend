package com.costumi.backend.clientes.aplicacion;

import com.costumi.backend.clientes.dominio.Cliente;

/** Puerto de entrada: alta de un Cliente (RF-7.1). */
public interface CrearCliente {

	Cliente ejecutar(CrearClienteComando comando);
}
