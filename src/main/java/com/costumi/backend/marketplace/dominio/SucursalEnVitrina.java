package com.costumi.backend.marketplace.dominio;

import java.util.UUID;

/**
 * Modelo de lectura de una sucursal (punto de retiro) visible en el marketplace del cliente
 * (RF-18.5). El cliente elige en qué sucursal retira su renta/compra; el stock se reserva ahí.
 */
public record SucursalEnVitrina(UUID id, String nombre, String direccion) {
}
