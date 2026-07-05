package com.costumi.backend.clientes.adaptadores.entrada;

/** DTO de entrada para poner/quitar a un Cliente de la lista negra. */
public record CambiarListaNegraRequest(boolean enListaNegra) {
}
