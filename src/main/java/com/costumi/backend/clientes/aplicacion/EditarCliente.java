package com.costumi.backend.clientes.aplicacion;

import com.costumi.backend.clientes.dominio.Cliente;

/** Puerto de entrada: edita los datos de contacto/identidad de una ficha de cliente (RF-7), scoped por tenant. */
public interface EditarCliente {

	Cliente ejecutar(EditarClienteComando comando);
}
