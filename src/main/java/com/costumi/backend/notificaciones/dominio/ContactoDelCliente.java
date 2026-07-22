package com.costumi.backend.notificaciones.dominio;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida: resuelve el contacto (teléfono/token) de un cliente para poder notificarlo.
 *
 * <p>Exige la empresa a proposito: una ficha de cliente pertenece a una tienda (§5.4), y sin ese filtro
 * se podria leer el contacto —o notificar— a un cliente de OTRA empresa.
 */
public interface ContactoDelCliente {

	Optional<ContactoDeCliente> buscar(UUID empresaId, UUID clienteId);
}
