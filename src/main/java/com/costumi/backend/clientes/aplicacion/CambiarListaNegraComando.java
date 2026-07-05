package com.costumi.backend.clientes.aplicacion;

import java.util.UUID;

/** Datos para poner/quitar a un Cliente de la lista negra (RF-7.3). */
public record CambiarListaNegraComando(UUID empresaId, UUID clienteId, boolean enListaNegra) {
}
