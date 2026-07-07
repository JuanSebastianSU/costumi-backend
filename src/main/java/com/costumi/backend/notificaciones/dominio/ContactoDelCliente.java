package com.costumi.backend.notificaciones.dominio;

import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: resuelve el contacto (teléfono/token) de un cliente para poder notificarlo. */
public interface ContactoDelCliente {

	Optional<ContactoDeCliente> buscar(UUID clienteId);
}
