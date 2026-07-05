package com.costumi.backend.clientes.aplicacion;

import com.costumi.backend.clientes.dominio.Cliente;

/** Puerto de entrada: cambia el estado de lista negra de un Cliente (RF-7.3). */
public interface CambiarListaNegra {

	Cliente ejecutar(CambiarListaNegraComando comando);
}
